/**
 * Firebase Cloud Functions for Amatyma
 *
 * These functions handle CometChat user creation and auth token generation
 * IMPORTANT: Do NOT commit your CometChat API key to version control
 */

const functions = require('firebase-functions/v1');
const axios = require('axios');
const admin = require('firebase-admin');

admin.initializeApp();

// ─────────────────────────────────────────────────────────────────────────────
// FastPix (video posts) — modern v2 functions reading creds from Secret Manager.
// Set the values once with (you run these; secrets never touch the repo):
//   firebase functions:secrets:set FASTPIX_TOKEN_ID
//   firebase functions:secrets:set FASTPIX_SECRET_KEY
//   firebase functions:secrets:set FASTPIX_WEBHOOK_SECRET
// ─────────────────────────────────────────────────────────────────────────────
const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const crypto = require('crypto');

const FASTPIX_TOKEN_ID = defineSecret('FASTPIX_TOKEN_ID');
const FASTPIX_SECRET_KEY = defineSecret('FASTPIX_SECRET_KEY');
const FASTPIX_WEBHOOK_SECRET = defineSecret('FASTPIX_WEBHOOK_SECRET');
const FASTPIX_UPLOAD_API = 'https://api.fastpix.com/v1/on-demand/upload';

// Get CometChat configuration from environment variables
// Set these using: firebase functions:config:set cometchat.api_key="YOUR_KEY"
// functions.config() throws in the v2 runtime. This file also defines v2
// functions, so read it defensively — the module must never crash at load.
// v1 functions (1st-gen runtime) still get real values; the v2 functions below
// don't use CometChat config.
let cometchatCfg = {};
try {
  cometchatCfg = (functions.config() && functions.config().cometchat) || {};
} catch (e) {
  cometchatCfg = {};
}
const COMETCHAT_APP_ID = cometchatCfg.app_id || "1678655d5116b4d9e";
const COMETCHAT_REGION = cometchatCfg.region || "us";
const COMETCHAT_API_KEY = cometchatCfg.api_key;

const COMETCHAT_API_URL = `https://${COMETCHAT_APP_ID}.api-${COMETCHAT_REGION}.cometchat.io/v3`;

/**
 * Create a new CometChat user and return auth token
 *
 * Called after Firebase signup and profile setup
 */
exports.createCometChatUser = functions.https.onCall(async (data, context) => {
  // Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in to create chat account'
    );
  }

  const { uid, name, avatar, username, email } = data;

  // Validate required fields
  if (!uid || !name) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Missing required fields: uid and name'
    );
  }

  // Verify the requesting user matches the UID
  if (context.auth.uid !== uid) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Cannot create user for another account'
    );
  }

  try {
    console.log(`Creating CometChat user for: ${uid}`);

    const response = await axios.post(
      `${COMETCHAT_API_URL}/users`,
      {
        uid: uid,
        name: name,
        avatar: avatar || '',
        metadata: {
          '@private': {
            email: email || '',
            username: username || ''
          }
        },
        withAuthToken: true  // Get auth token in same call
      },
      {
        headers: {
          'apikey': COMETCHAT_API_KEY,
          'Content-Type': 'application/json'
        }
      }
    );

    console.log(`CometChat user created successfully: ${uid}`);

    return {
      success: true,
      authToken: response.data.data.authToken
    };

  } catch (error) {
    console.error('CometChat user creation failed:', error.response?.data || error.message);

    // Handle specific errors
    if (error.response?.status === 409) {
      // User already exists, generate new token
      return await generateAuthToken({ uid }, context);
    }

    throw new functions.https.HttpsError(
      'internal',
      `Failed to create CometChat user: ${error.response?.data?.error?.message || error.message}`
    );
  }
});

/**
 * Generate a new CometChat auth token for an existing user
 *
 * Called during login
 */
exports.getCometChatAuthToken = functions.https.onCall(async (data, context) => {
  // Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be logged in'
    );
  }

  const uid = context.auth.uid;

  return await generateAuthToken({ uid }, context);
});

/**
 * Shared FCM token lookup — CometChat lowercases UIDs in webhook payloads,
 * so we try the lowercase doc first and fall back to a case-insensitive scan.
 * Returns the tokens plus the Firestore doc id they live on (for cleanup).
 */
async function getFcmTokens(uid) {
  const lowerDoc = await admin.firestore().collection('users').doc(uid).get();
  if (lowerDoc.exists && lowerDoc.data()?.fcmTokens?.length > 0) {
    return { tokens: lowerDoc.data().fcmTokens, docId: lowerDoc.id };
  }
  const snapshot = await admin.firestore().collection('users').get();
  const match = snapshot.docs.find(d =>
    (d.data().uid || d.id).toLowerCase() === uid.toLowerCase()
  );
  return match
    ? { tokens: match.data().fcmTokens || [], docId: match.id }
    : { tokens: [], docId: uid };
}

async function removeStaleTokens(docId, allTokens, sendResults) {
  const stale = allTokens.filter((_, i) => sendResults[i].status === 'rejected');
  if (stale.length > 0) {
    await admin.firestore().collection('users').doc(docId).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...stale),
    });
  }
}

/**
 * CometChat webhook — fires on every message_sent event. CometChat
 * represents BOTH calls and chat messages this way (calls carry
 * category="call"), so one endpoint handles both — meaning there is
 * only ever a single URL to register for the "message_sent" trigger:
 *
 *   https://<region>-<project>.cloudfunctions.net/onCallInitiated
 *
 * - category "call"    → rings the callee's phone (WhatsApp-style incoming call)
 * - category "message" → pushes a "new message" notification with reply support
 */
exports.onCallInitiated = functions.https.onRequest(async (req, res) => {
  try {
    const trigger = req.body?.trigger;
    const message = req.body?.data?.message;

    if (trigger !== 'message_sent' || !message) {
      return res.status(200).send('ignored');
    }

    // 1:1 voice/video — CometChat "call" messages (default calling)
    if (message.category === 'call') {
      return await notifyIncomingCall(message, res);
    }

    // Group voice/video — CometChat "meeting" custom messages (direct calling).
    // These don't ring on their own, so we fan a ring-push out to all members.
    if (message.category === 'custom' && message.type === 'meeting' && message.receiverType === 'group') {
      return await notifyGroupCall(message, res);
    }

    if (message.category === 'message' && message.receiverType === 'user') {
      return await notifyNewMessage(message, res);
    }

    return res.status(200).send('ignored');

  } catch (err) {
    console.error('message_sent webhook error:', err);
    return res.status(500).send('error');
  }
});

/** Fetch all member UIDs of a CometChat group (paginated). */
async function getGroupMembers(guid) {
  const uids = [];
  let page = 1;
  const perPage = 100;
  try {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const resp = await axios.get(`${COMETCHAT_API_URL}/groups/${guid}/members`, {
        params: { perPage, page },
        headers: { apikey: COMETCHAT_API_KEY, 'Content-Type': 'application/json' },
      });
      const list = resp.data?.data || [];
      list.forEach((m) => { if (m.uid) uids.push(m.uid); });
      const total = resp.data?.meta?.pagination?.total;
      if (list.length === 0 || (total != null && uids.length >= total)) break;
      page++;
      if (page > 20) break; // safety cap (~2000 members)
    }
  } catch (e) {
    console.error('getGroupMembers failed:', e.response?.data || e.message);
  }
  return uids;
}

/** Fetch a group's display name + icon (for the incoming-call screen). */
async function getGroupInfo(guid) {
  try {
    const resp = await axios.get(`${COMETCHAT_API_URL}/groups/${guid}`, {
      headers: { apikey: COMETCHAT_API_KEY, 'Content-Type': 'application/json' },
    });
    return { name: resp.data?.data?.name || guid, icon: resp.data?.data?.icon || '' };
  } catch (e) {
    console.error('getGroupInfo failed:', e.response?.data || e.message);
    return { name: guid, icon: '' };
  }
}

/** Send a data-only call push to every device of every UID in the list. */
async function sendCallPushToUsers(uids, data) {
  for (const uid of uids) {
    const { tokens, docId } = await getFcmTokens(uid);
    if (tokens.length === 0) continue;
    const sendResults = await Promise.allSettled(
      tokens.map((token) =>
        admin.messaging().send({ token, data, android: { priority: 'high' } })
          .catch((e) => { console.error('FCM call send error:', e.code, e.message); throw e; })
      )
    );
    await removeStaleTokens(docId, tokens, sendResults);
  }
}

async function notifyIncomingCall(message, res) {
  const action = message.data?.action;
  // 'initiated' rings the recipients; these end-of-life actions tell them
  // to STOP ringing (caller gave up, call ended, rejected elsewhere, etc.).
  const STOP_ACTIONS = ['cancelled', 'unanswered', 'rejected', 'busy', 'ended'];
  const isInitiated = action === 'initiated';
  const isStop = STOP_ACTIONS.includes(action);

  if (!isInitiated && !isStop) {
    return res.status(200).send('ignored action: ' + action);
  }

  // Try multiple paths for sessionId — CometChat structure varies
  const sessionId    = message.data?.entities?.on?.entity?.sessionid
                    || message.data?.entities?.on?.sessionid
                    || message.sessionid
                    || message.id
                    || 'unknown';
  const receiver     = message.receiver;        // user UID or group GUID
  const receiverType = message.receiverType;    // 'user' | 'group'
  const callerUid    = message.sender || '';

  if (!receiver) {
    return res.status(200).send('missing receiver');
  }

  const isGroup = receiverType === 'group';

  // Who rings? 1:1 → the single callee. Group → every member except the caller.
  let recipientUids;
  if (isGroup) {
    const members = await getGroupMembers(receiver);
    recipientUids = members.filter((uid) => uid && uid.toLowerCase() !== callerUid.toLowerCase());
    console.log(`group call ${action} — group ${receiver}, members ${members.length}, ringing ${recipientUids.length}`);
  } else {
    recipientUids = [receiver];
  }

  if (recipientUids.length === 0) {
    return res.status(200).send('no recipients');
  }

  // DATA-ONLY high-priority push. No `notification` block — that is what lets
  // FirebaseMessagingService.onMessageReceived() fire even when the app is
  // killed, so we can hand the call to Android's native call screen.
  let data;
  if (isInitiated && isGroup) {
    const info = await getGroupInfo(receiver);
    data = {
      type:           'call',
      callAction:     'initiated',
      sessionId:      String(sessionId),
      callType:       String(message.type),
      receiver:       String(receiver),         // group GUID — app joins this group session
      receiverType:   'group',
      receiverName:   String(info.name),        // group name on the incoming screen
      receiverAvatar: String(info.icon),
      sentAt:         String(Date.now()),
    };
  } else if (isInitiated) {
    const callerName   = message.data?.entities?.by?.entity?.name
                      || message.data?.entities?.for?.entity?.name
                      || 'Someone';
    const callerAvatar = message.data?.entities?.by?.entity?.avatar || '';
    data = {
      type:           'call',
      callAction:     'initiated',
      sessionId:      String(sessionId),
      callType:       String(message.type),     // 'audio' | 'video'
      receiver:       String(callerUid),        // the other party shown on the incoming screen
      receiverType:   'user',
      receiverName:   String(callerName),
      receiverAvatar: String(callerAvatar),
      sentAt:         String(Date.now()),       // ms — used for the 30s freshness check on-device
    };
  } else {
    // Stop the ring on all recipients' phones
    data = {
      type:       'call',
      callAction: 'cancelled',
      sessionId:  String(sessionId),
      sentAt:     String(Date.now()),
    };
  }

  await sendCallPushToUsers(recipientUids, data);
  return res.status(200).send('ok');
}

/**
 * Group call (CometChat "meeting" / direct calling). The meeting message
 * carries the session id in customData; we ring every group member so they
 * get the native incoming-call screen and can join the shared session.
 */
async function notifyGroupCall(message, res) {
  const customData = message.data?.customData || {};
  const sessionId  = customData.sessionID || customData.sessionId;
  const callType   = customData.callType || 'video';
  const groupGuid  = message.receiver;
  const groupName  = message.data?.entities?.receiver?.entity?.name || 'Group call';
  const groupIcon  = message.data?.entities?.receiver?.entity?.icon
                  || message.data?.entities?.receiver?.entity?.avatar || '';
  const callerUid  = message.sender || '';

  if (!sessionId || !groupGuid) {
    return res.status(200).send('missing meeting session/group');
  }

  const members = await getGroupMembers(groupGuid);
  const recipientUids = members.filter((uid) => uid && uid.toLowerCase() !== callerUid.toLowerCase());
  if (recipientUids.length === 0) {
    return res.status(200).send('no recipients');
  }

  const data = {
    type:           'call',
    callAction:     'initiated',
    callWorkflow:   'meeting',          // tells the app to JOIN via direct calling (no acceptCall)
    sessionId:      String(sessionId),
    callType:       String(callType),
    receiver:       String(groupGuid),
    receiverType:   'group',
    receiverName:   String(groupName),  // group name on the incoming screen
    receiverAvatar: String(groupIcon),
    sentAt:         String(Date.now()),
  };

  console.log(`group call → group ${groupGuid}, ringing ${recipientUids.length} members, session ${sessionId}`);
  await sendCallPushToUsers(recipientUids, data);
  return res.status(200).send('ok');
}

async function notifyNewMessage(message, res) {
  const senderId   = message.sender;
  const senderName = message.data?.entities?.sender?.entity?.name || senderId || 'Someone';
  const receiverId = message.receiver;
  const msgType    = message.type;

  if (!receiverId || !senderId) {
    return res.status(200).send('missing sender/receiver');
  }

  const previewByType = {
    text:  message.data?.text || 'New message',
    image: '📷 Photo',
    video: '🎥 Video',
    audio: '🎤 Voice message',
    file:  '📎 File',
  };
  const preview = previewByType[msgType] || 'New message';

  const { tokens: fcmTokens, docId } = await getFcmTokens(receiverId);
  if (fcmTokens.length === 0) {
    console.log(`No FCM tokens for user: ${receiverId}`);
    return res.status(200).send('no tokens');
  }

  const notification = {
    title: senderName,
    body:  preview,
  };

  const data = {
    type:           'new_message',
    // From the receiver's point of view, this 1:1 chat is identified by the sender's uid —
    // matches the conversationId the app uses when opening MessagesActivity for that user
    conversationId: senderId,
    senderUid:      senderId,
    senderName:     senderName,
    messageType:    msgType,
    preview:        preview,
    messageId:      String(message.id || ''),
  };

  const sendResults = await Promise.allSettled(
    fcmTokens.map(token =>
      admin.messaging().send({
        token,
        notification,
        data,
        android: {
          priority: 'high',
          notification: {
            channelId:  'chat_messages',
            priority:   'high',
            sound:      'default',
            tag:        senderId, // newer messages from the same sender update/replace the prior notification
          },
        },
      }).catch(e => { console.error('FCM send error:', e.code, e.message); throw e; })
    )
  );

  await removeStaleTokens(docId, fcmTokens, sendResults);

  console.log(`Message notification sent to ${receiverId}, tokens: ${fcmTokens.length}`);
  return res.status(200).send('ok');
}

async function generateAuthToken(data, context) {
  const { uid } = data;

  try {
    console.log(`Generating auth token for: ${uid}`);

    const response = await axios.post(
      `${COMETCHAT_API_URL}/users/${uid}/auth_tokens`,
      { force: true },  // Generate fresh token
      {
        headers: {
          'apikey': COMETCHAT_API_KEY,
          'Content-Type': 'application/json'
        }
      }
    );

    console.log(`Auth token generated successfully: ${uid}`);

    return {
      authToken: response.data.data.authToken
    };

  } catch (error) {
    console.error('Auth token generation failed:', error.response?.data || error.message);

    throw new functions.https.HttpsError(
      'internal',
      `Failed to generate auth token: ${error.response?.data?.error?.message || error.message}`
    );
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// FastPix video posts
// ═════════════════════════════════════════════════════════════════════════════

/**
 * createFastpixUpload (callable)
 *
 * Creates a FastPix direct upload and a matching `videoPosts` doc in
 * `status: "uploading"`. The client uploads the file straight to the returned
 * signedUrl (via the FastPix Android SDK); the webhook flips the doc to "ready".
 *
 * Returns: { postId, uploadId, signedUrl }
 */
exports.createFastpixUpload = onCall(
  // Public IAM invoker so the app's callable request reaches the function at all;
  // Firebase Auth is still enforced inside via request.auth below.
  { secrets: [FASTPIX_TOKEN_ID, FASTPIX_SECRET_KEY], invoker: 'public' },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'Sign in to post a video');
    }
    const uid = request.auth.uid;
    const caption = String(request.data?.caption || '').slice(0, 2000);
    const topic = String(request.data?.topic || 'All').slice(0, 40);

    // Author profile snapshot (denormalised onto the post for fast feed reads)
    const userSnap = await admin.firestore().collection('users').doc(uid).get();
    const u = userSnap.data() || {};
    const authorName = u.displayName || '';
    const authorHandle = u.username ? `@${u.username}` : '';
    const authorAvatar = u.photoURL || '';

    // Reserve the post id up-front so we can pass it to FastPix as metadata,
    // which comes back on the webhook to map the asset → this doc.
    const postRef = admin.firestore().collection('videoPosts').doc();
    const postId = postRef.id;

    const basic = Buffer
      .from(`${FASTPIX_TOKEN_ID.value()}:${FASTPIX_SECRET_KEY.value()}`)
      .toString('base64');

    let signedUrl = null;
    let uploadId = null;
    try {
      const resp = await axios.post(
        FASTPIX_UPLOAD_API,
        {
          corsOrigin: '*',
          pushMediaSettings: {
            accessPolicy: 'public',
            maxResolution: '1080p',
            metadata: { postId, authorUid: uid },
          },
        },
        { headers: { 'Authorization': `Basic ${basic}`, 'Content-Type': 'application/json' } }
      );
      const d = resp.data?.data || resp.data || {};
      signedUrl = d.signedUrl || d.url || null;
      uploadId = d.uploadId || d.id || null;
    } catch (error) {
      console.error('FastPix create-upload failed:', error.response?.data || error.message);
      throw new HttpsError('internal', 'Could not start the video upload');
    }

    if (!signedUrl) {
      throw new HttpsError('internal', 'FastPix did not return a signed URL');
    }

    await postRef.set({
      postId,
      authorUid: uid,
      authorName,
      authorHandle,
      authorAvatar,
      caption,
      topic,
      status: 'uploading',           // uploading → processing → ready | rejected | failed
      fastpixUploadId: uploadId,
      playbackId: null,
      hlsUrl: null,
      posterUrl: null,
      durationSec: null,
      kudosCount: 0,
      commentCount: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { postId, uploadId, signedUrl };
  }
);

/**
 * fastpixWebhook (HTTP)
 *
 * Receives FastPix events. On `video.media.ready` it writes the playbackId/HLS
 * URL/duration back to the matching `videoPosts` doc and flips it to "ready"
 * (or "rejected" if the clip is outside the 30–60s window — server backstop).
 *
 * Signature: `FastPix-Signature` = Base64 HMAC-SHA256 of the raw body using the
 * dashboard signing secret.
 *
 * NOTE: exact payload field paths are handled defensively + logged on the first
 * live event so we can pin them precisely.
 */
function parseDurationSec(d) {
  if (d == null) return null;
  if (typeof d === 'number') return Math.round(d);
  const s = String(d).trim();
  if (s.includes(':')) {
    const p = s.split(':').map(Number);
    if (p.some(isNaN)) return null;
    if (p.length === 3) return p[0] * 3600 + p[1] * 60 + p[2];
    if (p.length === 2) return p[0] * 60 + p[1];
  }
  const n = parseFloat(s);
  return isNaN(n) ? null : Math.round(n);
}

exports.fastpixWebhook = onRequest(
  // Public: webhooks arrive unauthenticated from FastPix. Authenticity is
  // verified via the FastPix-Signature HMAC, not IAM.
  { secrets: [FASTPIX_WEBHOOK_SECRET], invoker: 'public' },
  async (req, res) => {
    // ── Verify signature (if a signing secret is configured) ──
    try {
      const secret = FASTPIX_WEBHOOK_SECRET.value();
      // 'UNSET' sentinel = verification disabled until the real signing secret
      // (generated by FastPix when the webhook is created) is configured.
      if (secret && secret !== 'UNSET') {
        const provided = req.get('FastPix-Signature') || '';
        const expected = crypto
          .createHmac('sha256', secret)
          .update(req.rawBody)
          .digest('base64');
        if (provided !== expected) {
          console.warn('FastPix webhook signature mismatch');
          return res.status(401).send('bad signature');
        }
      }
    } catch (e) {
      console.error('FastPix signature verify error:', e.message);
    }

    const body = req.body || {};
    const type = body.type || body.event || '';
    const data = body.data || body.object || {};

    const postId = data.metadata?.postId || body.metadata?.postId || null;

    // FastPix carries the playbackId on `video.upload.media_created`
    // (data.playbackIds[0].id), NOT on `video.media.ready`. Grab it wherever it
    // appears and never overwrite a real id with null.
    const playbackId =
      (Array.isArray(data.playbackIds) && data.playbackIds.length > 0
        ? (data.playbackIds[0].id || data.playbackIds[0])
        : null) ||
      data.playbackId ||
      null;

    if (!postId) {
      console.log('FastPix webhook (no postId):', type, JSON.stringify(body).slice(0, 1500));
      return res.status(200).send('ignored');
    }

    const ref = admin.firestore().collection('videoPosts').doc(postId);
    const update = {};

    if (playbackId) {
      update.playbackId = playbackId;
      update.hlsUrl = `https://stream.fastpix.io/${playbackId}.m3u8`;
      update.posterUrl = `https://images.fastpix.io/${playbackId}/thumbnail.jpg`;
    }

    if (type === 'video.media.ready') {
      update.status = 'ready';
      const durationSec = parseDurationSec(data.duration);
      if (durationSec != null) update.durationSec = durationSec;
    }

    if (Object.keys(update).length > 0) {
      await ref.set(update, { merge: true });
      console.log(`videoPost ${postId}: ${type} → ${JSON.stringify(update)}`);
    }
    return res.status(200).send('ok');
  }
);

// ═════════════════════════════════════════════════════════════════════════════
// Engagement counters (kudos + comments) — keep denormalised counts on the post
// ═════════════════════════════════════════════════════════════════════════════
const { onDocumentCreated, onDocumentDeleted, onDocumentUpdated, onDocumentWritten } = require('firebase-functions/v2/firestore');

function bumpPostField(postId, field, delta) {
  return admin.firestore().collection('videoPosts').doc(postId)
    .set({ [field]: admin.firestore.FieldValue.increment(delta) }, { merge: true });
}

// ── Social notifications (in-app Alerts feed) ──
async function getActor(uid) {
  try {
    const u = (await admin.firestore().collection('users').doc(uid).get()).data() || {};
    return { name: u.displayName || '', avatar: u.photoURL || '' };
  } catch (e) {
    return { name: '', avatar: '' };
  }
}

function pushTextFor(notif) {
  const who = notif.actorName || 'Someone';
  switch (notif.type) {
    case 'follow':   return { title: 'New follower', body: `${who} started following you` };
    case 'kudos':    return { title: who, body: 'liked your post' };
    case 'comment':  return { title: `${who} commented`, body: notif.text || '' };
    case 'reply':    return { title: `${who} replied`, body: notif.text || '' };
    case 'new_post': return { title: who, body: 'posted a new video' };
    default:         return { title: 'Amatyma', body: who };
  }
}

// Social FCM push. Tagged type:'social' so the Android receiver routes it to the
// social channel and NEVER collides with chat ('new_message') or calls ('call').
async function sendPush(recipientUid, notif) {
  const userDoc = (await admin.firestore().collection('users').doc(recipientUid).get()).data() || {};
  const tokens = Array.isArray(userDoc.fcmTokens) ? userDoc.fcmTokens.filter(Boolean) : [];
  if (tokens.length === 0) return;

  const { title, body } = pushTextFor(notif);
  const data = {
    type: 'social',
    socialType: String(notif.type || ''),
    title, body,
    actorUid: String(notif.actorUid || ''),
    postId: String(notif.postId || ''),
    posterUrl: String(notif.posterUrl || ''),
    actorAvatar: String(notif.actorAvatar || ''),
  };

  const res = await admin.messaging().sendEachForMulticast({ tokens, data, android: { priority: 'high' } });

  const dead = [];
  res.responses.forEach((r, i) => {
    const code = r.success ? null : (r.error && r.error.code);
    if (code === 'messaging/registration-token-not-registered' || code === 'messaging/invalid-argument') {
      dead.push(tokens[i]);
    }
  });
  if (dead.length) {
    await admin.firestore().collection('users').doc(recipientUid)
      .update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(...dead) }).catch(() => {});
  }
}

async function writeNotification(recipientUid, notif) {
  if (!recipientUid || recipientUid === notif.actorUid) return; // never notify yourself
  await admin.firestore()
    .collection('users').doc(recipientUid)
    .collection('notifications').add({
      ...notif,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  try { await sendPush(recipientUid, notif); } catch (e) { console.error('sendPush failed', e); }
}

// Post likes — single source of truth at users/{uid}/likes/{postId}. The
// trigger keeps kudosCount on the post and fires the "kudos" Alert.
exports.onLikeCreated = onDocumentCreated('users/{uid}/likes/{postId}', async (event) => {
  const { uid, postId } = event.params;
  await bumpPostField(postId, 'kudosCount', 1);
  const post = (await admin.firestore().collection('videoPosts').doc(postId).get()).data() || {};
  if (post.authorUid) {
    const actor = await getActor(uid);
    await writeNotification(post.authorUid, {
      type: 'kudos', actorUid: uid, actorName: actor.name, actorAvatar: actor.avatar,
      postId, posterUrl: post.posterUrl || '',
    });
  }
});

exports.onLikeDeleted = onDocumentDeleted('users/{uid}/likes/{postId}', (event) =>
  bumpPostField(event.params.postId, 'kudosCount', -1));

exports.onCommentCreated = onDocumentCreated('videoPosts/{postId}/comments/{commentId}', async (event) => {
  const { postId } = event.params;
  await bumpPostField(postId, 'commentCount', 1);
  const comment = (event.data && event.data.data) ? event.data.data() : {};
  const post = (await admin.firestore().collection('videoPosts').doc(postId).get()).data() || {};
  if (post.authorUid) {
    await writeNotification(post.authorUid, {
      type: 'comment',
      actorUid: comment.authorUid || '', actorName: comment.authorName || '', actorAvatar: comment.authorAvatar || '',
      postId, text: (comment.text || '').slice(0, 140), posterUrl: post.posterUrl || '',
    });
  }
});

exports.onCommentDeleted = onDocumentDeleted('videoPosts/{postId}/comments/{commentId}', (event) =>
  bumpPostField(event.params.postId, 'commentCount', -1));

// ── Comment replies — replyCount on the parent comment + "reply" Alert ──
function bumpCommentField(postId, commentId, field, delta) {
  return admin.firestore().collection('videoPosts').doc(postId)
    .collection('comments').doc(commentId)
    .set({ [field]: admin.firestore.FieldValue.increment(delta) }, { merge: true });
}

exports.onReplyCreated = onDocumentCreated('videoPosts/{postId}/comments/{commentId}/replies/{replyId}', async (event) => {
  const { postId, commentId } = event.params;
  await bumpCommentField(postId, commentId, 'replyCount', 1);
  const reply = (event.data && event.data.data) ? event.data.data() : {};
  const comment = (await admin.firestore().collection('videoPosts').doc(postId)
    .collection('comments').doc(commentId).get()).data() || {};
  if (comment.authorUid) {
    const post = (await admin.firestore().collection('videoPosts').doc(postId).get()).data() || {};
    await writeNotification(comment.authorUid, {
      type: 'reply',
      actorUid: reply.authorUid || '', actorName: reply.authorName || '', actorAvatar: reply.authorAvatar || '',
      postId, text: (reply.text || '').slice(0, 140), posterUrl: post.posterUrl || '',
    });
  }
});

exports.onReplyDeleted = onDocumentDeleted('videoPosts/{postId}/comments/{commentId}/replies/{replyId}', (event) =>
  bumpCommentField(event.params.postId, event.params.commentId, 'replyCount', -1));

// ═════════════════════════════════════════════════════════════════════════════
// Social graph (follow / "Brothers") — maintain follower/following counts on the
// public `profiles` collection. A follow edge is users/{uid}/following/{targetUid}.
// ═════════════════════════════════════════════════════════════════════════════
function bumpFollow(uid, targetUid, delta) {
  const db = admin.firestore();
  return Promise.all([
    db.collection('profiles').doc(uid)
      .set({ followingCount: admin.firestore.FieldValue.increment(delta) }, { merge: true }),
    db.collection('profiles').doc(targetUid)
      .set({ followersCount: admin.firestore.FieldValue.increment(delta) }, { merge: true }),
  ]);
}

exports.onFollowCreated = onDocumentCreated('users/{uid}/following/{targetUid}', async (event) => {
  const { uid, targetUid } = event.params;
  await bumpFollow(uid, targetUid, 1);
  // Reverse index: lets us fan-out a creator's new posts to their followers.
  await admin.firestore().collection('users').doc(targetUid)
    .collection('followers').doc(uid).set({ createdAt: admin.firestore.FieldValue.serverTimestamp() });
  const actor = await getActor(uid);
  await writeNotification(targetUid, {
    type: 'follow', actorUid: uid, actorName: actor.name, actorAvatar: actor.avatar,
  });
});

exports.onFollowDeleted = onDocumentDeleted('users/{uid}/following/{targetUid}', async (event) => {
  const { uid, targetUid } = event.params;
  await bumpFollow(uid, targetUid, -1);
  await admin.firestore().collection('users').doc(targetUid)
    .collection('followers').doc(uid).delete().catch(() => {});
});

// New video from someone you follow → Alert + push to the author's followers.
// Fires only on the processing→ready transition (when the FastPix webhook lands).
exports.onVideoPostReady = onDocumentWritten('videoPosts/{postId}', async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  if (!after) return;
  // Fire once, on the transition INTO ready (covers video update→ready and
  // image/text created-ready in one shot).
  if ((before && before.status === 'ready') || after.status !== 'ready') return;
  const authorUid = after.authorUid;
  if (!authorUid) return;

  const followers = await admin.firestore().collection('users').doc(authorUid)
    .collection('followers').limit(500).get();
  const notif = {
    type: 'new_post',
    actorUid: authorUid,
    actorName: after.authorName || '',
    actorAvatar: after.authorAvatar || '',
    postId: event.params.postId,
    posterUrl: after.posterUrl || '',
  };
  await Promise.all(followers.docs.map((d) => writeNotification(d.id, notif)));
});

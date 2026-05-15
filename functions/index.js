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

// Get CometChat configuration from environment variables
// Set these using: firebase functions:config:set cometchat.api_key="YOUR_KEY"
const COMETCHAT_APP_ID = functions.config().cometchat.app_id || "1678655d5116b4d9e";
const COMETCHAT_REGION = functions.config().cometchat.region || "us";
const COMETCHAT_API_KEY = functions.config().cometchat.api_key;

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
 * Helper function to generate auth token
 */
/**
 * CometChat webhook — fires when a call is initiated.
 * Reads the callee's FCM tokens from Firestore and sends a
 * high-priority FCM notification so the phone rings even when
 * the app is killed.
 *
 * Set this URL as a CometChat webhook for the "call" trigger:
 *   https://<region>-<project>.cloudfunctions.net/onCallInitiated
 */
exports.onCallInitiated = functions.https.onRequest(async (req, res) => {
  try {
    const body = req.body;
    const trigger = body.trigger;
    const message = body.data?.message;

    // CometChat sends call events as message_sent with category=call
    if (trigger !== 'message_sent' || message?.category !== 'call') {
      return res.status(200).send('ignored');
    }

    // Only notify on initiated calls — ignore cancelled/rejected/ended
    const action = message.data?.action;
    if (action !== 'initiated') {
      return res.status(200).send('not initiated: ' + action);
    }

    const callType   = message.type;                                // 'audio' or 'video'
    // Try multiple paths for sessionId — CometChat structure varies
    const sessionId  = message.data?.entities?.on?.entity?.sessionid
                    || message.data?.entities?.on?.sessionid
                    || message.sessionid
                    || message.id
                    || 'unknown';
    const receiverId = message.receiver;                            // callee UID (lowercase from CometChat)
    const callerName = message.data?.entities?.by?.entity?.name
                    || message.data?.entities?.for?.entity?.name
                    || 'Someone';

    console.log(`call initiated — caller: ${callerName}, receiver: ${receiverId}, sessionId: ${sessionId}, type: ${callType}`);

    if (!receiverId) {
      console.log('missing receiverId');
      return res.status(200).send('missing receiver');
    }

    // Get callee's FCM tokens from Firestore
    const userDoc = await admin.firestore()
      .collection('users')
      .doc(receiverId)
      .get();

    console.log(`Firestore doc exists: ${userDoc.exists}, data: ${JSON.stringify(userDoc.data())}`);

    const fcmTokens = userDoc.data()?.fcmTokens || [];
    if (fcmTokens.length === 0) {
      console.log(`No FCM tokens for user: ${receiverId}`);
      return res.status(200).send('no tokens');
    }

    // Send high-priority FCM to all the user's devices
    const notification = {
      title: `Incoming ${callType} call`,
      body: `${callerName} is calling you`,
    };

    const data = {
      type:       'incoming_call',
      callType:   callType,
      sessionId:  sessionId,
      callerName: callerName,
      callerUid:  callData.initiator?.uid || '',
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
              channelId:  'incoming_calls',
              priority:   'max',
              sound:      'default',
              visibility: 'public',
            },
          },
        })
      )
    );

    // Remove any stale tokens that FCM rejected
    const staleTokens = fcmTokens.filter((_, i) =>
      sendResults[i].status === 'rejected'
    );

    if (staleTokens.length > 0) {
      await admin.firestore()
        .collection('users')
        .doc(receiverId)
        .update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...staleTokens),
        });
    }

    console.log(`Call notification sent to ${receiverId}, tokens: ${fcmTokens.length}`);
    return res.status(200).send('ok');

  } catch (err) {
    console.error('onCallInitiated error:', err);
    return res.status(500).send('error');
  }
});

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

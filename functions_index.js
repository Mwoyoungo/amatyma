/**
 * Firebase Cloud Functions for Amatyma
 *
 * These functions handle CometChat user creation and auth token generation
 * IMPORTANT: Do NOT commit your CometChat API key to version control
 */

const functions = require('firebase-functions');
const axios = require('axios');

// Get CometChat configuration from environment variables
// Set these using: firebase functions:config:set cometchat.api_key="YOUR_KEY"
const COMETCHAT_APP_ID = functions.config().cometchat.app_id || "281421fd397d9bf6";
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

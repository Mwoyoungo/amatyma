package com.lokaleza.amatyma.social

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lokaleza.amatyma.social.ui.theme.AmatymaSocialTheme

/**
 * Host for the new Compose social surface.
 *
 * Phase 0: standalone so the shell can be previewed/run in isolation. The
 * production mount (Option A — hosting this shell inside MainActivity so the
 * existing CometChat/FCM/VoIP bootstrap stays byte-for-byte) lands next.
 */
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmatymaSocialTheme {
                SocialShell(onLogout = { finish() })
            }
        }
    }
}

package com.lokaleza.amatyma.voip

/**
 * Intent/bundle keys shared across the VoIP telecom layer.
 *
 * These mirror CometChat's official push-notification sample
 * (cometchat-push-notification-app-android, ConstantFile.IntentStrings)
 * so the native-call flow behaves exactly like their reference.
 */
object VoipConstants {
    const val NAME          = "name"
    const val TYPE          = "type"
    const val ID            = "id"
    const val RECEIVER      = "receiver"
    const val RECEIVER_ID   = "id"
    const val SESSION_ID     = "sessionId"
    const val RECEIVER_TYPE  = "receiverType"
    const val RECEIVER_NAME  = "receiverName"
    const val RECEIVER_AVATAR = "receiverAvatar"
    const val CALL_TYPE      = "callType"
    const val CALL_ACTION    = "callAction"
    const val CALL_WORKFLOW  = "callWorkflow"   // "default" (1:1) | "meeting" (group/direct calling)
    const val SENT_AT        = "sentAt"

    const val WORKFLOW_MEETING = "meeting"
    const val WORKFLOW_DEFAULT = "default"

    const val MISSED        = "Missed"
    const val REJECTED      = "Rejected"
    const val COMETCHAT_CALL_EVENT = "CometChat_Call_Event"
}

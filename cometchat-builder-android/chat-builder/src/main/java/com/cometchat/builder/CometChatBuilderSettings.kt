package com.cometchat.builder

/**
 * Auto-generated constants from cometchat-builder-settings.json
 */
object CometChatBuilderSettings {

    object ChatFeatures {

         object CoreMessagingExperience {
             var TYPINGINDICATOR: Boolean = true
             var THREADCONVERSATIONANDREPLIES: Boolean = true
             var PHOTOSSHARING: Boolean = true
             var VIDEOSHARING: Boolean = true
             var AUDIOSHARING: Boolean = true
             var FILESHARING: Boolean = true
             var EDITMESSAGE: Boolean = true
             var DELETEMESSAGE: Boolean = true
             var MESSAGEDELIVERYANDREADRECEIPTS: Boolean = true
             var USERANDFRIENDSPRESENCE: Boolean = true
             var CONVERSATIONANDADVANCEDSEARCH: Boolean = true
             var MODERATION: Boolean = true
             var QUOTEDREPLIES: Boolean = false
        }

         object DeeperUserEngagement {
             var MENTIONS: Boolean = true
             var REACTIONS: Boolean = true
             var MESSAGETRANSLATION: Boolean = true
             var POLLS: Boolean = true
             var COLLABORATIVEWHITEBOARD: Boolean = true
             var COLLABORATIVEDOCUMENT: Boolean = true
             var VOICENOTES: Boolean = true
             var EMOJIS: Boolean = true
             var STICKERS: Boolean = true
             var USERINFO: Boolean = true
             var GROUPINFO: Boolean = true
             var MENTIONALL: Boolean = true
        }

         object AiUserCopilot {
             var CONVERSATIONSTARTER: Boolean = false
             var CONVERSATIONSUMMARY: Boolean = false
             var SMARTREPLY: Boolean = false
        }

         object GroupManagement {
             var CREATEGROUP: Boolean = true
             var ADDMEMBERSTOGROUPS: Boolean = true
             var JOINLEAVEGROUP: Boolean = true
             var DELETEGROUP: Boolean = true
             var VIEWGROUPMEMBERS: Boolean = true
        }

         object ModeratorControls {
             var KICKUSERS: Boolean = true
             var BANUSERS: Boolean = true
             var PROMOTEDEMOTEMEMBERS: Boolean = true
        }

         object PrivateMessagingWithinGroups {
             var SENDPRIVATEMESSAGETOGROUPMEMBERS: Boolean = true
        }

         object UserManagement {
             var FRIENDSONLY: Boolean = false
        }

    }

    object CallFeatures {

         object VoiceAndVideoCalling {
             var ONEONONEVOICECALLING: Boolean = true
             var ONEONONEVIDEOCALLING: Boolean = true
             var GROUPVIDEOCONFERENCE: Boolean = true
             var GROUPVOICECONFERENCE: Boolean = true
        }

    }

    object Layout {
         var WITHSIDEBAR: Boolean = true
         var TABS: List<String> = listOf(
            "chats",
            "calls",
            "users",
            "groups",
        )
         var CHATTYPE: String = "user"

    }

    object Style {
         var THEME: String = "light"

         object Color {
             var BRANDCOLOR: String = "#000000"
             var PRIMARYTEXTLIGHT: String = "#14141461"
             var PRIMARYTEXTDARK: String = "#FFFFFF"
             var SECONDARYTEXTLIGHT: String = "#72727257"
             var SECONDARYTEXTDARK: String = "#989898"
        }

         object Typography {
             var FONT: String = "roboto"
             var SIZE: String = "default"
        }

    }

    object NoCode {
         var DOCKED: Boolean = false

         object Styles {
             var BUTTONBACKGROUND: String = "#141414"
             var BUTTONSHAPE: String = "rounded"
             var OPENICON: String = "https://cdn.jsdelivr.net/npm/@cometchat/chat-embed@latest/dist/icons/docked_open_icon.svg"
             var CLOSEICON: String = "https://cdn.jsdelivr.net/npm/@cometchat/chat-embed@latest/dist/icons/docked_close_icon.svg"
             var CUSTOMJS: String = ""
             var CUSTOMCSS: String = ""
             var DOCKEDALIGNMENT: String = "right"
        }

    }

    object Agent {
         var CHATHISTORY: Boolean = true
         var NEWCHAT: Boolean = true
         var AGENTICON: String = ""
         var SHOWAGENTICON: Boolean = true

    }

}
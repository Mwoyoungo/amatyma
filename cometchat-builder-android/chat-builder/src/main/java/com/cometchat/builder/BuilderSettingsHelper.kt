package com.cometchat.builder

import android.content.Context
import android.view.View
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chatuikit.calls.calllogs.CometChatCallLogs
import com.cometchat.chatuikit.conversations.CometChatConversations
import com.cometchat.chatuikit.groupmembers.CometChatGroupMembers
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader
import com.cometchat.chatuikit.messagelist.CometChatMessageList
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.models.AdditionParameter
import com.cometchat.chatuikit.shared.models.CometChatMessageOption
import com.cometchat.chatuikit.shared.models.CometChatMessageTemplate
import com.cometchat.chatuikit.threadheader.CometChatThreadHeader
import com.cometchat.chatuikit.users.CometChatUsers
import com.google.android.material.bottomnavigation.BottomNavigationView

object BuilderSettingsHelper {

    fun applySettingsToBottomNavigationView(bottomNavigationView: BottomNavigationView, callback: CometChat.CallbackListener<Int?>) {
        val menu = bottomNavigationView.menu

        menu.findItem(R.id.nav_chats).isVisible = false
        menu.findItem(R.id.nav_calls).isVisible = false
        menu.findItem(R.id.nav_users).isVisible = false
        menu.findItem(R.id.nav_groups).isVisible = false

        var id = 0
        // Add menu items based on tabs list
        for (tab in CometChatBuilderSettings.Layout.TABS) {
            if (tab.equals("chats", ignoreCase = true)) {
                if (id == 0) id = R.id.nav_chats
                menu.findItem(R.id.nav_chats).isVisible = true
            } else if (tab.equals("calls", ignoreCase = true)) {
                if (id == 0) id = R.id.nav_calls
                menu.findItem(R.id.nav_calls).isVisible = true
            } else if (tab.equals("users", ignoreCase = true)) {
                if (id == 0) id = R.id.nav_users
                menu.findItem(R.id.nav_users).isVisible = true
            } else if (tab.equals("groups", ignoreCase = true)) {
                if (id == 0) id = R.id.nav_groups
                menu.findItem(R.id.nav_groups).isVisible = true
            }
        }

        if (id != 0) callback.onSuccess(id)
    }

    fun applySettingsToConversations(conversations: CometChatConversations) {
        conversations.userStatusVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.USERANDFRIENDSPRESENCE) View.VISIBLE else View.GONE
        conversations.receiptsVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.MESSAGEDELIVERYANDREADRECEIPTS) View.VISIBLE else View.GONE
        conversations.searchBoxVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.CONVERSATIONANDADVANCEDSEARCH) View.VISIBLE else View.GONE
    }

    fun applySettingsToMessageHeader(messageHeader: CometChatMessageHeader) {
        if (messageHeader.group != null) {
            messageHeader.voiceCallButtonVisibility = if (CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.GROUPVOICECONFERENCE) View.VISIBLE else View.GONE
            messageHeader.videoCallButtonVisibility = if (CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.GROUPVIDEOCONFERENCE) View.VISIBLE else View.GONE
        } else if (messageHeader.user != null) {
            messageHeader.voiceCallButtonVisibility = if (CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.ONEONONEVOICECALLING) View.VISIBLE else View.GONE
            messageHeader.videoCallButtonVisibility = if (CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.ONEONONEVIDEOCALLING) View.VISIBLE else View.GONE
        }
        messageHeader.userStatusVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.USERANDFRIENDSPRESENCE) View.VISIBLE else View.GONE
    }

    fun applySettingsToMessageList(messageList: CometChatMessageList) {
        messageList.replyInThreadOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.THREADCONVERSATIONANDREPLIES) View.VISIBLE else View.GONE
        messageList.editMessageOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.EDITMESSAGE) View.VISIBLE else View.GONE
        messageList.deleteMessageOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.DELETEMESSAGE) View.VISIBLE else View.GONE
        messageList.receiptsVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.MESSAGEDELIVERYANDREADRECEIPTS) View.VISIBLE else View.GONE
        messageList.messageReactionOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.REACTIONS) View.VISIBLE else View.GONE
        messageList.translateMessageOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.MESSAGETRANSLATION) View.VISIBLE else View.GONE
        messageList.isEnableConversationStarter = CometChatBuilderSettings.ChatFeatures.AiUserCopilot.CONVERSATIONSTARTER
        messageList.isEnableSmartReplies = CometChatBuilderSettings.ChatFeatures.AiUserCopilot.SMARTREPLY
        messageList.messagePrivatelyOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.PrivateMessagingWithinGroups.SENDPRIVATEMESSAGETOGROUPMEMBERS) View.VISIBLE else View.GONE
        messageList.messageInfoOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.MESSAGEDELIVERYANDREADRECEIPTS) View.VISIBLE else View.GONE
        messageList.moderationViewVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.MODERATION) View.VISIBLE else View.GONE
        messageList.isSwipeToReplyEnabled = CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.QUOTEDREPLIES
        messageList.replyOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.QUOTEDREPLIES) View.VISIBLE else View.GONE

        val messageTemplateList = CometChatUIKit.getDataSource().getMessageTemplates(messageList.additionParameter)

        for (messageTemplate in messageTemplateList) {
            if (!CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.PHOTOSSHARING) {
                removeOptionFromMessageTemplate(
                    messageTemplate, UIKitConstants.MessageCategory.MESSAGE, UIKitConstants.MessageType.IMAGE,
                    UIKitConstants.MessageOption.SHARE, messageList.additionParameter
                )
            }
            if (!CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.VIDEOSHARING) {
                removeOptionFromMessageTemplate(
                    messageTemplate, UIKitConstants.MessageCategory.MESSAGE, UIKitConstants.MessageType.VIDEO,
                    UIKitConstants.MessageOption.SHARE, messageList.additionParameter
                )
            }
            if (!CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.AUDIOSHARING) {
                removeOptionFromMessageTemplate(
                    messageTemplate, UIKitConstants.MessageCategory.MESSAGE, UIKitConstants.MessageType.AUDIO,
                    UIKitConstants.MessageOption.SHARE, messageList.additionParameter
                )
            }
            if (!CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.FILESHARING) {
                removeOptionFromMessageTemplate(
                    messageTemplate, UIKitConstants.MessageCategory.MESSAGE, UIKitConstants.MessageType.FILE,
                    UIKitConstants.MessageOption.SHARE, messageList.additionParameter
                )
            }
        }
        messageList.setTemplates(messageTemplateList)
    }

    private fun removeOptionFromMessageTemplate(
        messageTemplate: CometChatMessageTemplate, category: String, type: String, optionId: String, additionParameter: AdditionParameter
    ) {
        if (messageTemplate.category != category || messageTemplate.type != type) {
            return
        }
        messageTemplate.setOptions { context: Context?, baseMessage: BaseMessage?, group: Group? ->
            val options = CometChatUIKit
                .getDataSource()
                .getMessageOptions(context, baseMessage, group, additionParameter)
            options.removeIf { option: CometChatMessageOption -> option.id == optionId }
            options
        }
    }

    fun applySettingsToMessageComposer(messageComposer: CometChatMessageComposer) {
        messageComposer.disableTypingEvents(
            !CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.TYPINGINDICATOR
        )
        messageComposer.isDisableMentions = !CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.MENTIONS
        messageComposer.pollAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.POLLS) View.VISIBLE else View.GONE
        messageComposer.collaborativeWhiteboardOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.COLLABORATIVEWHITEBOARD) View.VISIBLE else View.GONE
        messageComposer.collaborativeDocumentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.COLLABORATIVEDOCUMENT) View.VISIBLE else View.GONE
        messageComposer.voiceNoteButtonVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.VOICENOTES) View.VISIBLE else View.GONE
        messageComposer.stickersButtonVisibility = if (CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.STICKERS) View.VISIBLE else View.GONE

        messageComposer.cameraAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.PHOTOSSHARING) View.VISIBLE else View.GONE
        messageComposer.imageAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.PHOTOSSHARING) View.VISIBLE else View.GONE
        messageComposer.videoAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.VIDEOSHARING) View.VISIBLE else View.GONE
        messageComposer.audioAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.AUDIOSHARING) View.VISIBLE else View.GONE
        messageComposer.fileAttachmentOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.FILESHARING) View.VISIBLE else View.GONE
        messageComposer.setDisableMentionAll(CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.MENTIONALL)


        // Hide attachment option if all sharing features are disabled
        val anyAttachmentEnabled = CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.PHOTOSSHARING || CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.VIDEOSHARING || CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.AUDIOSHARING || CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.FILESHARING || CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.COLLABORATIVEWHITEBOARD || CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.POLLS || CometChatBuilderSettings.ChatFeatures.DeeperUserEngagement.COLLABORATIVEDOCUMENT
        messageComposer.attachmentButtonVisibility = if (anyAttachmentEnabled) View.VISIBLE else View.GONE
    }

    fun applySettingsToThreadHeader(threadHeader: CometChatThreadHeader) {
        threadHeader.receiptsVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.MESSAGEDELIVERYANDREADRECEIPTS) View.VISIBLE else View.GONE
    }

    fun applySettingsToUsers(users: CometChatUsers) {
        users.userStatusVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.USERANDFRIENDSPRESENCE) View.VISIBLE else View.GONE
    }

    fun applySettingToGroupMembers(groupMembers: CometChatGroupMembers) {
        groupMembers.kickMemberOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.ModeratorControls.KICKUSERS) View.VISIBLE else View.GONE
        groupMembers.banMemberOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.ModeratorControls.BANUSERS) View.VISIBLE else View.GONE
        groupMembers.scopeChangeOptionVisibility = if (CometChatBuilderSettings.ChatFeatures.ModeratorControls.PROMOTEDEMOTEMEMBERS) View.VISIBLE else View.GONE
        groupMembers.userStatusVisibility = if (CometChatBuilderSettings.ChatFeatures.CoreMessagingExperience.USERANDFRIENDSPRESENCE) View.VISIBLE else View.GONE
    }

    fun applySettingsToCallLogs(callLog: CometChatCallLogs) {
        if (!CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.ONEONONEVIDEOCALLING) {
            callLog.itemVideoCallIcon = null
        }
        if (!CometChatBuilderSettings.CallFeatures.VoiceAndVideoCalling.ONEONONEVOICECALLING) {
            callLog.itemIncomingCallIcon = null
        }
    }

    fun getSDKVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getSDKPackageName(): String {
        return BuildConfig.APPLICATION_ID
    }

}
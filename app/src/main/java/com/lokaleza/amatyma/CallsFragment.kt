package com.lokaleza.amatyma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.calls.utils.CallUtils
import com.cometchat.chatuikit.shared.interfaces.OnItemClick
import com.lokaleza.amatyma.databinding.FragmentCallsBinding

class CallsFragment : Fragment() {

    private var _binding: FragmentCallsBinding? = null
    private val binding get() = _binding!!

    private var isCallActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tapping the row itself is informational — no action needed beyond what the SDK shows.
        // The call-back icon (phone icon on the right of each row) is wired below.
        binding.callLogs.setOnCallIconClickListener { _, _, _, callLog ->
            if (isCallActive) return@setOnCallIconClickListener
            isCallActive = true

            val initiator = callLog.initiator as? CallUser
            val targetUid = if (initiator != null && CallUtils.isLoggedInUser(initiator)) {
                (callLog.receiver as? CallUser)?.uid
            } else {
                initiator?.uid
            }

            if (targetUid == null) {
                isCallActive = false
                return@setOnCallIconClickListener
            }

            val callType = if (callLog.type == CometChatCallsConstants.CALL_TYPE_VIDEO) {
                CometChatConstants.CALL_TYPE_VIDEO
            } else {
                CometChatConstants.CALL_TYPE_AUDIO
            }

            val call = Call(targetUid, CometChatConstants.RECEIVER_TYPE_USER, callType)
            CometChat.initiateCall(call, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(c: Call) {
                    activity?.runOnUiThread {
                        isCallActive = false
                        CometChatCallActivity.launchOutgoingCallScreen(requireActivity(), c, null)
                    }
                }
                override fun onError(e: CometChatException?) {
                    activity?.runOnUiThread {
                        isCallActive = false
                        Toast.makeText(requireContext(), "Call failed: ${e?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

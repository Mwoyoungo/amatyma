package com.lokaleza.amatyma

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.models.User
import com.lokaleza.amatyma.databinding.FragmentUsersWithMessagesBinding

class UsersWithMessagesFragment : Fragment() {

    private var _binding: FragmentUsersWithMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersWithMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUsers()
    }

    private fun setupUsers() {
        binding.usersWithMessages.setLoadingStateVisibility(View.GONE)

        binding.usersWithMessages.setOnItemClick { _: View, _: Int, user: User ->
            showUserDetails(user)
        }
    }

    private fun showUserDetails(user: User) {
        val intent = Intent(requireActivity(), UserDetailsActivity::class.java).apply {
            putExtra(UserDetailsActivity.EXTRA_USER_ID, user.uid)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.cometchat.builder.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.builder.R
import com.cometchat.builder.databinding.BuilderFragmentUsersBinding
import com.cometchat.builder.ui.activity.MessagesActivity
import com.cometchat.builder.BuilderSettingsHelper
import com.google.gson.Gson

class UsersFragment : Fragment() {

    private lateinit var binding: BuilderFragmentUsersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BuilderFragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.users.setOnItemClick { view, position, user ->
            val intent = Intent(requireActivity(), MessagesActivity::class.java)
            intent.putExtra(getString(R.string.builder_user), user.toJson().toString())
            startActivity(intent)
        }
        BuilderSettingsHelper.applySettingsToUsers(binding.users)
    }
}

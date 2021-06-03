package com.onelogin.mfa.appkotlin.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.appkotlin.databinding.FragmentRegisterBinding

private lateinit var binding: FragmentRegisterBinding

class RegisterFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerOidcButton.setOnClickListener { registerOidc(view) }
        binding.registerWebviewButton.setOnClickListener { registerWebview(view) }
        binding.registerQrManualButton.setOnClickListener { registerQrManual(view) }
    }

    private fun registerOidc(view: View) { }

    private fun registerWebview(view: View) {
        view.findNavController().navigate(R.id.action_navigation_register_to_web_login_fragment)
    }

    private fun registerQrManual(view: View) {
        view.findNavController().navigate(R.id.action_navigation_register_to_qr_fragment)
    }
}

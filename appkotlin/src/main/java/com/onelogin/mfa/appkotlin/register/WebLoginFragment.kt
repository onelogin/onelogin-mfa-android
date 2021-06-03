package com.onelogin.mfa.appkotlin.register

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.model.RegisterFactorError
import com.onelogin.mfa.model.RegisterFactorSuccess


class WebLoginFragment : Fragment() {

    private val mfaClient = OneLoginMfa.getClient()
    private val subdomainInput: EditText by lazy { requireView().findViewById(R.id.subdomain) }
    private val usernameInput: EditText by lazy { requireView().findViewById(R.id.username) }
    private val passwordInput: EditText by lazy { requireView().findViewById(R.id.password) }
    private val registerButton: Button by lazy { requireView().findViewById(R.id.register) }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (subdomainInput.text.isNotEmpty() && usernameInput.text.isNotEmpty() && passwordInput.text.isNotEmpty()) {
                enableButton(true)
            } else {
                enableButton(false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_web_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableButton(false)
        registerButton.setOnClickListener {
            registerByWebLogin()
        }

        subdomainInput.addTextChangedListener(textWatcher)
        usernameInput.addTextChangedListener(textWatcher)
        passwordInput.addTextChangedListener(textWatcher)

    }

    private fun registerByWebLogin() {
        val subdomain = subdomainInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (subdomain.isBlank() || username.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        enableButton(false)
        hideKeyboard()
        val snackbar: Snackbar = Snackbar.make(
            requireView().findViewById(R.id.web_login_container),
            "Registering factor...",
            Snackbar.LENGTH_LONG
        ).apply { show() }

        mfaClient.registerFactorByWebLogin(
            subdomain,
            username,
            password,
            object : MfaCallback<RegisterFactorSuccess, RegisterFactorError> {
                override fun onSuccess(success: RegisterFactorSuccess) {
                    snackbar.dismiss()
                    activity?.onBackPressed()
                    showToast("Successfully added OneLogin factor")
                }

                override fun onError(error: RegisterFactorError) {
                    snackbar.dismiss()
                    enableButton(true)
                    showToast("Error adding OneLogin factor")
                }
            }
        )
    }

    private fun enableButton(enable: Boolean) {
        registerButton.isEnabled = enable
        registerButton.isClickable = enable
        registerButton.alpha = if (enable) 1F else 0.5F
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity?.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

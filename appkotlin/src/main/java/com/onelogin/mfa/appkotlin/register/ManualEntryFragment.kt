package com.onelogin.mfa.appkotlin.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.model.RegisterFactorError
import com.onelogin.mfa.model.RegisterFactorSuccess
import com.onelogin.mfa.view.ManualEntry
import com.onelogin.mfa.view.OnCodeEntryListener

class ManualEntryFragment: Fragment() {

    private val mfaClient = OneLoginMfa.getClient()

    private val manualEntry: ManualEntry by lazy { requireView().findViewById(R.id.manual_entry_input) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_manual_entry, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpEntryListener()
    }

    private fun setUpEntryListener() {
        manualEntry.setEntryCompleteListener(object: OnCodeEntryListener {
            override fun onOneLoginCode(code: String) {
                registerOneLoginFactor(code)
            }

            override fun onThirdPartyCode(code: String) {
                registerThirdPartyFactor(code)
            }
        })
    }

    private fun registerOneLoginFactor(code: String) {
        mfaClient.registerFactor(
            code,
            object : MfaCallback<RegisterFactorSuccess, RegisterFactorError> {
                override fun onSuccess(success: RegisterFactorSuccess) {
                    activity?.onBackPressed()
                    Toast.makeText(context, "Successfully added OneLogin factor", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: RegisterFactorError) {
                    Toast.makeText(context, "Error adding OneLogin factor", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun registerThirdPartyFactor(code: String) {
        mfaClient.registerFactor(
            code,
            object : MfaCallback<RegisterFactorSuccess, RegisterFactorError> {
                override fun onSuccess(success: RegisterFactorSuccess) {
                    activity?.onBackPressed()
                    Toast.makeText(context, "Successfully added factor", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: RegisterFactorError) {
                    Toast.makeText(context, "Error adding factor", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

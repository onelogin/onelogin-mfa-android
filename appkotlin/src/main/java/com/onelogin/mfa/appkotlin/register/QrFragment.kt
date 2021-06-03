package com.onelogin.mfa.appkotlin.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.model.RegisterFactorError
import com.onelogin.mfa.model.RegisterFactorSuccess
import com.onelogin.mfa.view.OnCodeEntryListener
import com.onelogin.mfa.view.QrScan

class QrFragment: Fragment() {

    private val mfaClient = OneLoginMfa.getClient()

    private val qrScanner: QrScan by lazy { requireView().findViewById(R.id.qr_scanner) }

    private val manualEntryButton: TextView by lazy { requireView().findViewById(R.id.manual_entry_button) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_qr, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manualEntryButton.setOnClickListener { onManualEntryClicked() }
        setUpScanListener()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        qrScanner.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setUpScanListener() {
        qrScanner.setScanListener(object : OnCodeEntryListener {
            override fun onOneLoginCode(code: String) {
                registerOneLoginFactor(code)
            }

            override fun onThirdPartyCode(code: String) {
                registerThirdPartyFactor(code)
            }
        })
    }

    private fun onManualEntryClicked() {
        view?.findNavController()?.navigate(R.id.action_qr_fragment_to_manual_entry_fragment)
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

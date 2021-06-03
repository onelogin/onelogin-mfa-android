package com.onelogin.mfa.appkotlin.factors

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.appkotlin.factors.FactorsFragment.Companion.FACTOR_ID_BUNDLE
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import com.onelogin.mfa.model.Factor
import com.onelogin.mfa.view.Otp

class SingleFragment : Fragment() {

    private val mfaClient = OneLoginMfa.getClient()

    private var factor: Factor? = null
    private var detailDialog: AlertDialog? = null


    private val displayName: TextView by lazy { requireView().findViewById(R.id.displayName) }
    private val username: TextView by lazy { requireView().findViewById(R.id.username) }
    private val otpCode: Otp by lazy { requireView().findViewById(R.id.otp) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_single, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.getLong(FACTOR_ID_BUNDLE)?.let {
            getFactor(it)
        }
    }

    private fun getFactor(factorId: Long) {
        mfaClient.getFactorById(factorId, object : MfaCallback<Factor?, Exception> {
            override fun onSuccess(success: Factor?) {
                if (success == null) { return }
                showFactor(success)
                factor = success
            }

            override fun onError(error: Exception) {
                Toast.makeText(context, "Failed to retrieve factor", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFactor(factor: Factor) {
        displayName.text = factor.displayName
        username.text = OneLoginMfaUtils.modifyUserNameForDisplay(factor.username)
        otpCode.setFactor(factor)

        requireView().findViewById<ConstraintLayout>(R.id.single_factor_container)
            .apply {
                setOnLongClickListener {
                    onShowDetails()
                    return@setOnLongClickListener true
                }
            }
    }

    private fun onShowDetails() {
        if (activity?.isFinishing == true || detailDialog?.isShowing == true) {
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_factor_detail, null)

        if (!factor?.issuer.isNullOrBlank()) {
            view.findViewById<Group>(R.id.issuer_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.issuer).text = factor?.issuer
        }

        if (!factor?.username.isNullOrBlank()) {
            view.findViewById<Group>(R.id.username_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.username).text = factor?.username
        }

        if (!factor?.subdomain.isNullOrBlank() && factor?.issuer.equals("onelogin", ignoreCase = true)) {
            view.findViewById<Group>(R.id.url_group).visibility = View.VISIBLE
            "${factor?.subdomain}.onelogin.com".also { view.findViewById<TextView>(R.id.url).text = it }
        }

        if (!factor?.credentialId.isNullOrBlank()) {
            view.findViewById<Group>(R.id.credential_id_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.credential_id).text = factor?.credentialId
        }

        detailDialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(if (factor?.subdomain.isNullOrEmpty()) factor?.issuer else factor?.subdomain)
            .setPositiveButton("Close") { _, _ -> }
            .setNegativeButton("Delete") { _, _ -> onRemoveFactor() }
            .show()
    }

    private fun onRemoveFactor() {
        mfaClient.removeAllFactors(object : MfaCallback<Int, Exception> {
            override fun onSuccess(success: Int) {
                showEmptyFragment()
            }

            override fun onError(error: Exception) {
                Toast.makeText(context, "Failed to remove factor", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEmptyFragment() {
        val emptyFragment = EmptyFragment()
        val fragmentManager = activity?.supportFragmentManager
        val transaction = fragmentManager?.beginTransaction()
        transaction?.replace(R.id.factors_container, emptyFragment)
        transaction?.commit()
    }

    companion object {
        @JvmStatic
        fun newInstance(factorId: Long) = SingleFragment().apply {
            arguments = Bundle().apply {
                putLong(FACTOR_ID_BUNDLE, factorId)
            }
        }
    }
}

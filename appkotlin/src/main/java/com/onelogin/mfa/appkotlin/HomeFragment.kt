package com.onelogin.mfa.appkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.MfaClient
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import com.onelogin.mfa.model.Factor
import com.onelogin.mfa.model.RefreshFactorsSuccess

class HomeFragment: Fragment() {

    private val mfaClient: MfaClient = OneLoginMfa.getClient()

    private val factorCount: TextView by lazy { requireView().findViewById(R.id.home_total_factors_count) }

    private val deleteFactorsButton: Button by lazy { requireView().findViewById(R.id.home_delete_factors_button) }

    private val versionName: TextView by lazy { requireView().findViewById(R.id.version_name) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deleteFactorsButton.setOnClickListener { deleteAllFactors() }
        versionName.text = "Version ${OneLoginMfaUtils.getOneLoginMfaVersion()}"
        refreshFactors()
    }

    private fun refreshFactors() {
        mfaClient.refreshFactors(object : MfaCallback<RefreshFactorsSuccess, Exception> {
            override fun onSuccess(success: RefreshFactorsSuccess) {
                val unpairedCount = success.unpairedCount
                val updatedCount = success.updatedCount

                if (unpairedCount > 0 || updatedCount > 0) {
                    showToast("Updated factors", Toast.LENGTH_LONG)
                }
                getFactors()
            }

            override fun onError(error: Exception) {
                showToast("Error refreshing factors", Toast.LENGTH_LONG)
                getFactors()
            }
        })
    }

    private fun getFactors() {
        mfaClient.getFactors(object : MfaCallback<List<Factor>, Exception> {
            override fun onSuccess(success: List<Factor>) {
                val size = success.size
                factorCount.text = size.toString()
                when  {
                    size < 1 -> { enableDeleteButton(false) }
                    size >= 1 -> { enableDeleteButton(true) }
                }
            }

            override fun onError(error: Exception) {
                showToast("Failed to retrieve factors")
            }

        })
    }

    private fun deleteAllFactors() {
        mfaClient.removeAllFactors(object : MfaCallback<Int, Exception> {
            override fun onSuccess(success: Int) {
                showToast("Removed all factors")
                getFactors()
            }

            override fun onError(error: Exception) {
                showToast("Failed to remove all factors")
            }
        })
    }

    private fun enableDeleteButton(enable: Boolean) {
        deleteFactorsButton.isClickable = enable
        if (enable) {
            deleteFactorsButton.alpha = 1F
        } else {
            deleteFactorsButton.alpha = 0.5F
        }
    }

    private fun showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, length).show()
    }
}

package com.onelogin.mfa.appkotlin.factors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.MfaClient
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.model.Factor

class FactorsFragment: Fragment() {

    private val mfaClient: MfaClient = OneLoginMfa.getClient()

    private var factorsList: List<Factor> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_factors, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getFactors()
    }

    private fun getFactors() {
        mfaClient.getFactors(object : MfaCallback<List<Factor>, Exception> {
            override fun onSuccess(success: List<Factor>) {
                val factorCount = success.size
                factorsList = success
                when  {
                    factorCount < 1 -> { showEmptyFragment() }
                    factorCount == 1 -> { showSingleFragment() }
                    factorCount > 1 -> { showMultiFragment() }
                }
            }

            override fun onError(error: Exception) {
                showEmptyFragment()
                Toast.makeText(context, "Failed to retrieve factors", Toast.LENGTH_SHORT).show()
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

    private fun showSingleFragment() {
        val factorId = factorsList[0].id
        val singleFragment = SingleFragment.newInstance(factorId)
        val fragmentManager = activity?.supportFragmentManager
        val transaction = fragmentManager?.beginTransaction()
        transaction?.replace(R.id.factors_container, singleFragment)
        transaction?.commit()
    }

    private fun showMultiFragment() {
        val multiFragment = MultiFactorFragment()
        val fragmentManager = activity?.supportFragmentManager
        val transaction = fragmentManager?.beginTransaction()
        transaction?.replace(R.id.factors_container, multiFragment)
        transaction?.commit()
    }

    companion object {
        const val FACTOR_ID_BUNDLE = "FACTOR_ID"
    }
}

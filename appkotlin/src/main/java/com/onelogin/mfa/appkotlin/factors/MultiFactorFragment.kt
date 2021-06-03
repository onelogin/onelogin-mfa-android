package com.onelogin.mfa.appkotlin.factors

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onelogin.mfa.MfaCallback
import com.onelogin.mfa.MfaClient
import com.onelogin.mfa.OneLoginMfa
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.model.Factor

class MultiFactorFragment : Fragment(), MultiFactorRecyclerViewAdapter.FactorItemInteractionListener {

    private lateinit var recyclerView: RecyclerView

    private var detailDialog: AlertDialog? = null

    private val mfaClient: MfaClient = OneLoginMfa.getClient()
    private val recyclerAdapter : MultiFactorRecyclerViewAdapter by lazy {
        MultiFactorRecyclerViewAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.fragment_multi_factor, container, false) as RecyclerView
        recyclerView = view.findViewById(R.id.multi_factor_list) as RecyclerView

        with (recyclerView) {
            adapter = recyclerAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getFactors()
        setFactorListener()
    }

    override fun onShowDetails(factor: Factor) {
        if (activity?.isFinishing == true || detailDialog?.isShowing == true) {
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_factor_detail, null)

        if (!factor.issuer.isNullOrBlank()) {
            view.findViewById<Group>(R.id.issuer_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.issuer).text = factor.issuer
        }

        if (!factor.username.isNullOrBlank()) {
            view.findViewById<Group>(R.id.username_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.username).text = factor.username
        }

        if (!factor.subdomain.isNullOrBlank() && factor.issuer.equals("onelogin", ignoreCase = true)) {
            view.findViewById<Group>(R.id.url_group).visibility = View.VISIBLE
            "${factor.subdomain}.onelogin.com".also { view.findViewById<TextView>(R.id.url).text = it }
        }

        if (!factor.credentialId.isNullOrBlank()) {
            view.findViewById<Group>(R.id.credential_id_group).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.credential_id).text = factor.credentialId
        }

        detailDialog = AlertDialog.Builder(context)
            .setView(view)
            .setTitle(if (factor.subdomain.isNullOrEmpty()) factor.issuer else factor.subdomain)
            .setPositiveButton("Close") { _, _ -> }
            .setNegativeButton("Delete") { _, _ -> onRemoveFactor(factor) }
            .show()
    }

    private fun getFactors() {
        mfaClient.getFactors(object : MfaCallback<List<Factor>, Exception> {
            override fun onSuccess(success: List<Factor>) {
                for (factor in success) {
                    recyclerAdapter.addFactorItem(factor)
                }
            }

            override fun onError(error: Exception) {
                Toast.makeText(context, "Failed to retrieve factors", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun onRemoveFactor(factor: Factor) {
        mfaClient.removeFactor(factor, object : MfaCallback<Int, Exception> {
            override fun onSuccess(success: Int) {
                Toast.makeText(context, "Removed factor", Toast.LENGTH_SHORT).show()
                recyclerAdapter.removeFactorItem(factor)
            }

            override fun onError(error: Exception) {
                Toast.makeText(context, "Failed to remove factor", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun setFactorListener() {
        recyclerAdapter.setFactorItemInteractionListener(this@MultiFactorFragment)
    }
}

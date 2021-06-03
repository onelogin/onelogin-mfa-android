package com.onelogin.mfa.appkotlin.factors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onelogin.mfa.appkotlin.R
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import com.onelogin.mfa.model.Factor
import com.onelogin.mfa.view.CountdownDial
import com.onelogin.mfa.view.Otp

class MultiFactorRecyclerViewAdapter(
    private var factorList: ArrayList<Factor> = ArrayList()
) : RecyclerView.Adapter<MultiFactorRecyclerViewAdapter.ViewHolder>() {

    private var seeds: HashSet<String>? = HashSet()
    private var onFactorItemInteractionListener: FactorItemInteractionListener? = null

    interface FactorItemInteractionListener {
        fun onShowDetails(factor: Factor)
    }

    fun setFactorItemInteractionListener(listener: FactorItemInteractionListener) {
        onFactorItemInteractionListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_multi_factor_item, parent, false)
        return ViewHolder((view))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val factor = factorList[position]

        holder.itemView.setOnLongClickListener {
            onFactorItemInteractionListener?.onShowDetails(factor)
            return@setOnLongClickListener true
        }
        holder.otp.setFactor(factor)
        holder.countdownDial.setProgress(factor)
        holder.username.text = OneLoginMfaUtils.modifyUserNameForDisplay(factor.username)
        holder.displayName.text = factor.displayName
    }

    override fun getItemCount(): Int = factorList.size

    fun addFactorItem(factor: Factor) {
        if (seeds?.contains(factor.seed) == true) {
            return
        }

        seeds?.add(factor.seed)
        factorList.add(factor)
        notifyItemChanged(factorList.size - 1)
    }

    fun removeFactorItem(factor: Factor) {
        val index = factorList.indexOf(factor)
        factorList.remove(factor)
        seeds?.remove(factor.seed)
        notifyItemRemoved(index)
    }

    inner class ViewHolder(factorView: View) : RecyclerView.ViewHolder(factorView) {
        val username: TextView = factorView.findViewById(R.id.user_name)
        val otp: Otp = factorView.findViewById(R.id.otp)
        val displayName: TextView = factorView.findViewById(R.id.display_name)
        val countdownDial: CountdownDial = factorView.findViewById(R.id.countdown)
    }
}

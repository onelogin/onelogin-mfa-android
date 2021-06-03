package com.onelogin.mfa.appjava.factors;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.data.util.OneLoginMfaUtils;
import com.onelogin.mfa.model.Factor;
import com.onelogin.mfa.view.CountdownDial;
import com.onelogin.mfa.view.Otp;

import java.util.ArrayList;
import java.util.List;

public class MultiFactorRecyclerViewAdapter extends RecyclerView.Adapter<MultiFactorRecyclerViewAdapter.ViewHolder> {

    public final List<Factor> factorList;

    public MultiFactorRecyclerViewAdapter() { factorList = new ArrayList<>(); };

    public MultiFactorRecyclerViewAdapter(List<Factor> items) {
        factorList = items;
    }

    private FactorItemInteractionListener onFactorItemInteractionListener = null;

    interface FactorItemInteractionListener {
        void onShowDetails(Factor factor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_multi_factor_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.factorItem = factorList.get(position);

        holder.itemView.setOnLongClickListener(v -> {
            onFactorItemInteractionListener.onShowDetails(holder.factorItem);
            return true;
        });

        holder.otp.setFactor(holder.factorItem);
        holder.countdownDial.setProgress(holder.factorItem, 100);
        holder.username.setText(OneLoginMfaUtils.INSTANCE.modifyUserNameForDisplay(holder.factorItem.getUsername()));
        holder.displayName.setText(holder.factorItem.getDisplayName());
    }

    @Override
    public int getItemCount() {
        return factorList.size();
    }

    public void setFactorItemInteractionListener(FactorItemInteractionListener listener) {
        onFactorItemInteractionListener = listener;
    }

    public void addFactorItem(Factor factor) {
        factorList.add(factor);
        notifyItemChanged(factorList.size() - 1);
    }

    public void removeFactorItem(Factor factor) {
        int index = factorList.indexOf(factor);
        factorList.remove(factor);
        notifyItemRemoved(index);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View factorView;
        public final TextView username;
        public final Otp otp;
        public final TextView displayName;
        public final CountdownDial countdownDial;
        public Factor factorItem;

        public ViewHolder(View view) {
            super(view);
            factorView = view;
            username = factorView.findViewById(R.id.user_name);
            otp = factorView.findViewById(R.id.otp);
            displayName = factorView.findViewById(R.id.display_name);
            countdownDial = factorView.findViewById(R.id.countdown);
        }
    }
}
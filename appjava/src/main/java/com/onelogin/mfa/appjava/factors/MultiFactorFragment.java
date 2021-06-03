package com.onelogin.mfa.appjava.factors;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.model.Factor;

import java.util.List;

public class MultiFactorFragment extends Fragment implements MultiFactorRecyclerViewAdapter.FactorItemInteractionListener {

    private final MfaClient mfaClient = OneLoginMfa.getClient();

    private MultiFactorRecyclerViewAdapter recyclerViewAdapter;

    private AlertDialog detailDialog = null;

    public MultiFactorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_multi_factor, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.multi_factor_list);
        recyclerViewAdapter = new MultiFactorRecyclerViewAdapter();

        Context context = view.getContext();
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        setFactorListener();

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getFactors();
    }

    @Override
    public void onShowDetails(Factor factor) {
        if (getActivity().isFinishing() || (detailDialog != null && detailDialog.isShowing())) {
            return;
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_factor_detail, null);
        TextView issuerText = view.findViewById(R.id.issuer);
        TextView usernameText = view.findViewById(R.id.username);
        TextView urlText = view.findViewById(R.id.url);
        TextView credentialIdText = view.findViewById(R.id.credential_id);

        if (factor.getIssuer() != null || !factor.getIssuer().isEmpty()) {
            view.findViewById(R.id.issuer_group).setVisibility(View.VISIBLE);
            issuerText.setText(factor.getIssuer());
        }

        if (factor.getUsername() != null || !factor.getUsername().isEmpty()) {
            view.findViewById(R.id.username_group).setVisibility(View.VISIBLE);
            usernameText.setText(factor.getUsername());
        }

        if ((factor.getSubdomain() != null || !factor.getSubdomain().isEmpty()) &&
                factor.getIssuer().equalsIgnoreCase("onelogin")
        ) {
            view.findViewById(R.id.url_group).setVisibility(View.VISIBLE);
            urlText.setText(String.format("%s.onelogin.com", factor.getSubdomain()));
        }

        if (factor.getCredentialId() != null || !factor.getCredentialId().isEmpty()) {
            view.findViewById(R.id.credential_id_group).setVisibility(View.VISIBLE);
            credentialIdText.setText(factor.getCredentialId());
        }

        String dialogTitle = (factor.getSubdomain() == null || factor.getSubdomain().isEmpty()) ? factor.getIssuer() : factor.getSubdomain();
        detailDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(dialogTitle)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Delete", (dialog, which) -> onRemoveFactor(factor))
                .show();

    }

    private void getFactors() {
        mfaClient.getFactors(new MfaCallback<List<Factor>, Exception>() {
            @Override
            public void onSuccess(List<Factor> factors) {
                for (Factor factor : factors) {
                    recyclerViewAdapter.addFactorItem(factor);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to retrieve all factors", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onRemoveFactor(Factor factor) {
        mfaClient.removeFactor(factor, new MfaCallback<Integer, Exception>() {
            @Override
            public void onSuccess(Integer integer) {
                Toast.makeText(getContext(), "Removed factor", Toast.LENGTH_SHORT).show();
                recyclerViewAdapter.removeFactorItem(factor);

                if (recyclerViewAdapter.factorList.size() < 1) {
                    getActivity().onBackPressed();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to remove factor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFactorListener() {
        recyclerViewAdapter.setFactorItemInteractionListener(this);
    }
}
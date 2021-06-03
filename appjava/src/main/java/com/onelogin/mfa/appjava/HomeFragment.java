package com.onelogin.mfa.appjava;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.data.util.OneLoginMfaUtils;
import com.onelogin.mfa.model.Factor;
import com.onelogin.mfa.model.RefreshFactorsSuccess;

import java.util.List;

public class HomeFragment extends Fragment {

    private final MfaClient mfaClient = OneLoginMfa.getClient();

    private TextView factorCount;
    private Button deleteFactorsButton;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        factorCount = view.findViewById(R.id.home_total_factors_count);

        TextView versionName = view.findViewById(R.id.version_name);
        versionName.setText(String.format("Version %s", OneLoginMfaUtils.INSTANCE.getOneLoginMfaVersion()));

        deleteFactorsButton = view.findViewById(R.id.home_delete_factors_button);
        deleteFactorsButton.setOnClickListener(v -> deleteAllFactors());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFactors();
    }

    private void refreshFactors() {
        mfaClient.refreshFactors(new MfaCallback<RefreshFactorsSuccess, Exception>() {
            @Override
            public void onSuccess(@NonNull RefreshFactorsSuccess refreshFactorsSuccess) {
                int unpairedCount = refreshFactorsSuccess.getUnpairedCount();
                int updateCount = refreshFactorsSuccess.getUpdatedCount();

                if (unpairedCount > 0 || updateCount > 0) {
                    showToast("Updated factors", Toast.LENGTH_LONG);
                }
                getFactors();
            }

            @Override
            public void onError(@NonNull Exception e) {
                showToast("Error refreshing factors", Toast.LENGTH_LONG);
                getFactors();
            }
        });
    }

    private void getFactors() {
        mfaClient.getFactors(new MfaCallback<List<Factor>, Exception>() {
            @Override
            public void onSuccess(@NonNull List<Factor> factors) {
                int size = factors.size();
                factorCount.setText(String.valueOf(size));
                enableDeleteButton(size >= 1);
            }

            @Override
            public void onError(@NonNull Exception e) {
                showToast("Failed to retrieve factors", Toast.LENGTH_SHORT);
            }
        });
    }

    private void deleteAllFactors() {
        mfaClient.removeAllFactors(new MfaCallback<Integer, Exception>() {
            @Override
            public void onSuccess(@NonNull Integer integer) {
                showToast("Removed all factors", Toast.LENGTH_SHORT);
                getFactors();
            }

            @Override
            public void onError(@NonNull Exception e) {
                showToast("Failed to remove all factors", Toast.LENGTH_SHORT);
            }
        });
    }

    private void enableDeleteButton(Boolean enable) {
        deleteFactorsButton.setClickable(enable);

        if (enable) {
            deleteFactorsButton.setAlpha(1F);
        } else {
            deleteFactorsButton.setAlpha(0.5F);
        }
    }

    private void showToast(String message, int length) {
        Toast.makeText(getContext(), message, length).show();
    }
}
package com.onelogin.mfa.appjava.factors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.model.Factor;

import java.util.List;

public class FactorsActivity extends AppCompatActivity {

    final private MfaClient mfaClient = OneLoginMfa.getClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_factors);

        getFactors();
    }

    private void getFactors() {
        mfaClient.getFactors(new MfaCallback<List<Factor>, Exception>() {
            @Override
            public void onSuccess(List<Factor> factors) {
                int factorCount = factors.size();

                if (factorCount >= 1) {
                    showMultiFactorsFragment();
                } else {
                    showEmptyFactorsFragment();
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void showEmptyFactorsFragment() {
        Fragment emptyFragment = new EmptyFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.factors_container, emptyFragment);
        fragmentTransaction.commit();
    }

    private void showMultiFactorsFragment() {
        Fragment multiFragment = new MultiFactorFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.factors_container, multiFragment);
        fragmentTransaction.commit();
    }
}
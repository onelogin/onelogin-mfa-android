package com.onelogin.mfa.appjava.register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.appjava.MainActivity;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.model.RegisterFactorError;
import com.onelogin.mfa.model.RegisterFactorSuccess;
import com.onelogin.mfa.view.ManualEntry;
import com.onelogin.mfa.view.OnCodeEntryListener;

public class ManualEntryActivity extends AppCompatActivity {

    private final MfaClient mfaClient = OneLoginMfa.getClient();

    private ManualEntry manualEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);

        manualEntry = findViewById(R.id.manual_entry_input);
        setUpEntryListener();
    }

    private void setUpEntryListener() {
        manualEntry.setEntryCompleteListener(new OnCodeEntryListener() {
            @Override
            public void onOneLoginCode(@NonNull String code) {
                registerFactor(code);
            }

            @Override
            public void onThirdPartyCode(@NonNull String code) {
                registerFactor(code);
            }
        });
    }

    private void registerFactor(String code) {
        mfaClient.registerFactor(code, new MfaCallback<RegisterFactorSuccess, RegisterFactorError>() {
            @Override
            public void onSuccess(@NonNull RegisterFactorSuccess registerFactorSuccess) {
                closeManualEntry();
                Toast.makeText(getBaseContext(), "Successfully added factor", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull RegisterFactorError registerFactorError) {
                Toast.makeText(getBaseContext(), "Error adding factor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void closeManualEntry() {
        setResult(MainActivity.MANUAL_ENTRY_SUCCESS_CODE);
        finish();
    }

}
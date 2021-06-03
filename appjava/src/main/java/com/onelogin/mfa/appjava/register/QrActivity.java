package com.onelogin.mfa.appjava.register;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.appjava.MainActivity;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.model.RegisterFactorError;
import com.onelogin.mfa.model.RegisterFactorSuccess;
import com.onelogin.mfa.view.OnCodeEntryListener;
import com.onelogin.mfa.view.QrScan;

public class QrActivity extends AppCompatActivity {

    private final MfaClient mfaClient = OneLoginMfa.getClient();

    private QrScan qrScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        qrScanner = findViewById(R.id.qr_scanner);
        TextView manualEntryButton = findViewById(R.id.manual_entry_button);
        manualEntryButton.setOnClickListener(v -> onManualEntryClicked());
        setUpScanListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        qrScanner.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setUpScanListener() {
        qrScanner.setScanListener(new OnCodeEntryListener() {
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
                finish();
                Toast.makeText(getBaseContext(), "Successfully added factor", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull RegisterFactorError registerFactorError) {
                Toast.makeText(getBaseContext(), "Error adding factor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onManualEntryClicked() {
        Intent intent = new Intent(this, ManualEntryActivity.class);
        intent.putExtra(MainActivity.MANUAL_ENTRY_SUCCESS_TILE, MainActivity.MANUAL_ENTRY_SUCCESS_CODE);

        launchManualEntry.launch(intent);
    }

    final ActivityResultLauncher<Intent> launchManualEntry = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == MainActivity.MANUAL_ENTRY_SUCCESS_CODE) {
                    finish();
                }
            });
}

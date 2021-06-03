package com.onelogin.mfa.appjava.register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.onelogin.mfa.MfaCallback;
import com.onelogin.mfa.MfaClient;
import com.onelogin.mfa.OneLoginMfa;
import com.onelogin.mfa.appjava.R;
import com.onelogin.mfa.model.RegisterFactorError;
import com.onelogin.mfa.model.RegisterFactorSuccess;

public class WebLoginActivity extends AppCompatActivity {

    private final MfaClient mfaClient = OneLoginMfa.getClient();

    private EditText subdomainInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private Button registerButton;

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            enableButton(
                    !subdomainInput.getText().toString().isEmpty() &&
                    !usernameInput.getText().toString().isEmpty() &&
                    !passwordInput.getText().toString().isEmpty()
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_login);

        subdomainInput = findViewById(R.id.subdomain);
        subdomainInput.addTextChangedListener(textWatcher);
        usernameInput = findViewById(R.id.username);
        usernameInput.addTextChangedListener(textWatcher);
        passwordInput = findViewById(R.id.password);
        passwordInput.addTextChangedListener(textWatcher);
        registerButton = findViewById(R.id.register);
        registerButton.setOnClickListener(v -> registerByWebLogin());
        enableButton(false);
    }

    private void registerByWebLogin() {
        String subdomain = subdomainInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (subdomain.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showToast("Empty fields are not allowed");
            return;
        }

        enableButton(false);
        hideKeyboard();
        Snackbar snackbar = Snackbar.make(
                findViewById(R.id.web_login_container),
                "Registering factor...",
                Snackbar.LENGTH_LONG
        );
        snackbar.show();

        mfaClient.registerFactorByWebLogin(subdomain, username, password,
                new MfaCallback<RegisterFactorSuccess, RegisterFactorError>() {
                    @Override
                    public void onSuccess(@NonNull RegisterFactorSuccess registerFactorSuccess) {
                        snackbar.dismiss();
                        showToast("Successfully added OneLogin factor");
                        finish();
                    }

                    @Override
                    public void onError(@NonNull RegisterFactorError registerFactorError) {
                        snackbar.dismiss();
                        showToast("Error adding OneLogin factor");
                        enableButton(true);
                    }
        });
    }

    private void enableButton(Boolean enable) {
        registerButton.setEnabled(enable);
        registerButton.setClickable(enable);
        registerButton.setAlpha(enable ? 1F : 0.5F);
    }

    private void showToast(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
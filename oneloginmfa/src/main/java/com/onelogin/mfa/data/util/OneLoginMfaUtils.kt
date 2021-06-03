package com.onelogin.mfa.data.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.Html
import android.widget.Toast
import com.onelogin.mfa.BuildConfig
import timber.log.Timber
import java.lang.Exception
import java.util.regex.Pattern

/**
 * Provides utility methods to use within this client.
 */
object OneLoginMfaUtils {

    // We make this lazy as compiling patterns is a long running operation and it may slow the
    // startup of the application
    private val oneLoginCodeMatchers by lazy {
        listOf(
            Pattern.compile("\\d{2}-\\w{7,}"),
            Pattern.compile("\\d{9}"),
            Pattern.compile("\\d{2}\\w{17,}")
        )
    }

    /**
     * Provides the current version of this client in the format: 1.0.0 (1).
     *
     * @return Returns String value of version name and code.
     */
    fun getOneLoginMfaVersion() : String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    /**
     * Verify that the registration code is a OneLogin code by pattern matching or issuer verification.
     *
     * @param code The code used to register a OneLogin factor.
     * @return Returns true if it is a valid OneLogin code, else it returns false.
     */
    fun isOneLoginCode(code: String): Boolean {
        val isOneLogin = oneLoginCodeMatchers.any {
            it.matcher(code).matches()
        }

        if (isOneLogin) {
            return true
        }

        val uri = Uri.parse(code)
        val issuer: String? = uri.getQueryParameter("issuer")
        if (issuer?.equals("OneLogin", ignoreCase = true) == true)
            return true

        return false
    }

    /**
     * Verify that the registration code is valid to register a third party factor.
     *
     * @param code The code used to register a factor.
     * @return Returns true if it is a valid third party code, else it returns false.
     */
    fun isValidThirdPartyCode(code: String?): Boolean {
        if (code == null || code.isEmpty()) {
            return false
        }

        try {
            val uri = Uri.parse(code)
            val seed = uri.getQueryParameter("secret")

            if (seed != null && seed.isNotEmpty() && seed.isNotBlank()) {
                return true
            }
        } catch (e: Exception) {
            Timber.e(e,"Unable to verify third party code")
            return false
        }

        return false
    }

    /**
     * Retrieve the seed parameter used to register a OneLogin factor.
     *
     * @param code The code used to register a OneLogin factor.
     * @return Returns String value of seed. If no value is found, returns null.
     */
    fun getOtpCode(code: String): String? {
        val isOneLogin = oneLoginCodeMatchers.any {
            it.matcher(code).matches()
        }

        if(isOneLogin) {
            return code
        }

        val uri = Uri.parse(code)
        if (uri.host.equals("protect.onelogin.com", ignoreCase = true)) {
            return uri.getQueryParameter("code")
        }

        val secret = uri.getQueryParameter("secret")
        val issuer = uri.getQueryParameter("issuer")
        if (issuer == null || !issuer.equals("OneLogin", ignoreCase = true)) {
            Timber.d("Unsupported url format")
            return null
        }

        return secret
    }

    /**
     * Retrieve the issuer of a registration code.
     *
     * @param code The code used to register a factor.
     * @return Returns the issuer associated with the factor. If no value is found, returns null.
     */
    fun getIssuer(code: String?): String {
        if (code == null || !code.startsWith("otpauth://totp")) {
            return "OneLogin"
        }

        val uri = Uri.parse(code)
        return uri.getQueryParameter("issuer") ?: ""
    }

    /**
     * Show a toast message to the user.
     *
     * @param context Current context.
     * @param message Message to display to the user.
     * @param duration Duration time of displaying the toast.
     */
    fun showToast(context: Context, message: Int, duration: Int) {
        Toast.makeText(context, message, duration).show()
    }

    /**
     * Call this method to modify a HTML styled username to display in a TextView.
     *
     * @param value Username to display.
     */
    fun modifyUserNameForDisplay(value: String?): String? {
        value?.let {
            return if (Build.VERSION.SDK_INT >= 24)
                Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT)?.toString()
            else
                Html.fromHtml(value)?.toString()
        }
        return null
    }
}

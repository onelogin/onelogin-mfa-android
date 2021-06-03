package com.onelogin.mfa.view

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.onelogin.mfa.R
import com.onelogin.mfa.data.util.FactorCodeMaskWatcher
import com.onelogin.mfa.data.util.OneLoginMfaUtils

class ManualEntry(context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs) {

    private val manualEntryInput: EditText by lazy { findViewById(R.id.onelogin_mfa_manual_entry_code)}

    private var onEntryCompleteListener: OnCodeEntryListener? = null

    fun setEntryCompleteListener(entryCompleteListener: OnCodeEntryListener) {
        onEntryCompleteListener = entryCompleteListener
    }

    init {
        inflate(context, R.layout.onelogin_mfa_manual_entry_view, this)
        initAttributes(attrs)
        setActionListener()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ManualEntry)

        val codeColor = attributes.getColor(R.styleable.ManualEntry_factor_code_color, manualEntryInput.currentTextColor)
        manualEntryInput.setTextColor(codeColor)

        val textSize = convertPixelsToDp(attributes.getDimensionPixelSize(
            R.styleable.ManualEntry_factor_code_size,
            manualEntryInput.textSize.toInt()), context)
        manualEntryInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        val hint = attributes.getString(R.styleable.ManualEntry_factor_code_hint)
        manualEntryInput.hint = hint

        val hintColor = attributes.getColor(R.styleable.ManualEntry_factor_code_hint_color, manualEntryInput.currentHintTextColor)
        manualEntryInput.setHintTextColor(hintColor)

        val enableNineDigitFormat = attributes.getBoolean(R.styleable.ManualEntry_enable_nine_digit_format, true)
        setMask(enableNineDigitFormat)

        attributes.recycle()
    }

    private fun setActionListener() {
        val keyListener = TextView.OnEditorActionListener { _, _, _ ->
            return@OnEditorActionListener onActionComplete(manualEntryInput.text?.toString())
        }

        manualEntryInput.setOnEditorActionListener(keyListener)
    }

    private fun onActionComplete(input: String?): Boolean {
        if (input.isNullOrEmpty() || input.length < 9) {
            OneLoginMfaUtils.showToast(context, R.string.onelogin_mfa_invalid_code, Toast.LENGTH_SHORT)
            return false
        }

        if (!OneLoginMfaUtils.isOneLoginCode(input)) {
            onEntryCompleteListener?.onThirdPartyCode(input)
            return true
        }

        onEntryCompleteListener?.onOneLoginCode(input)
        return true
    }

    private fun setMask(enableNineDigits: Boolean) {
        if (enableNineDigits) {
            manualEntryInput.addTextChangedListener(FactorCodeMaskWatcher.buildNineDigitCodeMask())
        } else {
            manualEntryInput.addTextChangedListener(FactorCodeMaskWatcher.buildEighteenDigitCodeMask())
        }
    }

    private fun convertPixelsToDp(px: Int, context: Context): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}

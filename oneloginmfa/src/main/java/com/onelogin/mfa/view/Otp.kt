package com.onelogin.mfa.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.TypedArray
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.core.view.updateLayoutParams
import com.onelogin.mfa.R
import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.device.DeviceManagerImpl
import com.onelogin.mfa.data.network.NetworkProvider
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import com.onelogin.mfa.model.Factor
import java.util.*
import kotlin.concurrent.schedule

class Otp @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    private val otpCode: TextView by lazy { findViewById(R.id.onelogin_mfa_otp_code) }
    private val warningContainer: LinearLayout by lazy { findViewById(R.id.onelogin_mfa_otp_warning_container)}
    private val warningText: TextView by lazy { findViewById(R.id.onelogin_mfa_otp_warning) }
    private val warningIcon: ImageView by lazy { findViewById(R.id.onelogin_mfa_otp_warning_icon) }

    private val deviceManager = DeviceManagerImpl(context, NetworkProvider.getOneLoginApi(context))
    private var factor: Factor? = null
    private var period: Long = 0L
    private var lastOtp: String = ""
    private var countDownTimer: CountDownTimer? = null

    private var showToast: Boolean = true
    private var showWarningIcon: Boolean = true
    private var unrootedDeviceRequiredMessage: String = ""
    private var keyguardRequiredMessage: String = ""

    init {
        inflate(context, R.layout.onelogin_mfa_otp_view, this)
        initAttributes(attrs)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initClickListener()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
    }

    fun setFactor(factor: Factor) {
        if (factor.seed.isBlank()) {
            throw OneLoginMfaException("Invalid factor")
        }
        this.factor = factor
        this.period = factor.period.toLong()
        setTimer()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Otp)

        val attrsOtpCode =  attributes.getString(R.styleable.Otp_code)
        otpCode.text = attrsOtpCode?.let { formatOtp(it) }

        otpCode.setTextColor(
            attributes.getColor(
                R.styleable.Otp_code_color,
                otpCode.currentTextColor
            )
        )

        val otpTextSize = convertPixelsToDp(
            attributes.getDimensionPixelSize(R.styleable.Otp_code_size, otpCode.textSize.toInt()),
            context
        )

        otpCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, otpTextSize)

        val enableTapToCopy = attributes.getBoolean(R.styleable.Otp_enable_tap_to_copy, true)
        if (enableTapToCopy) {
            showToast = attributes.getBoolean(R.styleable.Otp_enable_show_toast_on_copy, true)
            initClickListener()
        }

        val warningTextSize = convertPixelsToDp(
            attributes.getDimensionPixelSize(
                R.styleable.Otp_message_warning_size,
                warningText.textSize.toInt()
            ), context
        )

        warningText.setTextSize(TypedValue.COMPLEX_UNIT_SP, warningTextSize)

        showWarningIcon = attributes.getBoolean(R.styleable.Otp_enable_warning_icon, true)

        val warningIconSize = attributes.getDimensionPixelSize(R.styleable.Otp_warning_icon_size, warningIcon.layoutParams.height)
        warningIcon.updateLayoutParams {
            width = warningIconSize
            height = warningIconSize
        }

        unrootedDeviceRequiredMessage = attributes.getString(
            R.styleable.Otp_message_warning_unrooted_device_required
        ) ?: context.getString(R.string.onelogin_mfa_otp_unrooted_device_required)

        keyguardRequiredMessage = attributes.getString(
            R.styleable.Otp_message_warning_keyguard_required
        ) ?: context.getString(R.string.onelogin_mfa_otp_device_lock_required)

        attributes.recycle()
    }

    private fun initClickListener() {
        otpCode.setOnClickListener {
            otpCode.alpha = 0.25F
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("OTP", otpCode.text.filter { !it.isWhitespace() })
            clipboardManager.setPrimaryClip(clipData)
            if (showToast && otpCode.visibility == View.VISIBLE) {
                OneLoginMfaUtils.showToast(
                    context,
                    R.string.onelogin_mfa_copy_otp,
                    Toast.LENGTH_SHORT
                )
            }
            Timer().schedule(150){
                otpCode.alpha = 1F
            }
        }
    }

    private fun setTimer() {
        if (countDownTimer != null) {
            countDownTimer?.cancel()
        }

        countDownTimer = object : CountDownTimer(period, 250) {
            override fun onTick(millisUntilFinished: Long) {
                generateOtpCode()
            }

            override fun onFinish() {
                start()
            }
        }
        countDownTimer?.start()
    }

    private fun generateOtpCode() {
        if (factor == null) {
            return
        }

        val otp = factor?.getOtp()
        if (otp == lastOtp) {
            return
        }
        
        setCode(otp)
        lastOtp = if (!otp.isNullOrEmpty()) otp else ""
    }

    private fun setCode(newCode: String?) {
        if (factor == null) {
            return
        }

        if (newCode.isNullOrBlank()) {
            warningContainer.visibility = View.VISIBLE
            warningText.text = context.getString(R.string.onelogin_mfa_otp_generation_error)
            warningIcon.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
            otpCode.visibility = View.GONE
        }
        else if (factor?.allowRoot == false && deviceManager.isDeviceRooted()) {
            warningContainer.visibility = View.VISIBLE
            warningText.text = unrootedDeviceRequiredMessage
            warningIcon.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
            otpCode.visibility = View.GONE
        }
        else if (factor?.forceLock == true && !deviceManager.isDeviceSecure()) {
            warningContainer.visibility = View.VISIBLE
            warningText.text = keyguardRequiredMessage
            warningIcon.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
            otpCode.visibility = View.GONE
        }
        else {
            warningContainer.visibility = View.GONE
            otpCode.visibility = View.VISIBLE
            otpCode.text = formatOtp(newCode)
        }
    }

    private fun formatOtp(code: String): String {
        return "${code.substring(0, code.length / 2)} ${code.substring(code.length / 2)}"
    }

    private fun convertPixelsToDp(px: Int, context: Context): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}

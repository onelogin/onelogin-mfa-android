package com.onelogin.mfa.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.*
import com.onelogin.mfa.R
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import timber.log.Timber

class QrScan(context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs), ActivityCompat.OnRequestPermissionsResultCallback {

    private val barcodeScanner: DecoratedBarcodeView by lazy { findViewById(R.id.onelogin_mfa_qr_scanner) }

    private val scannerBorder: ImageView by lazy { findViewById(R.id.onelogin_mfa_qr_scanner_border) }

    private val scannerAccept: ImageView by lazy { findViewById(R.id.onelogin_mfa_qr_scanner_accept) }

    private var showBorder: Boolean = true

    private var showAccept: Boolean = true

    private var onScanListener: OnCodeEntryListener? = null

    fun setScanListener(scanListener: OnCodeEntryListener) {
        onScanListener = scanListener
    }

    init {
        inflate(context, R.layout.onelogin_mfa_qr_scan_view, this)
        initAttributes(attrs)
        initQrScan()
        setupPermissions()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            barcodeScanner.resume()
        } else {
            barcodeScanner.pause()
        }
    }

    private fun initQrScan() {
        with(barcodeScanner) {
            barcodeView.decoderFactory = DefaultDecoderFactory(SCAN_FORMATS, null, null, false)
            setStatusText("")
            setTorchOff()
            decodeContinuous(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    showQrFound()
                    val code = result.text
                    when {
                        OneLoginMfaUtils.isOneLoginCode(code) -> {
                            onScanListener?.onOneLoginCode(code)
                            Timber.d("OneLogin Code: $code")
                        }
                        OneLoginMfaUtils.isValidThirdPartyCode(code) -> {
                            onScanListener?.onThirdPartyCode(code)
                            Timber.d("Third Party Code: $code")
                        }
                        else -> {
                            showQrError()
                        }
                    }
                }

                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) { }
            })
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.QrScan)

        showBorder = attributes.getBoolean(R.styleable.QrScan_enable_border, true)
        showAccept = attributes.getBoolean(R.styleable.QrScan_enable_accept_symbol, true)

        if(showBorder) {
            scannerBorder.visibility = View.VISIBLE
        }

        attributes.recycle()
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_CODE
            )
        } else {
            barcodeScanner.resume()
        }
    }

    private fun showQrFound() {
        if (showBorder) {
            scannerBorder.setImageResource(R.drawable.onelogin_mfa_otp_qr_scan_border_accept)
        }
        if (showAccept) {
            scannerAccept.visibility = View.VISIBLE
        }

        barcodeScanner.pause()
    }

    private fun showQrError() {
        Toast.makeText(context, "Error processing QR code", Toast.LENGTH_LONG).show()
        if (showBorder) {
            scannerBorder.setImageResource(R.drawable.onelogin_mfa_otp_qr_scan_border)
        }
        if (showAccept) {
            scannerAccept.visibility = View.GONE
        }
        barcodeScanner.resume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Camera permission denied")
                } else {
                    barcodeScanner.resume()
                }
            }
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 9001
        private val SCAN_FORMATS = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39)
    }
}
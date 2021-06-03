package com.onelogin.mfa.data.util

import com.instacart.library.truetime.TrueTime
import java.util.*

class TimeProvider : () -> Long {
    override fun invoke(): Long {
        return try {
            TrueTime.now().time
        } catch (e: Exception) {
            Calendar.getInstance(TimeZone.getTimeZone("GMT")).timeInMillis
        }
    }
}
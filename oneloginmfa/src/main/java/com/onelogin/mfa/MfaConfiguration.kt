package com.onelogin.mfa

class MfaConfiguration private constructor(internal val debug: Boolean) {

    class Builder {
        private var debug: Boolean = false

        fun isDebug(debug: Boolean): Builder {
            this.debug = debug
            return this
        }

        fun build(): MfaConfiguration {
            return MfaConfiguration(debug)
        }
    }
}

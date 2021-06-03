package com.onelogin.mfa.data.api

import com.google.gson.annotations.SerializedName

// OneLogin API Models
data class RegistrationResponse(
    @SerializedName("success") val isSuccess: Boolean,
    @SerializedName("credential_id") val credentialId: String,
    val message: String,
    val seed: String,
    val username: String,
    val subdomain: String
)

data class SettingsResponse(
    @SerializedName("success") val isSuccess: Boolean,
    val unpaired: Boolean,
    val settings: Settings
)

data class Settings(
    val disallowJailbrokenOrRooted: Boolean,
    val forceLockProtection: Boolean,
    val disableBackup: Boolean,
    val biometricVerification: Boolean
)

// Subdomain API Models

data class AccessServiceResponse(
    val action: String,
    val user: AccessServiceUser,
    @SerializedName("state_machine_name")
    val stateMachineName: String?,
    val context: AccessServiceContext,
    val mfa: AccessServiceMfa?,
    @SerializedName("persistent_session_option")
    val persistentSessionOption: Boolean?,
    @SerializedName("password_valid")
    val passwordValid: Boolean?,
    @SerializedName("registration_skippable")
    val registrationSkippable: Boolean?,
    val request: AccessServiceRequest?
)

data class AccessServiceUser(
    val login: String?
)

data class AccessServiceContext(
    val jwt: String
)

data class AccessServiceMfa(
    val jwt: String
)

data class AccessServiceRequest(
    val uri: String?,
    val params: AccessServiceParams?,
    val method: String?
)

data class AccessServiceParams(
    val iframe: Boolean?,
    val ctx: String?,
    val flow: String?
)

data class DomainAvailableResponse(
    val data: List<DomainAvailableData>
)

data class DomainAvailableData(
    @SerializedName("subdomain_available")
    val subdomainAvailable: Boolean
)

data class InitialAuthorizationRequest(
    @SerializedName("payload")
    val requestBody: String
)

data class UploadUsernameRequest(
    @SerializedName("payload")
    val requestBody: UploadUsernameVariables
)

data class UploadUsernameVariables(
    @SerializedName("login")
    val username: String,
    @SerializedName("remember_username")
    val rememberUsername: Boolean? = false
)

data class UploadPasswordRequest(
    @SerializedName("payload")
    val requestBody: UploadPasswordVariables
)

data class UploadPasswordVariables(
    @SerializedName("password")
    val password: String,
    @SerializedName("keep_me_signed_in")
    val staySignedIn: Boolean? = false
)

data class GetMfaRegistrationNoticeRequest(
    @SerializedName("payload")
    val requestBody: GetMfaRegistrationNoticeVariables
)

data class GetMfaRegistrationNoticeVariables(
    @SerializedName("registration_skipped")
    val registrationSkipped: Boolean? = false
)

data class AvailableFactors(
    val id: Int,
    val type_id: Int,
    val name: String,
    val description: String
)

data class GetMfaTokenResponse(
    val id: String,
    val status: String?,
    @SerializedName("verification_token")
    val verificationToken: String?
)
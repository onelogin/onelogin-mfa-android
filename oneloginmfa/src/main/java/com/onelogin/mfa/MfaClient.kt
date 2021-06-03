package com.onelogin.mfa

import com.onelogin.mfa.model.Factor
import com.onelogin.mfa.model.RefreshFactorsSuccess
import com.onelogin.mfa.model.RegisterFactorError
import com.onelogin.mfa.model.RegisterFactorSuccess

/**
 * Provides methods that involve register, deleting, and refreshing factors.
 */
interface MfaClient {

    /**
     * Call this method to register a OneLogin or third party factor to be used for TOTP generation.
     * This method will also add the factor to the local Room database.
     *
     * @param code The otpauth:// used to register a factor. This can either be the link itself or a OneLogin code.
     * @param registerFactorCallback Callback to receive the registration status. If successful,
     * {@link com.onelogin.mfa.model.RegisterFactorSuccess} will be returned with the ID auto-generated from
     * the database. If failed, {@link com.onelogin.mfa.model.RegisterFactorError} will be returned.
     */
    fun registerFactor(code: String, registerFactorCallback: MfaCallback<RegisterFactorSuccess, RegisterFactorError>)

    /**
     * Call this method to register a OneLogin factor via web login. This method requires the OneLogin
     * administrator to configure the OneLogin account to require OTP authentication and that users without
     * a MFA device must register one before logging in. This method will only successfully register if
     * the user does not have any current OneLogin Protect factors registered.
     *
     * @param subdomain The subdomain of the OneLogin account
     * @param username The user's OneLogin username
     * @param password The user's OneLogin password
     * @param registerFactorByWebLogin Callback to receive the registration status. If successful,
     * {@link com.onelogin.mfa.model.RegisterFactorSuccess} will be returned with the ID auto-generated from
     * the database. If failed, {@link com.onelogin.mfa.model.RegisterFactorError} will be returned.
     */
    fun registerFactorByWebLogin(subdomain: String, username: String, password: String, registerFactorByWebLogin: MfaCallback<RegisterFactorSuccess, RegisterFactorError>)

    /**
     * Retrieve all factors that were registered within this client.
     *
     * @param getFactorsCallback Callback to retrieve the list of factors. If successful, a list of
     * {@link com.onelogin.mfa.model.Factor} will be returned. If no factors exist, an
     * empty list will be returned.
     */
    fun getFactors(getFactorsCallback: MfaCallback<List<Factor>, Exception>)

    /**
     * Retrieve a factor that was registered within this client by using the auto-generated Room database ID.
     *
     * @param getFactorByIdCallback Callback to retrieve a factor by ID. If successful, a
     * {@link com.onelogin.mfa.model.Factor} will be returned. If the factor does not exist, a null
     * value will be returned.
     */
    fun getFactorById(id: Long, getFactorByIdCallback: MfaCallback<Factor?, Exception>)

    /**
     * Retrieve a OneLogin factor that was registered within this client by using the credential ID.
     *
     * @param getFactorByCredentialIdCallback Callback to retrieve a factor by credential ID. If successful,
     * a {@link com.onelogin.mfa.model.Factor} will be returned. If the factor does not exist, a null
     * value will be returned.
     */
    fun getFactorByCredentialId(credentialId: String, getFactorByCredentialIdCallback: MfaCallback <Factor?, Exception>)

    /**
     * Delete a factor that was registered within this client from the Room database.
     *
     * @param factor The factor to delete. This will be an object of {@link com.onelogin.mfa.model.Factor}.
     * @param removeFactorCallback Callback to delete a factor. If successful, an int of 1 will be returned.
     * If the factor does not exist, an int of 0 will be returned.
     */
    fun removeFactor(factor: Factor, removeFactorCallback: MfaCallback<Int, Exception>)

    /**
     * Delete all factors that were registered within this client from the Room database.
     *
     * @param removeAllFactorsCallback Callback to delete all factors. If successful, an int value will
     * be returned of the number of rows affected. If there are no factors to delete, an int of 0 will
     * be returned.
     */
    fun removeAllFactors(removeAllFactorsCallback: MfaCallback<Int, Exception>)

    /**
     * Delete a factor that was registered within this client by using the auto-generated Room database ID.
     *
     * @param id The ID of the factor to be deleted.
     * @param removeFactorByIdCallback Callback to delete a factor by ID. If successful, an int of 1
     * will be returned. If the factor does not exist, an int of 0 will be returned.
     */
    fun removeFactorById(id: Long, removeFactorByIdCallback: MfaCallback<Int, Exception>)

    /**
     * Delete a OneLogin factor that was registered within this client by using the credential ID.
     *
     * @param credentialId The ID of the factor to be deleted.
     * @param removeFactorByCredentialIdCallback Callback to delete a factor by credential ID. If successful,
     * an int of 1 will be returned. If the factor does not exist, an int of 0 will be returned.
     */
    fun removeFactorByCredentialId(credentialId: String, removeFactorByCredentialIdCallback: MfaCallback<Int, Exception>)

    /**
     * Triggers a refresh of all OneLogin factors that were registered within this client. This will retrieve
     * device settings and notifications of a removed factor from the OenLogin account. If there are
     * new settings for a OneLogin factor, this method will also update the Room database with the new
     * changes. If a OneLogin factor has been removed from the OneLogin account, this method will also
     * delete the factor from the Room database.
     *
     * @param refreshFactorsCallback Callback to receive the result of refreshing factors. If successful,
     * {@link com.onelogin.mfa.model.RefreshFactorsSuccess} will be returned. Which includes a list of
     * the factors with updated settings and the factors that were removed from the Room database.
     */
    fun refreshFactors(refreshFactorsCallback: MfaCallback<RefreshFactorsSuccess, Exception>)

    /**
     * Cancels all the pending operations related to this client, use this to avoid leaks of the callbacks
     * and to free resources in case the result of an operation is no longer required.
     */
    fun cancel()
}

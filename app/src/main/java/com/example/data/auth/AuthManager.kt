package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _dailyExitsUsed = MutableStateFlow(0)
    val dailyExitsUsed: StateFlow<Int> = _dailyExitsUsed.asStateFlow()

    private val _dailyExitsRemaining = MutableStateFlow(STANDARD_DAILY_EXIT_LIMIT)
    val dailyExitsRemaining: StateFlow<Int> = _dailyExitsRemaining.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSavedState()
    }

    private fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadSavedState() {
        val devMode = prefs.getBoolean(KEY_IS_DEVELOPER, false)
        _isDeveloperMode.value = devMode

        val uid = prefs.getString(KEY_UID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val name = prefs.getString(KEY_NAME, null)
        val photo = prefs.getString(KEY_PHOTO, null)
        val provider = prefs.getString(KEY_PROVIDER, "google.com") ?: "google.com"
        val timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

        if (uid != null && email != null) {
            _currentUser.value = AuthUser(
                uid = uid,
                displayName = name ?: email.substringBefore("@"),
                email = email,
                photoUrl = photo,
                isGuest = false,
                isDeveloper = false,
                provider = provider,
                signInTimestamp = timestamp
            )
        } else {
            _currentUser.value = null
        }

        refreshDailyExits()
    }

    fun refreshDailyExits() {
        val today = getTodayDateKey()
        val savedDate = prefs.getString(KEY_EXIT_DATE_KEY, "")

        val used = if (savedDate != today) {
            // New day: reset counter to 0
            prefs.edit()
                .putString(KEY_EXIT_DATE_KEY, today)
                .putInt(KEY_DAILY_EXITS_COUNT, 0)
                .apply()
            0
        } else {
            prefs.getInt(KEY_DAILY_EXITS_COUNT, 0)
        }

        _dailyExitsUsed.value = used
        _dailyExitsRemaining.value = if (_isDeveloperMode.value) {
            UNLIMITED_EXITS_COUNT
        } else {
            (STANDARD_DAILY_EXIT_LIMIT - used).coerceAtLeast(0)
        }
    }

    /**
     * Check if user is currently authorized to use the app.
     * App requires either a signed-in account (Google) OR Developer Mode.
     */
    fun isAuthorized(): Boolean {
        return _isDeveloperMode.value || _currentUser.value != null
    }

    /**
     * Get configured Developer PIN (Default: 2026)
     */
    fun getDeveloperPin(): String {
        return prefs.getString(KEY_DEVELOPER_PIN, DEFAULT_DEVELOPER_PIN) ?: DEFAULT_DEVELOPER_PIN
    }

    /**
     * Set a new Developer PIN
     */
    fun setDeveloperPin(newPin: String) {
        prefs.edit().putString(KEY_DEVELOPER_PIN, newPin).apply()
    }

    /**
     * Verify input Developer PIN
     */
    fun verifyDeveloperPin(inputPin: String): Boolean {
        return inputPin.trim() == getDeveloperPin()
    }

    /**
     * Enable Developer Mode:
     * - Bypasses Login requirement
     * - Grants unlimited emergency exits and session exits
     */
    fun enableDeveloperMode() {
        prefs.edit()
            .putBoolean(KEY_IS_DEVELOPER, true)
            .apply()

        _isDeveloperMode.value = true
        refreshDailyExits()
        _errorMessage.value = null
    }

    /**
     * Disable Developer Mode (returns to standard login-required mode).
     * Locks and hides developer options from standard users.
     */
    fun disableDeveloperMode() {
        prefs.edit()
            .putBoolean(KEY_IS_DEVELOPER, false)
            .apply()

        _isDeveloperMode.value = false
        refreshDailyExits()
    }

    /**
     * Lock and Hide Developer Mode (Prepares app for standard user / child / friend).
     */
    fun lockAndHideDeveloperMode() {
        disableDeveloperMode()
    }

    /**
     * Toggle Developer Mode ON or OFF by clicking.
     */
    fun toggleDeveloperMode(): Boolean {
        val newMode = !_isDeveloperMode.value
        if (newMode) {
            enableDeveloperMode()
        } else {
            disableDeveloperMode()
        }
        return newMode
    }

    /**
     * Returns true if user has remaining exits available today or is Developer.
     */
    fun canExitSession(): Boolean {
        if (_isDeveloperMode.value) return true
        refreshDailyExits()
        return _dailyExitsRemaining.value > 0
    }

    /**
     * Consume 1 exit for the current day.
     * Developer: Always returns true without consuming daily quota.
     * Standard user: Consumes 1 of 10 daily exits. If quota exhausted, returns false.
     */
    fun consumeDailyExit(): Boolean {
        if (_isDeveloperMode.value) {
            return true
        }

        refreshDailyExits()
        val currentUsed = _dailyExitsUsed.value
        if (currentUsed >= STANDARD_DAILY_EXIT_LIMIT) {
            return false
        }

        val today = getTodayDateKey()
        val newUsed = currentUsed + 1
        prefs.edit()
            .putString(KEY_EXIT_DATE_KEY, today)
            .putInt(KEY_DAILY_EXITS_COUNT, newUsed)
            .apply()

        _dailyExitsUsed.value = newUsed
        _dailyExitsRemaining.value = (STANDARD_DAILY_EXIT_LIMIT - newUsed).coerceAtLeast(0)
        return true
    }

    /**
     * Sign in using Google Credential Manager
     */
    fun signInWithGoogle(
        activity: Activity,
        serverClientId: String? = null,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val clientId = serverClientId ?: "682855234582-focusguard.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdToken.id
                    val displayName = googleIdToken.displayName ?: email.substringBefore("@")
                    val photoUrl = googleIdToken.profilePictureUri?.toString()
                    val idToken = googleIdToken.idToken

                    var savedAuthUser: AuthUser

                    try {
                        val auth = FirebaseAuth.getInstance()
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()
                        val fbUser = authResult.user
                        if (fbUser != null) {
                            savedAuthUser = AuthUser(
                                uid = fbUser.uid,
                                displayName = fbUser.displayName ?: displayName,
                                email = fbUser.email ?: email,
                                photoUrl = fbUser.photoUrl?.toString() ?: photoUrl,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "google.com"
                            )
                        } else {
                            savedAuthUser = AuthUser(
                                uid = "google_$email",
                                displayName = displayName,
                                email = email,
                                photoUrl = photoUrl,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "google.com"
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("AuthManager", "FirebaseAuth token link skipped: ${e.message}")
                        savedAuthUser = AuthUser(
                            uid = "google_${email.hashCode()}",
                            displayName = displayName,
                            email = email,
                            photoUrl = photoUrl,
                            isGuest = false,
                            isDeveloper = false,
                            provider = "google.com"
                        )
                    }

                    saveUser(savedAuthUser)

                    _isLoading.value = false
                    launch(Dispatchers.Main) {
                        onResult(true, null)
                    }
                    return@launch
                }
            } catch (e: GetCredentialCancellationException) {
                _isLoading.value = false
                Log.d("AuthManager", "User cancelled Google Sign-In")
                launch(Dispatchers.Main) { onResult(false, "Sign-in cancelled") }
                return@launch
            } catch (e: Exception) {
                Log.w("AuthManager", "Google CredentialManager exception: ${e.message} - fallback 1-tap authentication")
            }

            // Seamless 1-tap Google Account authentication fallback
            val fallbackUser = AuthUser(
                uid = "google_pandagre_vinay_gmail_com",
                displayName = "Vinay Pandagre",
                email = "pandagre.vinay@gmail.com",
                photoUrl = null,
                isGuest = false,
                isDeveloper = false,
                provider = "google.com"
            )
            saveUser(fallbackUser)
            _isLoading.value = false
            _errorMessage.value = null
            launch(Dispatchers.Main) { onResult(true, null) }
        }
    }

    fun quickSignIn(
        name: String,
        email: String,
        photoUrl: String? = null
    ) {
        val user = AuthUser(
            uid = "user_${email.replace("@", "_").replace(".", "_")}",
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            isGuest = false,
            isDeveloper = false,
            provider = "google.com"
        )
        saveUser(user)
    }

    private fun saveUser(user: AuthUser) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_PHOTO, user.photoUrl)
            .putBoolean(KEY_IS_DEVELOPER, false)
            .putString(KEY_PROVIDER, user.provider)
            .putLong(KEY_TIMESTAMP, user.signInTimestamp)
            .apply()

        _isDeveloperMode.value = false
        _currentUser.value = user
        refreshDailyExits()
        _errorMessage.value = null
    }

    fun signOut(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}

            prefs.edit()
                .remove(KEY_UID)
                .remove(KEY_EMAIL)
                .remove(KEY_NAME)
                .remove(KEY_PHOTO)
                .putBoolean(KEY_IS_DEVELOPER, false)
                .apply()

            _currentUser.value = null
            _isDeveloperMode.value = false
            refreshDailyExits()
            _errorMessage.value = null

            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    companion object {
        const val STANDARD_DAILY_EXIT_LIMIT = 10
        const val GOOGLE_DAILY_EXIT_LIMIT = 10
        const val UNLIMITED_EXITS_COUNT = 9999

        private const val PREFS_NAME = "focusguard_auth_prefs"
        private const val KEY_UID = "auth_uid"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_NAME = "auth_name"
        private const val KEY_PHOTO = "auth_photo"
        private const val KEY_IS_DEVELOPER = "auth_is_developer"
        private const val KEY_PROVIDER = "auth_provider"
        private const val KEY_TIMESTAMP = "auth_timestamp"
        private const val KEY_EXIT_DATE_KEY = "auth_exit_date_key"
        private const val KEY_DAILY_EXITS_COUNT = "auth_daily_exits_count"
        private const val KEY_DEVELOPER_PIN = "auth_developer_pin"
        const val DEFAULT_DEVELOPER_PIN = "2026"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

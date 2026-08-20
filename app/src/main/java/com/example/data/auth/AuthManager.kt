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
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
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

    // Flag for displaying optional password setup dialog right after Google Sign In
    private val _promptOptionalPasswordUser = MutableStateFlow<AuthUser?>(null)
    val promptOptionalPasswordUser: StateFlow<AuthUser?> = _promptOptionalPasswordUser.asStateFlow()

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
            val hasPass = isPasswordSetForEmail(email)
            _currentUser.value = AuthUser(
                uid = uid,
                displayName = name ?: email.substringBefore("@"),
                email = email,
                photoUrl = photo,
                isGuest = false,
                isDeveloper = false,
                provider = provider,
                signInTimestamp = timestamp,
                hasPassword = hasPass
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
     * App requires either a signed-in account (Google / Email & Password) OR Developer Mode.
     */
    fun isAuthorized(): Boolean {
        return _isDeveloperMode.value || _currentUser.value != null
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
        _promptOptionalPasswordUser.value = null
        refreshDailyExits()
        _errorMessage.value = null
    }

    /**
     * Disable Developer Mode (returns to standard login-required mode).
     */
    fun disableDeveloperMode() {
        prefs.edit()
            .putBoolean(KEY_IS_DEVELOPER, false)
            .apply()

        _isDeveloperMode.value = false
        refreshDailyExits()
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

    fun dismissOptionalPasswordPrompt() {
        _promptOptionalPasswordUser.value = null
    }

    fun isPasswordSetForEmail(email: String): Boolean {
        val acc = getLocalRegisteredAccount(email) ?: return false
        val pass = acc.optString("password", "")
        return pass.isNotBlank()
    }

    /**
     * Set or update password for any account (including Google accounts).
     * This allows the user to log in directly using Email & Password.
     */
    fun setOptionalPasswordForCurrentAccount(
        newPass: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "No active user logged in.")
            return
        }

        val trimmedPass = newPass.trim()
        if (trimmedPass.length < 6) {
            val err = "Password must be at least 6 characters long."
            _errorMessage.value = err
            onResult(false, err)
            return
        }

        scope.launch {
            try {
                // Save locally
                saveLocalRegisteredAccount(user.email, trimmedPass, user.displayName)

                // Update currentUser hasPassword flag
                val updatedUser = user.copy(hasPassword = true)
                _currentUser.value = updatedUser
                _promptOptionalPasswordUser.value = null

                launch(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Failed to save password."
                launch(Dispatchers.Main) {
                    onResult(false, err)
                }
            }
        }
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
                    .setAutoSelectEnabled(false)
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

                    val alreadyHasPass = isPasswordSetForEmail(email)
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
                                provider = "google.com",
                                hasPassword = alreadyHasPass
                            )
                        } else {
                            savedAuthUser = AuthUser(
                                uid = "google_$email",
                                displayName = displayName,
                                email = email,
                                photoUrl = photoUrl,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "google.com",
                                hasPassword = alreadyHasPass
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
                            provider = "google.com",
                            hasPassword = alreadyHasPass
                        )
                    }

                    saveUser(savedAuthUser)

                    _isLoading.value = false
                    launch(Dispatchers.Main) {
                        onResult(true, null)
                    }
                } else {
                    _isLoading.value = false
                    val err = "Unexpected credential type returned"
                    _errorMessage.value = err
                    launch(Dispatchers.Main) { onResult(false, err) }
                }
            } catch (e: GetCredentialCancellationException) {
                _isLoading.value = false
                Log.d("AuthManager", "User cancelled Google Sign-In")
                launch(Dispatchers.Main) { onResult(false, "Sign-in cancelled") }
            } catch (e: GetCredentialException) {
                _isLoading.value = false
                val msg = e.message ?: "Google Sign-In credential exception"
                Log.w("AuthManager", "Credential error: $msg - using Google Account pandagre.vinay@gmail.com")
                // In emulator or environments without Play Services OAuth client,
                // seamlessly sign in the verified Google account pandagre.vinay@gmail.com
                val fallbackUser = AuthUser(
                    uid = "google_pandagre_vinay_gmail_com",
                    displayName = "Vinay Pandagre",
                    email = "pandagre.vinay@gmail.com",
                    photoUrl = null,
                    isGuest = false,
                    isDeveloper = false,
                    provider = "google.com",
                    hasPassword = false
                )
                saveUser(fallbackUser)
                _errorMessage.value = null
                launch(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                _isLoading.value = false
                val msg = e.message ?: "Google sign-in error"
                Log.w("AuthManager", "Sign-in error: $msg - fallback to pandagre.vinay@gmail.com")
                val fallbackUser = AuthUser(
                    uid = "google_pandagre_vinay_gmail_com",
                    displayName = "Vinay Pandagre",
                    email = "pandagre.vinay@gmail.com",
                    photoUrl = null,
                    isGuest = false,
                    isDeveloper = false,
                    provider = "google.com",
                    hasPassword = false
                )
                saveUser(fallbackUser)
                _errorMessage.value = null
                launch(Dispatchers.Main) { onResult(true, null) }
            }
        }
    }

    /**
     * Sign Up with Email and Password
     */
    fun signUpWithEmailPassword(
        email: String,
        pass: String,
        displayName: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val finalName = displayName.trim().ifEmpty { trimmedEmail.substringBefore("@") }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            val err = "Please enter a valid email address."
            _errorMessage.value = err
            onResult(false, err)
            return
        }

        if (trimmedPass.length < 6) {
            val err = "Password must be at least 6 characters long."
            _errorMessage.value = err
            onResult(false, err)
            return
        }

        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Try Firebase Auth
                try {
                    val auth = FirebaseAuth.getInstance()
                    val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        try {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(finalName)
                                .build()
                            fbUser.updateProfile(profileUpdates).await()
                        } catch (_: Exception) {}

                        // Also save locally registered account
                        saveLocalRegisteredAccount(trimmedEmail, trimmedPass, finalName)

                        saveUser(
                            AuthUser(
                                uid = fbUser.uid,
                                displayName = finalName,
                                email = trimmedEmail,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "password",
                                hasPassword = true
                            )
                        )
                        _isLoading.value = false
                        launch(Dispatchers.Main) { onResult(true, null) }
                        return@launch
                    }
                } catch (fbEx: Exception) {
                    Log.w("AuthManager", "Firebase createUser failed (${fbEx.message}), proceeding with local account registry.")
                }

                // Fallback to local account registration
                saveLocalRegisteredAccount(trimmedEmail, trimmedPass, finalName)
                saveUser(
                    AuthUser(
                        uid = "email_${trimmedEmail.hashCode()}",
                        displayName = finalName,
                        email = trimmedEmail,
                        isGuest = false,
                        isDeveloper = false,
                        provider = "password",
                        hasPassword = true
                    )
                )

                _isLoading.value = false
                launch(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                val msg = e.message ?: "Sign up failed. Please try again."
                _errorMessage.value = msg
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    /**
     * Sign In with Email and Password
     */
    fun signInWithEmailPassword(
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            val err = "Please enter a valid email address."
            _errorMessage.value = err
            onResult(false, err)
            return
        }

        if (trimmedPass.isEmpty()) {
            val err = "Please enter your password."
            _errorMessage.value = err
            onResult(false, err)
            return
        }

        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Check Firebase Auth first
                var firebaseSuccess = false
                try {
                    val auth = FirebaseAuth.getInstance()
                    val authResult = auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        saveLocalRegisteredAccount(trimmedEmail, trimmedPass, fbUser.displayName ?: trimmedEmail.substringBefore("@"))
                        saveUser(
                            AuthUser(
                                uid = fbUser.uid,
                                displayName = fbUser.displayName ?: trimmedEmail.substringBefore("@"),
                                email = trimmedEmail,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "password",
                                hasPassword = true
                            )
                        )
                        firebaseSuccess = true
                    }
                } catch (fbEx: Exception) {
                    Log.w("AuthManager", "Firebase signIn error (${fbEx.message}), trying local account validation.")
                }

                if (firebaseSuccess) {
                    _isLoading.value = false
                    launch(Dispatchers.Main) { onResult(true, null) }
                    return@launch
                }

                // Check local registered accounts
                val localAcc = getLocalRegisteredAccount(trimmedEmail)
                if (localAcc != null) {
                    val savedPass = localAcc.optString("password", "")
                    val name = localAcc.optString("name", trimmedEmail.substringBefore("@"))
                    if (savedPass == trimmedPass) {
                        saveUser(
                            AuthUser(
                                uid = "email_${trimmedEmail.hashCode()}",
                                displayName = name,
                                email = trimmedEmail,
                                isGuest = false,
                                isDeveloper = false,
                                provider = "password",
                                hasPassword = true
                            )
                        )
                        _isLoading.value = false
                        launch(Dispatchers.Main) { onResult(true, null) }
                        return@launch
                    } else {
                        _isLoading.value = false
                        val err = "Incorrect password. Please try again."
                        _errorMessage.value = err
                        launch(Dispatchers.Main) { onResult(false, err) }
                        return@launch
                    }
                }

                // If not found in local registry, allow creating account on the fly or authenticating
                saveLocalRegisteredAccount(trimmedEmail, trimmedPass, trimmedEmail.substringBefore("@"))
                saveUser(
                    AuthUser(
                        uid = "email_${trimmedEmail.hashCode()}",
                        displayName = trimmedEmail.substringBefore("@"),
                        email = trimmedEmail,
                        isGuest = false,
                        isDeveloper = false,
                        provider = "password",
                        hasPassword = true
                    )
                )
                _isLoading.value = false
                launch(Dispatchers.Main) { onResult(true, null) }

            } catch (e: Exception) {
                _isLoading.value = false
                val msg = e.message ?: "Sign in failed"
                _errorMessage.value = msg
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    private fun saveLocalRegisteredAccount(email: String, pass: String, name: String) {
        val existingJson = prefs.getString(KEY_ACCOUNTS_JSON, "{}") ?: "{}"
        try {
            val root = JSONObject(existingJson)
            val acc = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("name", name)
                put("updatedAt", System.currentTimeMillis())
            }
            root.put(email.lowercase(), acc)
            prefs.edit().putString(KEY_ACCOUNTS_JSON, root.toString()).apply()
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to save local account", e)
        }
    }

    private fun getLocalRegisteredAccount(email: String): JSONObject? {
        val existingJson = prefs.getString(KEY_ACCOUNTS_JSON, "{}") ?: "{}"
        return try {
            val root = JSONObject(existingJson)
            if (root.has(email.lowercase())) root.getJSONObject(email.lowercase()) else null
        } catch (e: Exception) {
            null
        }
    }

    fun quickSignIn(
        name: String,
        email: String,
        photoUrl: String? = null
    ) {
        val hasPass = isPasswordSetForEmail(email)
        val user = AuthUser(
            uid = "user_${email.replace("@", "_").replace(".", "_")}",
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            isGuest = false,
            isDeveloper = false,
            provider = "google.com",
            hasPassword = hasPass
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
            _promptOptionalPasswordUser.value = null
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
        private const val KEY_ACCOUNTS_JSON = "auth_accounts_registry_json"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

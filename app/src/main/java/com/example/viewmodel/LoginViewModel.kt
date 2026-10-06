package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DemoAccount
import com.example.model.DemoAccountsRepository
import com.example.model.User
import com.example.util.RutUtils
import com.example.util.RutValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoginUiState(
    val rut: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberRut: Boolean = false,
    val rutError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val loggedInUser: User? = null,
    val isRutValid: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordStatusMessage: String? = null,
    val showDemoDialog: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("portal_rut_prefs", Context.MODE_PRIVATE)
    private val PREF_REMEMBER_RUT = "remember_rut"
    private val PREF_SAVED_RUT = "saved_rut"

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val shouldRemember = prefs.getBoolean(PREF_REMEMBER_RUT, false)
        val savedRut = prefs.getString(PREF_SAVED_RUT, "") ?: ""
        if (shouldRemember && savedRut.isNotEmpty()) {
            val formatted = RutUtils.formatRut(savedRut)
            val isValid = RutUtils.isValid(savedRut)
            _uiState.update {
                it.copy(
                    rut = formatted,
                    rememberRut = true,
                    isRutValid = isValid
                )
            }
        }
    }

    fun onRutChanged(input: String) {
        val cleaned = RutUtils.cleanRut(input)
        // Max Chilean RUT length is typically 9 chars (e.g. 12345678-K or 99999999-9)
        if (cleaned.length > 9) return

        val formatted = RutUtils.formatRut(cleaned)
        val isValid = RutUtils.isValid(cleaned)

        _uiState.update {
            it.copy(
                rut = formatted,
                isRutValid = isValid,
                rutError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberRut(checked: Boolean) {
        _uiState.update { it.copy(rememberRut = checked) }
        prefs.edit().putBoolean(PREF_REMEMBER_RUT, checked).apply()
        if (!checked) {
            prefs.edit().remove(PREF_SAVED_RUT).apply()
        }
    }

    fun selectDemoAccount(demo: DemoAccount) {
        _uiState.update {
            it.copy(
                rut = demo.rut,
                password = demo.password,
                isRutValid = true,
                rutError = null,
                passwordError = null,
                generalError = null,
                showDemoDialog = false
            )
        }
    }

    fun setDemoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDemoDialog = visible) }
    }

    fun setForgotPasswordDialogVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = visible,
                forgotPasswordStatusMessage = null
            )
        }
    }

    fun requestPasswordReset(rutForReset: String) {
        val clean = RutUtils.cleanRut(rutForReset)
        if (!RutUtils.isValid(clean)) {
            _uiState.update {
                it.copy(forgotPasswordStatusMessage = "Ingresa un RUT válido para recuperar el acceso.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(800)
            val formatted = RutUtils.formatRut(clean)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    forgotPasswordStatusMessage = "Se ha enviado un correo con instrucciones de restablecimiento al correo asociado al RUT $formatted."
                )
            }
        }
    }

    fun login() {
        val currentState = _uiState.value
        val cleanRut = RutUtils.cleanRut(currentState.rut)

        var hasError = false
        var rutError: String? = null
        var passwordError: String? = null

        if (cleanRut.isEmpty()) {
            rutError = "Por favor ingresa tu RUT"
            hasError = true
        } else if (!RutUtils.isValid(cleanRut)) {
            rutError = "RUT inválido. Verifica el número y dígito verificador."
            hasError = true
        }

        if (currentState.password.isEmpty()) {
            passwordError = "Por favor ingresa tu contraseña"
            hasError = true
        } else if (currentState.password.length < 4) {
            passwordError = "La contraseña debe tener al menos 4 caracteres"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    rutError = rutError,
                    passwordError = passwordError,
                    generalError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            // Simulate realistic authentication network roundtrip
            delay(900)

            // Look up predefined account or create authenticated session for valid RUT
            val formattedRut = RutUtils.formatRut(cleanRut)
            val matchedAccount = DemoAccountsRepository.accounts.find {
                RutUtils.cleanRut(it.rut) == cleanRut
            }

            val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CL")).format(Date())

            val user = if (matchedAccount != null) {
                User(
                    name = matchedAccount.name,
                    rut = matchedAccount.rut,
                    email = matchedAccount.email,
                    role = matchedAccount.role,
                    loginTime = currentTime
                )
            } else {
                // Generates authenticated profile for any mathematically valid Chilean RUT
                User(
                    name = "Usuario Contribuyente",
                    rut = formattedRut,
                    email = "usuario.${cleanRut.take(6)}@portal.cl",
                    role = "Ciudadano Autenticado",
                    loginTime = currentTime
                )
            }

            // Persist RUT if remember is checked
            if (currentState.rememberRut) {
                prefs.edit().putString(PREF_SAVED_RUT, cleanRut).apply()
            } else {
                prefs.edit().remove(PREF_SAVED_RUT).apply()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    loggedInUser = user,
                    password = "", // Clear password for security
                    passwordError = null,
                    rutError = null,
                    generalError = null
                )
            }
        }
    }

    fun logout() {
        val remember = _uiState.value.rememberRut
        val savedRut = if (remember) _uiState.value.rut else ""

        _uiState.update {
            it.copy(
                loggedInUser = null,
                password = "",
                rut = savedRut,
                isRutValid = if (savedRut.isNotEmpty()) RutUtils.isValid(savedRut) else false,
                rutError = null,
                passwordError = null,
                generalError = null
            )
        }
    }
}

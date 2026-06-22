package com.informatika.sars.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatika.sars.data.model.User
import com.informatika.sars.data.model.UserRole
import com.informatika.sars.data.model.DbUser
import com.informatika.sars.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val currentAccessToken: String? get() {
        val status = SupabaseClient.client.auth.currentSessionOrNull()
        return status?.accessToken
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collectLatest { status ->
                Log.d("AuthViewModel", "Status Sesi: $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val email = status.session.user?.email ?: ""
                        if (_currentUser.value?.email != email) {
                            fetchAndSetUserProfile(email)
                        } else {
                            _isInitializing.value = false
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentUser.value = null
                        _isInitializing.value = false
                    }
                    else -> {
                        _isInitializing.value = false
                    }
                }
            }
        }
    }

    private suspend fun fetchAndSetUserProfile(email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty()) {
            _isInitializing.value = false
            return
        }
        
        try {
            Log.d("AuthViewModel", "Mencoba ambil profil: '$cleanEmail'")
            
            val response = SupabaseClient.client.postgrest.from("users").select {
                filter { 
                    ilike("email", cleanEmail)
                }
            }

            Log.d("AuthViewModel", "Raw Response Data: ${response.data}")
            
            val dbUsers = response.decodeAs<List<DbUser>>()
            val dbUser = dbUsers.firstOrNull()

            if (dbUser == null) {
                val rlsCheck = SupabaseClient.client.postgrest.from("users").select { limit(1) }
                Log.w("AuthViewModel", "Hasil Diagnosa RLS: ${rlsCheck.data}")

                if (rlsCheck.data == "[]" || rlsCheck.data == "null") {
                    _loginError.value = "Akses Database Ditolak (RLS Aktif).\nJalankan SQL: ALTER TABLE public.users DISABLE ROW LEVEL SECURITY;"
                } else {
                    _loginError.value = "Email '$cleanEmail' tidak ditemukan di database."
                }
                
                _currentUser.value = null
                _isInitializing.value = false
                return
            }

            val roleStr = dbUser.role?.uppercase() ?: "STUDENT"
            val userRole = try { UserRole.valueOf(roleStr) } catch(e: Exception) { UserRole.STUDENT }

            _currentUser.value = User(
                id = dbUser.id ?: 0L,
                name = dbUser.name ?: "User",
                email = dbUser.email ?: cleanEmail,
                nim = dbUser.nimNip ?: "-",
                role = userRole,
                avatarUrl = dbUser.avatarUrl ?: "https://ui-avatars.com/api/?name=${(dbUser.name ?: "User").replace(" ", "+")}"
            )
            
            _loginError.value = null
            _isInitializing.value = false
            Log.d("AuthViewModel", "Login Berhasil: ${_currentUser.value?.name}")
            
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error fatal saat sinkronisasi", e)
            _loginError.value = "Gagal memuat profil: ${e.localizedMessage}"
            _currentUser.value = null
            _isInitializing.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            
            try {
                try { SupabaseClient.client.auth.signOut() } catch(e: Exception) {}

                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal Login Auth", e)
                val msg = e.localizedMessage ?: ""
                _loginError.value = if (msg.contains("credentials", true)) 
                    "Email/Password salah!" else "Error: $msg"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal Logout", e)
            } finally {
                _currentUser.value = null
            }
        }
    }

    fun updateAvatar(url: String) {
        val current = _currentUser.value
        if (current != null) {
            _currentUser.value = current.copy(avatarUrl = url)
        }
    }

    fun saveFcmToken(userId: Long, fcmToken: String) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["users"].update({
                    set("fcm_token", fcmToken)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
                Log.d("AuthViewModel", "FCM token saved successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to save FCM token", e)
            }
        }
    }
}

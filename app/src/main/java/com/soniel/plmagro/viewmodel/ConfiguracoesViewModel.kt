package com.soniel.plmagro.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ConfiguracoesViewModel : ViewModel() {

    private val _desbloqueado = MutableStateFlow(false)
    val desbloqueado = _desbloqueado.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError = _isError.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private var authTime: Long = 0

    init {
        monitorExpiry()
    }

    fun validarSenha(senha: String) {
        val trimmed = senha.trim()
        val isCorrect = trimmed == "158853"
        
        Log.d("CONFIG", "Senha digitada: '$trimmed'")
        Log.d("CONFIG", "Resultado validação: $isCorrect")

        if (isCorrect) {
            _desbloqueado.value = true
            _isError.value = false
            authTime = System.currentTimeMillis()
            viewModelScope.launch { _uiMessage.emit("Configurações liberadas") }
        } else {
            _isError.value = true
            viewModelScope.launch { 
                _uiMessage.emit("Senha inválida")
                delay(2000)
                _isError.value = false
            }
        }
    }

    fun logout() {
        _desbloqueado.value = false
    }

    private fun monitorExpiry() {
        viewModelScope.launch {
            while (isActive) {
                if (_desbloqueado.value) {
                    val elapsed = (System.currentTimeMillis() - authTime) / 1000
                    if (elapsed >= 300) { // Sessão de 5 minutos
                        _desbloqueado.value = false
                        _uiMessage.emit("Sessão administrativa expirada")
                    }
                }
                delay(1000)
            }
        }
    }
}

package com.soniel.plmagro.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniel.plmagro.api.WialonUnit
import com.soniel.plmagro.model.PlmRepository
import com.soniel.plmagro.model.VinculoFrotaWialonEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LinkFleetViewModel(
    private val repository: PlmRepository,
    private val wialonRepository: com.soniel.plmagro.api.WialonRepository? = null,
    private val sessionManager: com.soniel.plmagro.api.WialonSessionManager? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _vinculoSucesso = MutableSharedFlow<Boolean>()
    val vinculoSucesso = _vinculoSucesso.asSharedFlow()

    private val _selectedUnitData = MutableStateFlow<Map<String, Any>?>(null)
    val selectedUnitData: StateFlow<Map<String, Any>?> = _selectedUnitData

    fun fetchUnitData(unitId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = wialonRepository?.getUnitData(unitId)
                _selectedUnitData.value = result?.getOrNull()
            } catch (e: Exception) {
                Log.e("LINK_WIALON", "Erro ao buscar dados da unidade", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedUnitData() {
        _selectedUnitData.value = null
    }

    fun vincularFrota(
        codigoFrota: String?,
        unit: WialonUnit?,
        placa: String,
        tipo: String,
        operador: String,
        adminDesbloqueado: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                Log.d("LINK_WIALON", "Iniciando vínculo: codigoFrota=$codigoFrota, unitId=${unit?.id}, nome=${unit?.nm}")

                // 2. Validar antes de salvar
                if (codigoFrota.isNullOrBlank()) {
                    _uiMessage.emit("Erro: Código da frota local não informado.")
                    return@launch
                }
                if (unit == null) {
                    _uiMessage.emit("Erro: Nenhuma unidade Wialon selecionada.")
                    return@launch
                }
                if (unit.nm.isBlank()) {
                    _uiMessage.emit("Erro: Unidade Wialon selecionada não possui nome.")
                    return@launch
                }

                // Só exige admin se já houver um vínculo ativo e for diferente do selecionado
                val vinculoAtual = repository.obterVinculoAtual()
                if (vinculoAtual != null && vinculoAtual.wialonUnitId != unit.id && !adminDesbloqueado) {
                    _uiMessage.emit("Alteração requer confirmação de administrador.")
                    return@launch
                }

                // Buscar dados remotos do Wialon (KM, Placa, Tipo, UniqueID)
                val remoteData = wialonRepository?.getUnitData(unit.id)?.getOrNull()
                val remoteKm = remoteData?.get("km") as? Double ?: 0.0
                val remotePlaca = remoteData?.get("placa") as? String ?: ""
                val remoteTipo = remoteData?.get("tipo") as? String ?: ""
                val remoteUid = remoteData?.get("uniqueId") as? String ?: unit.uid
                
                val vinculo = VinculoFrotaWialonEntity(
                    codigoFrotaLocal = codigoFrota,
                    placa = if (placa.isBlank()) remotePlaca else placa,
                    tipoVeiculo = if (tipo.isBlank()) remoteTipo else tipo,
                    wialonUnitId = unit.id,
                    wialonNome = unit.nm,
                    wialonUniqueId = remoteUid, // Persiste o IMEI/Unique ID para o IPS
                    operadorResponsavel = operador,
                    ativo = true,
                    ultimoKmWialon = remoteKm // Salva o KM capturado do site
                )

                if (remoteKm > 0) {
                    Log.d("LINK_WIALON", "Importando KM do Wialon: $remoteKm")
                }
                if (remotePlaca.isNotBlank()) {
                    Log.d("LINK_WIALON", "Importando Placa: $remotePlaca")
                }

                val result = repository.vincularFrota(vinculo, force = adminDesbloqueado)
                
                result.fold(
                    onSuccess = {
                        Log.d("LINK_WIALON", "Sucesso ao vincular $codigoFrota")
                        viewModelScope.launch {
                            sessionManager?.saveLinkedUid(unit.uid)
                        }
                        _uiMessage.emit("Frota vinculada com sucesso")
                        _vinculoSucesso.emit(true)
                    },
                    onFailure = { e ->
                        Log.e("LINK_WIALON", "Erro ao vincular no Repository", e)
                        _uiMessage.emit("Erro: ${e.message}")
                    }
                )

            } catch (e: Exception) {
                Log.e("LINK_WIALON", "Erro fatal ao vincular", e)
                _uiMessage.emit("Erro inesperado: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

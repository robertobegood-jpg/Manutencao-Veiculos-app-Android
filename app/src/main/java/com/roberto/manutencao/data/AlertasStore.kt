package com.roberto.manutencao.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes dos alertas e a memória do que já foi avisado,
 * para o app não repetir a mesma notificação todo dia.
 */
class AlertasStore(context: Context) {
    private val prefs = context.getSharedPreferences("alertas", Context.MODE_PRIVATE)

    private val _ativos = MutableStateFlow(prefs.getBoolean(ATIVOS, true))
    val ativos: StateFlow<Boolean> = _ativos.asStateFlow()

    /** De quantos em quantos dias lembrar de atualizar o km. 0 = nunca. */
    private val _lembreteKmDias = MutableStateFlow(prefs.getInt(LEMBRETE_KM, 15))
    val lembreteKmDias: StateFlow<Int> = _lembreteKmDias.asStateFlow()

    private val _pediuPermissao = MutableStateFlow(prefs.getBoolean(PEDIU_PERMISSAO, false))
    val pediuPermissao: StateFlow<Boolean> = _pediuPermissao.asStateFlow()

    fun definirAtivos(valor: Boolean) {
        prefs.edit().putBoolean(ATIVOS, valor).apply()
        _ativos.value = valor
    }

    fun definirLembreteKm(dias: Int) {
        prefs.edit().putInt(LEMBRETE_KM, dias).apply()
        _lembreteKmDias.value = dias
    }

    fun marcarPermissaoPedida() {
        prefs.edit().putBoolean(PEDIU_PERMISSAO, true).apply()
        _pediuPermissao.value = true
    }

    /** Itens já avisados, no formato "id:STATUS". */
    fun conjuntoAvisado(veiculoId: Long): Set<String> =
        prefs.getStringSet("avisado_$veiculoId", null)?.toSet() ?: emptySet()

    fun dataAviso(veiculoId: Long): Long = prefs.getLong("data_aviso_$veiculoId", 0L)

    fun salvarAviso(veiculoId: Long, conjunto: Set<String>, notificou: Boolean) {
        val editor = prefs.edit().putStringSet("avisado_$veiculoId", conjunto)
        if (notificou) editor.putLong("data_aviso_$veiculoId", System.currentTimeMillis())
        editor.apply()
    }

    fun dataLembreteKm(veiculoId: Long): Long = prefs.getLong("lembrete_km_$veiculoId", 0L)

    fun salvarLembreteKm(veiculoId: Long) {
        prefs.edit().putLong("lembrete_km_$veiculoId", System.currentTimeMillis()).apply()
    }

    private companion object {
        const val ATIVOS = "ativos"
        const val LEMBRETE_KM = "lembrete_km_dias"
        const val PEDIU_PERMISSAO = "pediu_permissao"
    }
}

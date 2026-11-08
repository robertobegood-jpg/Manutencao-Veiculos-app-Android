package com.roberto.manutencao.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roberto.manutencao.ManutencaoApp
import com.roberto.manutencao.arquivos.Backup
import com.roberto.manutencao.arquivos.DadosVeiculo
import com.roberto.manutencao.arquivos.ExportacaoCsv
import com.roberto.manutencao.arquivos.RelatorioPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Relatório em PDF, planilhas CSV, backup e restauração. Tudo roda fora da tela principal. */
class DadosViewModel(private val app: ManutencaoApp) : ViewModel() {

    private val db get() = app.container.database

    /** Texto do que está sendo feito agora; null = livre. */
    private val _ocupado = MutableStateFlow<String?>(null)
    val ocupado: StateFlow<String?> = _ocupado.asStateFlow()

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens.asSharedFlow()

    private fun executar(rotulo: String, erro: String, tarefa: suspend () -> Unit) {
        if (_ocupado.value != null) return
        viewModelScope.launch {
            _ocupado.value = rotulo
            try {
                tarefa()
            } catch (e: Exception) {
                _mensagens.emit(e.message?.takeIf { it.endsWith(".") } ?: erro)
            } finally {
                _ocupado.value = null
            }
        }
    }

    fun gerarPdf(veiculoId: Long, aoConcluir: (File) -> Unit) =
        executar("Gerando o relatório", "Não foi possível gerar o relatório.") {
            val arquivo = withContext(Dispatchers.IO) {
                val dados = DadosVeiculo.coletar(db, veiculoId) ?: error("Veículo não encontrado.")
                RelatorioPdf.gerar(app, dados)
            }
            aoConcluir(arquivo)
        }

    fun gerarCsv(veiculoId: Long, aoConcluir: (List<File>) -> Unit) =
        executar("Montando as planilhas", "Não foi possível exportar.") {
            val arquivos = withContext(Dispatchers.IO) {
                val dados = DadosVeiculo.coletar(db, veiculoId) ?: error("Veículo não encontrado.")
                ExportacaoCsv.gerar(app, dados)
            }
            aoConcluir(arquivos)
        }

    fun backup(destino: Uri) =
        executar("Salvando o backup", "Não foi possível salvar o backup.") {
            val r = withContext(Dispatchers.IO) { Backup.exportar(app, db, destino) }
            _mensagens.emit("Backup salvo: ${plural(r.veiculos, "veículo", "veículos")}, ${plural(r.fotos, "foto", "fotos")}.")
        }

    fun restaurar(origem: Uri) =
        executar("Restaurando", "Não foi possível restaurar este arquivo.") {
            val r = withContext(Dispatchers.IO) { Backup.importar(app, db, origem) }
            _mensagens.emit("Backup restaurado: ${plural(r.veiculos, "veículo", "veículos")}, ${plural(r.registros, "registro", "registros")}.")
        }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DadosViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManutencaoApp)
            }
        }
    }
}

package com.roberto.manutencao

import android.app.Application
import com.roberto.manutencao.alertas.Agendador
import com.roberto.manutencao.arquivos.Anexos
import com.roberto.manutencao.arquivos.Compartilhar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.roberto.manutencao.alertas.Notificacoes

class ManutencaoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notificacoes.criarCanal(this)
        // KEEP: não reinicia o horário se a verificação diária já estiver agendada.
        if (container.alertasStore.ativos.value) Agendador.agendar(this)
        limparArquivosAntigos()
    }

    /** Apaga fotos que não pertencem a nenhum registro e exportações antigas. */
    private fun limparArquivosAntigos() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                Anexos.limparOrfaos(this@ManutencaoApp, container.database.manutencaoDao().todosOsArquivos())
                val limite = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                Compartilhar.pastaExportacoes(this@ManutencaoApp).listFiles()
                    ?.filter { it.lastModified() < limite }
                    ?.forEach { it.delete() }
            }
        }
    }
}

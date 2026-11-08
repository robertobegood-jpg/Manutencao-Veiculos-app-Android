package com.roberto.manutencao

import android.content.Context
import com.roberto.manutencao.alertas.VerificadorAlertas
import com.roberto.manutencao.data.AlertasStore
import com.roberto.manutencao.data.AppDatabase
import com.roberto.manutencao.data.CompraRepository
import com.roberto.manutencao.data.GastosRepository
import com.roberto.manutencao.data.ManutencaoRepository
import com.roberto.manutencao.data.TemaStore
import com.roberto.manutencao.data.VeiculoRepository
import com.roberto.manutencao.data.VeiculoSelecionadoStore
import com.roberto.manutencao.ui.theme.FontesOficina

/** Cria e guarda os objetos compartilhados pelo app inteiro. */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.criar(context)
    val veiculoRepository = VeiculoRepository(database)
    val manutencaoRepository = ManutencaoRepository(database)
    val compraRepository = CompraRepository(database)
    val gastosRepository = GastosRepository(database)
    val veiculoSelecionadoStore = VeiculoSelecionadoStore(context)
    val temaStore = TemaStore(context)
    val alertasStore = AlertasStore(context)
    val verificadorAlertas = VerificadorAlertas(context.applicationContext, database, alertasStore)
    val fontes = FontesOficina(context)
}

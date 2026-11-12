package com.roberto.manutencao.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roberto.manutencao.DestinoNotificacao
import com.roberto.manutencao.alertas.Notificacoes
import com.roberto.manutencao.data.StatusItem
import com.roberto.manutencao.data.Tema
import com.roberto.manutencao.ui.componentes.AbaNav
import com.roberto.manutencao.ui.componentes.BarraInferior
import com.roberto.manutencao.ui.componentes.BarraSuperior
import com.roberto.manutencao.ui.telas.AbastecimentoFormScreen
import com.roberto.manutencao.ui.telas.AjustesAlertas
import com.roberto.manutencao.ui.telas.CafezinhoScreen
import com.roberto.manutencao.ui.telas.CombustivelScreen
import com.roberto.manutencao.ui.telas.DadosScreen
import com.roberto.manutencao.ui.telas.DespesaFormScreen
import com.roberto.manutencao.ui.telas.DespesasScreen
import com.roberto.manutencao.ui.telas.CompraFormScreen
import com.roberto.manutencao.ui.telas.ComprasScreen
import com.roberto.manutencao.ui.telas.ItemFormScreen
import com.roberto.manutencao.ui.telas.MaisScreen
import com.roberto.manutencao.ui.telas.ManutencaoScreen
import com.roberto.manutencao.ui.telas.PainelScreen
import com.roberto.manutencao.ui.telas.RegistroFormScreen
import com.roberto.manutencao.ui.telas.VeiculoFormScreen
import com.roberto.manutencao.ui.telas.VeiculosScreen
import com.roberto.manutencao.ui.theme.Oficina
import kotlinx.coroutines.flow.merge

object Rotas {
    const val PAINEL = "painel"
    const val MANUTENCOES = "manutencoes"
    const val COMPRAS = "compras"
    const val ABASTECIMENTOS = "abastecimentos"
    const val MAIS = "mais"
    const val VEICULOS = "veiculos"
    const val DESPESAS = "despesas"
    const val DADOS = "dados"
    const val CAFEZINHO = "cafezinho"

    const val FORM_ABASTECIMENTO = "abastecimento_form?veiculo={veiculo}&id={id}"
    fun formAbastecimento(veiculo: Long, id: Long = 0L) = "abastecimento_form?veiculo=$veiculo&id=$id"

    const val FORM_DESPESA = "despesa_form?veiculo={veiculo}&id={id}"
    fun formDespesa(veiculo: Long, id: Long = 0L) = "despesa_form?veiculo=$veiculo&id=$id"

    const val FORM_VEICULO = "veiculo_form?id={id}"
    fun formVeiculo(id: Long = 0L) = "veiculo_form?id=$id"

    const val FORM_ITEM = "item_form?veiculo={veiculo}&id={id}"
    fun formItem(veiculo: Long, id: Long = 0L) = "item_form?veiculo=$veiculo&id=$id"

    const val FORM_REGISTRO = "registro_form?veiculo={veiculo}&id={id}&item={item}&compras={compras}"
    fun formRegistro(veiculo: Long, id: Long = 0L, item: Long = 0L, deCompras: Boolean = false) =
        "registro_form?veiculo=$veiculo&id=$id&item=$item&compras=${if (deCompras) 1 else 0}"

    const val FORM_COMPRA = "compra_form?veiculo={veiculo}&id={id}&item={item}"
    fun formCompra(veiculo: Long, id: Long = 0L, item: Long = 0L) =
        "compra_form?veiculo=$veiculo&id=$id&item=$item"
}

internal val abas = listOf(
    AbaNav(Rotas.PAINEL, "Painel", Icons.Outlined.Dashboard),
    AbaNav(Rotas.MANUTENCOES, "Serviços", Icons.Outlined.Build),
    AbaNav(Rotas.COMPRAS, "Compras", Icons.Outlined.ShoppingCart),
    AbaNav(Rotas.ABASTECIMENTOS, "Posto", Icons.Outlined.LocalGasStation),
    AbaNav(Rotas.MAIS, "Mais", Icons.Outlined.MoreHoriz)
)

private fun argLong(nome: String) = navArgument(nome) {
    type = NavType.LongType
    defaultValue = 0L
}

private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot(
    tema: Tema,
    onTema: (Tema) -> Unit,
    fontesFaltando: List<String>,
    destino: DestinoNotificacao?,
    onDestinoConsumido: () -> Unit,
    veiculosVm: VeiculosViewModel = viewModel(factory = VeiculosViewModel.Factory),
    manutencaoVm: ManutencaoViewModel = viewModel(factory = ManutencaoViewModel.Factory),
    alertasVm: AlertasViewModel = viewModel(factory = AlertasViewModel.Factory),
    comprasVm: ComprasViewModel = viewModel(factory = ComprasViewModel.Factory),
    gastosVm: GastosViewModel = viewModel(factory = GastosViewModel.Factory),
    dadosVm: DadosViewModel = viewModel(factory = DadosViewModel.Factory)
) {
    val cores = Oficina.cores
    val nav = rememberNavController()
    val veiculos by veiculosVm.veiculos.collectAsStateWithLifecycle()
    val selecionado by veiculosVm.veiculoSelecionado.collectAsStateWithLifecycle()
    val situacoes by manutencaoVm.situacoes.collectAsStateWithLifecycle()
    val registros by manutencaoVm.registros.collectAsStateWithLifecycle()
    val ultimaLeitura by veiculosVm.ultimaLeitura.collectAsStateWithLifecycle()
    val compras by comprasVm.compras.collectAsStateWithLifecycle()
    val abastecimentos by gastosVm.abastecimentos.collectAsStateWithLifecycle()
    val despesas by gastosVm.despesas.collectAsStateWithLifecycle()

    val leituras by veiculosVm.leituras.collectAsStateWithLifecycle()
    val alertasAtivos by alertasVm.ativos.collectAsStateWithLifecycle()
    val lembreteKmDias by alertasVm.lembreteKmDias.collectAsStateWithLifecycle()
    val pediuPermissao by alertasVm.pediuPermissao.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(selecionado?.id) {
        manutencaoVm.definirVeiculo(selecionado?.id)
        comprasVm.definirVeiculo(selecionado?.id)
        gastosVm.definirVeiculo(selecionado?.id)
    }
    LaunchedEffect(Unit) {
        merge(veiculosVm.mensagens, manutencaoVm.mensagens, alertasVm.mensagens, comprasVm.mensagens, gastosVm.mensagens, dadosVm.mensagens)
            .collect { snackbar.showSnackbar(it) }
    }

    // Na primeira vez que houver um veículo, pede para mostrar notificações (Android 13+).
    val contexto = LocalContext.current
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(veiculos?.isNotEmpty(), alertasAtivos, pediuPermissao) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            alertasAtivos && !pediuPermissao && veiculos?.isNotEmpty() == true &&
            !Notificacoes.podeNotificar(contexto)
        ) {
            alertasVm.marcarPermissaoPedida()
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Ponto de status na aba Serviços quando há troca vencida ou próxima.
    val vencidos = situacoes.orEmpty().count { it.status == StatusItem.VENCIDO }
    val proximos = situacoes.orEmpty().count { it.status == StatusItem.PROXIMO }
    val marcas = when {
        vencidos > 0 -> mapOf(Rotas.MANUTENCOES to cores.danger)
        proximos > 0 -> mapOf(Rotas.MANUTENCOES to cores.ambar)
        else -> emptyMap()
    }

    val entrada by nav.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val ehAbaPrincipal = abas.any { it.rota == rotaAtual }

    Scaffold(
        containerColor = cores.bg,
        contentColor = cores.ink,
        topBar = {
            if (ehAbaPrincipal) {
                BarraSuperior(
                    veiculos = veiculos.orEmpty(),
                    selecionado = selecionado,
                    onSelecionar = veiculosVm::selecionar,
                    onGerenciar = { nav.navigate(Rotas.VEICULOS) }
                )
            }
        },
        bottomBar = {
            if (ehAbaPrincipal) {
                BarraInferior(abas = abas, rotaAtual = rotaAtual, onAba = { nav.irParaAba(it) }, marcas = marcas)
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { dados ->
                Snackbar(
                    snackbarData = dados,
                    shape = RectangleShape,
                    containerColor = cores.invertBg,
                    contentColor = cores.invertFg
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Rotas.PAINEL,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Rotas.PAINEL) {
                PainelScreen(
                    veiculos = veiculos,
                    selecionado = selecionado,
                    situacoes = situacoes,
                    ultimaLeitura = ultimaLeitura,
                    lembreteKmDias = lembreteKmDias,
                    registros = registros.orEmpty(),
                    abastecimentos = abastecimentos.orEmpty(),
                    despesas = despesas.orEmpty(),
                    leituras = leituras,
                    onCadastrar = { nav.navigate(Rotas.formVeiculo()) },
                    onAtualizarKm = veiculosVm::atualizarKm,
                    onVerTodos = { nav.irParaAba(Rotas.MANUTENCOES) },
                    onAbrirItem = { id -> selecionado?.let { nav.navigate(Rotas.formItem(it.id, id)) } },
                    onRegistrar = { selecionado?.let { nav.navigate(Rotas.formRegistro(it.id)) } }
                )
            }
            composable(Rotas.MANUTENCOES) {
                ManutencaoScreen(
                    selecionado = selecionado,
                    situacoes = situacoes,
                    registros = registros,
                    onCadastrarVeiculo = { nav.navigate(Rotas.formVeiculo()) },
                    onAbrirItem = { id -> selecionado?.let { nav.navigate(Rotas.formItem(it.id, id)) } },
                    onNovoItem = { selecionado?.let { nav.navigate(Rotas.formItem(it.id)) } },
                    onAdicionarSugeridos = { selecionado?.let(manutencaoVm::adicionarSugeridos) },
                    onRegistrar = { selecionado?.let { nav.navigate(Rotas.formRegistro(it.id)) } },
                    onAbrirRegistro = { id -> selecionado?.let { nav.navigate(Rotas.formRegistro(it.id, id = id)) } }
                )
            }
            composable(Rotas.COMPRAS) {
                val trocas = situacoes.orEmpty()
                    .filter { it.status == StatusItem.VENCIDO || it.status == StatusItem.PROXIMO }
                    .map { it.item }
                ComprasScreen(
                    selecionado = selecionado,
                    compras = compras,
                    trocasPendentes = trocas.size,
                    onNovaPeca = { selecionado?.let { nav.navigate(Rotas.formCompra(it.id)) } },
                    onAbrir = { id -> selecionado?.let { nav.navigate(Rotas.formCompra(it.id, id = id)) } },
                    onAlternar = comprasVm::alternarComprado,
                    onAdicionarTrocasPendentes = { selecionado?.let { comprasVm.adicionarDeItens(it.id, trocas) } },
                    onRegistrarCompras = {
                        selecionado?.let { nav.navigate(Rotas.formRegistro(it.id, deCompras = true)) }
                    },
                    onLimparComprados = { selecionado?.let { comprasVm.limparComprados(it.id) } }
                )
            }
            composable(Rotas.ABASTECIMENTOS) {
                CombustivelScreen(
                    selecionado = selecionado,
                    abastecimentos = abastecimentos,
                    onRegistrar = { selecionado?.let { nav.navigate(Rotas.formAbastecimento(it.id)) } },
                    onAbrir = { id -> selecionado?.let { nav.navigate(Rotas.formAbastecimento(it.id, id)) } },
                    onDespesas = { nav.navigate(Rotas.DESPESAS) }
                )
            }
            composable(Rotas.CAFEZINHO) {
                CafezinhoScreen(onVoltar = { nav.popBackStack() })
            }
            composable(Rotas.DADOS) {
                val ocupado by dadosVm.ocupado.collectAsStateWithLifecycle()
                DadosScreen(
                    selecionado = selecionado,
                    ocupado = ocupado,
                    onVoltar = { nav.popBackStack() },
                    onGerarPdf = dadosVm::gerarPdf,
                    onGerarCsv = dadosVm::gerarCsv,
                    onBackup = dadosVm::backup,
                    onRestaurar = dadosVm::restaurar
                )
            }
            composable(Rotas.DESPESAS) {
                DespesasScreen(
                    selecionado = selecionado,
                    despesas = despesas,
                    onVoltar = { nav.popBackStack() },
                    onNova = { selecionado?.let { nav.navigate(Rotas.formDespesa(it.id)) } },
                    onAbrir = { id -> selecionado?.let { nav.navigate(Rotas.formDespesa(it.id, id)) } }
                )
            }
            composable(Rotas.FORM_ABASTECIMENTO, arguments = listOf(argLong("veiculo"), argLong("id"))) { e ->
                AbastecimentoFormScreen(
                    veiculoId = e.arguments?.getLong("veiculo") ?: 0L,
                    abastecimentoId = e.arguments?.getLong("id") ?: 0L,
                    gastosViewModel = gastosVm,
                    veiculosViewModel = veiculosVm,
                    onVoltar = { nav.popBackStack() }
                )
            }
            composable(Rotas.FORM_DESPESA, arguments = listOf(argLong("veiculo"), argLong("id"))) { e ->
                DespesaFormScreen(
                    veiculoId = e.arguments?.getLong("veiculo") ?: 0L,
                    despesaId = e.arguments?.getLong("id") ?: 0L,
                    gastosViewModel = gastosVm,
                    onVoltar = { nav.popBackStack() }
                )
            }
            composable(Rotas.MAIS) {
                MaisScreen(
                    onVeiculos = { nav.navigate(Rotas.VEICULOS) },
                    onDespesas = { nav.navigate(Rotas.DESPESAS) },
                    onDados = { nav.navigate(Rotas.DADOS) },
                    onCafezinho = { nav.navigate(Rotas.CAFEZINHO) },
                    tema = tema,
                    onTema = onTema,
                    alertas = AjustesAlertas(
                        ativos = alertasAtivos,
                        lembreteKmDias = lembreteKmDias,
                        pediuPermissao = pediuPermissao,
                        onAtivos = alertasVm::definirAtivos,
                        onLembreteKm = alertasVm::definirLembreteKm,
                        onPermissaoPedida = alertasVm::marcarPermissaoPedida,
                        onVerificarAgora = alertasVm::verificarAgora
                    ),
                    fontesFaltando = fontesFaltando
                )
            }
            composable(Rotas.VEICULOS) {
                VeiculosScreen(
                    veiculos = veiculos.orEmpty(),
                    selecionadoId = selecionado?.id,
                    onVoltar = { nav.popBackStack() },
                    onNovo = { nav.navigate(Rotas.formVeiculo()) },
                    onEditar = { nav.navigate(Rotas.formVeiculo(it.id)) },
                    onExcluir = veiculosVm::excluir,
                    onSelecionar = veiculosVm::selecionar
                )
            }
            composable(Rotas.FORM_VEICULO, arguments = listOf(argLong("id"))) { e ->
                VeiculoFormScreen(
                    veiculoId = e.arguments?.getLong("id") ?: 0L,
                    viewModel = veiculosVm,
                    onVoltar = { nav.popBackStack() }
                )
            }
            composable(Rotas.FORM_ITEM, arguments = listOf(argLong("veiculo"), argLong("id"))) { e ->
                val veiculoId = e.arguments?.getLong("veiculo") ?: 0L
                ItemFormScreen(
                    veiculoId = veiculoId,
                    itemId = e.arguments?.getLong("id") ?: 0L,
                    viewModel = manutencaoVm,
                    situacoes = situacoes,
                    onVoltar = { nav.popBackStack() },
                    onAdicionarCompra = { itemId ->
                        nav.navigate(Rotas.formCompra(veiculoId, item = itemId)) {
                            popUpTo(Rotas.FORM_ITEM) { inclusive = true }
                        }
                    },
                    onRegistrarTroca = { itemId ->
                        nav.navigate(Rotas.formRegistro(veiculoId, item = itemId)) {
                            popUpTo(Rotas.FORM_ITEM) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                Rotas.FORM_REGISTRO,
                arguments = listOf(argLong("veiculo"), argLong("id"), argLong("item"), argLong("compras"))
            ) { e ->
                RegistroFormScreen(
                    deCompras = (e.arguments?.getLong("compras") ?: 0L) == 1L,
                    comprasViewModel = comprasVm,
                    veiculoId = e.arguments?.getLong("veiculo") ?: 0L,
                    registroId = e.arguments?.getLong("id") ?: 0L,
                    itemInicialId = e.arguments?.getLong("item") ?: 0L,
                    viewModel = manutencaoVm,
                    veiculosViewModel = veiculosVm,
                    onVoltar = { nav.popBackStack() }
                )
            }
            composable(
                Rotas.FORM_COMPRA,
                arguments = listOf(argLong("veiculo"), argLong("id"), argLong("item"))
            ) { e ->
                CompraFormScreen(
                    veiculoId = e.arguments?.getLong("veiculo") ?: 0L,
                    compraId = e.arguments?.getLong("id") ?: 0L,
                    itemInicialId = e.arguments?.getLong("item") ?: 0L,
                    comprasViewModel = comprasVm,
                    manutencaoViewModel = manutencaoVm,
                    itensDoVeiculo = situacoes.orEmpty().map { it.item }.sortedBy { it.ordem },
                    onVoltar = { nav.popBackStack() }
                )
            }
        }
    }

    // Aberto por uma notificação: seleciona o veículo e vai para a aba certa.
    LaunchedEffect(destino) {
        val alvo = destino ?: return@LaunchedEffect
        if (alvo.veiculoId != 0L) veiculosVm.selecionar(alvo.veiculoId)
        nav.irParaAba(if (alvo.destino == Notificacoes.DESTINO_SERVICOS) Rotas.MANUTENCOES else Rotas.PAINEL)
        onDestinoConsumido()
    }
}

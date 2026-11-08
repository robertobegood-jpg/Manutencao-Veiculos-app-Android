package com.roberto.manutencao.data

data class ModeloItem(
    val nome: String,
    val intervaloKm: Int?,
    val intervaloMeses: Int?
)

/**
 * Itens sugeridos com intervalos comuns. São só um ponto de partida:
 * cada um pode ser editado conforme o manual do veículo.
 */
object ModelosManutencao {

    val carro = listOf(
        ModeloItem("Óleo do motor", 10_000, 12),
        ModeloItem("Filtro de óleo", 10_000, 12),
        ModeloItem("Filtro de ar", 20_000, 24),
        ModeloItem("Filtro de combustível", 20_000, 24),
        ModeloItem("Filtro de cabine", 15_000, 12),
        ModeloItem("Correia dentada", 50_000, 48),
        ModeloItem("Velas de ignição", 30_000, 36),
        ModeloItem("Pastilhas de freio", 30_000, null),
        ModeloItem("Discos de freio", 60_000, null),
        ModeloItem("Fluido de freio", null, 24),
        ModeloItem("Líquido de arrefecimento", 40_000, 24),
        ModeloItem("Alinhamento e balanceamento", 10_000, 12),
        ModeloItem("Pneus", 50_000, 60),
        ModeloItem("Bateria", null, 36)
    )

    val moto = listOf(
        ModeloItem("Óleo do motor", 3_000, 6),
        ModeloItem("Filtro de óleo", 6_000, 12),
        ModeloItem("Filtro de ar", 12_000, 12),
        ModeloItem("Lubrificação da corrente", 500, 1),
        ModeloItem("Kit relação", 20_000, 24),
        ModeloItem("Vela de ignição", 10_000, 12),
        ModeloItem("Pastilhas de freio", 10_000, null),
        ModeloItem("Fluido de freio", null, 24),
        ModeloItem("Cabos e regulagens", 6_000, 12),
        ModeloItem("Pneus", 15_000, 36),
        ModeloItem("Bateria", null, 24)
    )

    fun para(tipo: TipoVeiculo): List<ModeloItem> = when (tipo) {
        TipoVeiculo.CARRO -> carro
        TipoVeiculo.MOTO -> moto
    }

    fun criarItens(veiculoId: Long, tipo: TipoVeiculo, ordemInicial: Int = 1): List<ItemManutencao> =
        para(tipo).mapIndexed { i, modelo ->
            ItemManutencao(
                veiculoId = veiculoId,
                nome = modelo.nome,
                intervaloKm = modelo.intervaloKm,
                intervaloMeses = modelo.intervaloMeses,
                ordem = ordemInicial + i
            )
        }
}

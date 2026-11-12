package com.roberto.manutencao

import com.roberto.manutencao.data.Abastecimento
import com.roberto.manutencao.data.AbastecimentoComConsumo
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.CalculoVencimento
import com.roberto.manutencao.data.CategoriaDespesa
import com.roberto.manutencao.data.Combustivel
import com.roberto.manutencao.data.Datas
import com.roberto.manutencao.data.Despesa
import com.roberto.manutencao.data.ItemComUltimaTroca
import com.roberto.manutencao.data.ItemCompra
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.LeituraKm
import com.roberto.manutencao.data.ModelosManutencao
import com.roberto.manutencao.data.RegistroComItens
import com.roberto.manutencao.data.RegistroManutencao
import com.roberto.manutencao.data.SituacaoItem
import com.roberto.manutencao.data.TipoVeiculo
import com.roberto.manutencao.data.Veiculo
import java.time.LocalDate
import java.time.ZoneId

/**
 * Dados de teste "pesados": nomes longos, valores altos, km no limite
 * e históricos de vários anos, sempre relativos à data de hoje.
 */
object DadosDeTeste {
    val hoje: LocalDate = LocalDate.now()

    /** Data no formato das datas de registro (meia-noite UTC do dia). */
    fun dia(diasAtras: Long): Long = Datas.deLocalDate(hoje.minusDays(diasAtras))

    /** Instante comum (como System.currentTimeMillis), para leituras de km. */
    fun instante(diasAtras: Long): Long =
        hoje.minusDays(diasAtras).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val carro = Veiculo(
        id = 1, tipo = TipoVeiculo.CARRO, apelido = "Meu March",
        marca = "Nissan", modelo = "March 1.0 S 12v 3 Cilindros Flex", ano = 2015,
        placa = "ABC1D23", kmAtual = 201_000, criadoEm = instante(700)
    )

    val motoLonga = Veiculo(
        id = 2, tipo = TipoVeiculo.MOTO, apelido = "Moto do trabalho do meu irmão caçula",
        marca = "Honda", modelo = "CG 160 Titan EX Special Edition Flexone", ano = 2024,
        placa = "XYZ9K87", kmAtual = 999_999, criadoEm = instante(30)
    )

    val veiculos = listOf(carro, motoLonga)

    /** Itens do carro em todas as situações possíveis. */
    fun situacoesCarro(): List<SituacaoItem> {
        val itens = ModelosManutencao.criarItens(carro.id, carro.tipo).mapIndexed { i, it -> it.copy(id = i + 1L) }
        fun item(nome: String) = itens.first { it.nome == nome }
        val dados = listOf(
            // vencido por km e por tempo
            ItemComUltimaTroca(item("Óleo do motor").copy(modelo = "Mobil Super 3000 5W30 sintético 1L", referencia = "MOB-5W30-SN-PLUS-1L"), 185_000, dia(420)),
            // próximo (faltam 500 km de 10.000)
            ItemComUltimaTroca(item("Filtro de óleo").copy(modelo = "Solocar SL312008", referencia = "SL312008"), 191_500, dia(60)),
            // em dia, só por tempo
            ItemComUltimaTroca(item("Bateria").copy(modelo = "Moura M60GD 60Ah"), null, dia(30)),
            // em dia por km com nome longo
            ItemComUltimaTroca(item("Líquido de arrefecimento").copy(modelo = "Paraflu Rad Flu Concentrado Orgânico Rosa"), 200_000, dia(10)),
            // pausado
            ItemComUltimaTroca(item("Discos de freio").copy(ativo = false), 150_000, null),
            // sem histórico
            ItemComUltimaTroca(item("Correia dentada"), null, null),
            ItemComUltimaTroca(item("Velas de ignição"), null, null)
        )
        return CalculoVencimento.ordenar(dados.map { CalculoVencimento.calcular(it, carro.kmAtual, hoje) })
    }

    private fun registro(id: Long, diasAtras: Long, km: Int, pecas: Long, mao: Long, local: String, vararg itens: String) =
        RegistroComItens(
            registro = RegistroManutencao(
                id = id, veiculoId = carro.id, data = dia(diasAtras), km = km, local = local,
                valorPecasCentavos = pecas, valorMaoDeObraCentavos = mao,
                observacoes = "Revisão completa com troca de fluidos e checagem geral."
            ),
            itens = itens.mapIndexed { i, nome -> ItemManutencao(id = 100L + i, veiculoId = carro.id, nome = nome) }
        )

    /** Histórico de [anos] anos, com um registro a cada ~2 meses e valores altos. */
    fun registros(anos: Int): List<RegistroComItens> {
        val total = anos * 6
        return (0 until total).map { k ->
            val diasAtras = (total - k) * 60L
            registro(
                id = k + 1L, diasAtras = diasAtras, km = 201_000 - (total - k) * 2_000,
                pecas = if (k % 5 == 0) 1_234_567 else 45_990, mao = if (k % 3 == 0) 35_000 else 0,
                local = "Auto Center Mecânica Irmãos Rodrigues Ltda",
                "Óleo do motor", "Filtro de óleo", "Filtro de ar", "Pastilhas de freio", "Líquido de arrefecimento"
            )
        }.reversed()
    }

    fun abastecimentos(meses: Int): List<AbastecimentoComConsumo> {
        val lista = (0 until meses * 2).map { k ->
            val n = meses * 2 - k
            Abastecimento(
                id = k + 1L, veiculoId = carro.id, data = dia(n * 15L), km = 201_000 - n * 450,
                litros = 38.57, precoLitroMilesimos = 6_299, totalCentavos = 24_295,
                tanqueCheio = k % 4 != 1, combustivel = Combustivel.GASOLINA,
                posto = "Posto Ipiranga Avenida Marechal Tito"
            )
        }
        return CalculoConsumo.calcular(lista)
    }

    fun despesas(): List<Despesa> = listOf(
        Despesa(1, carro.id, dia(300), CategoriaDespesa.IPVA, "IPVA 2026 cota única com desconto", 1_987_654),
        Despesa(2, carro.id, dia(200), CategoriaDespesa.SEGURO, "Seguro anual Porto Seguro", 2_450_000),
        Despesa(3, carro.id, dia(40), CategoriaDespesa.MULTA, "Excesso de velocidade", 19_523),
        Despesa(4, carro.id, dia(5), CategoriaDespesa.PEDAGIO, "", 1_840)
    )

    fun leituras(): List<LeituraKm> = listOf(
        LeituraKm(1, carro.id, 175_000, instante(700)),
        LeituraKm(2, carro.id, 201_000, instante(1))
    )

    fun compras(): List<ItemCompra> = listOf(
        ItemCompra(1, carro.id, 1, "Óleo do motor", "Mobil Super 3000 5W30 sintético 1L", "MOB-5W30-SN-PLUS-1L", 4.0, "L", 4_590, "Mercado Livre"),
        ItemCompra(2, carro.id, null, "Kit Embreagem com Atuador Hidráulico Completo", "Elper 90402 / LuK 620 3057 00", "90402", 1.0, "kit", 146_990, "Mercado Car Peças e Acessórios Automotivos"),
        ItemCompra(3, carro.id, null, "Palheta", "", "", 2.0, "un", 0, ""),
        ItemCompra(4, carro.id, 2, "Pastilhas de freio", "Pastilhas freio dianteiro Cobreq N-1137", "N1137", 1.0, "jogo", 7_888, "Mercado Livre", comprado = true, compradoEm = instante(3), registroId = 7),
        ItemCompra(5, carro.id, 2, "Filtro de óleo", "Solocar SL312008", "SL312008-XYZ-123456", 1.0, "un", 2_290, "", comprado = true, compradoEm = instante(2))
    )
}

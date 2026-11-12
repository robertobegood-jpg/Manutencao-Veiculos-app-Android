package com.roberto.manutencao

import com.roberto.manutencao.DadosDeTeste.carro
import com.roberto.manutencao.DadosDeTeste.dia
import com.roberto.manutencao.DadosDeTeste.hoje
import com.roberto.manutencao.data.Abastecimento
import com.roberto.manutencao.data.CalculoConsumo
import com.roberto.manutencao.data.CalculoVencimento
import com.roberto.manutencao.data.Escala
import com.roberto.manutencao.data.Estatisticas
import com.roberto.manutencao.data.ItemComUltimaTroca
import com.roberto.manutencao.data.ItemManutencao
import com.roberto.manutencao.data.Periodo
import com.roberto.manutencao.data.Pix
import com.roberto.manutencao.data.StatusItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculosTest {

    private fun item(km: Int? = 10_000, meses: Int? = 12, ativo: Boolean = true, baseKm: Int? = null, baseData: Long? = null) =
        ItemManutencao(id = 1, veiculoId = 1, nome = "Óleo", intervaloKm = km, intervaloMeses = meses, baseKm = baseKm, baseData = baseData, ativo = ativo)

    /* ───── Vencimentos ───── */

    @Test fun vencidoPorKm() {
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(), 100_000, dia(10)), 110_500, hoje)
        assertEquals(StatusItem.VENCIDO, s.status)
        assertEquals(-500, s.kmRestante)
    }

    @Test fun vencidoPorTempoMesmoComKmSobrando() {
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(), 100_000, dia(400)), 101_000, hoje)
        assertEquals(StatusItem.VENCIDO, s.status)
    }

    @Test fun proximoQuandoFaltaMenosDe10PorCento() {
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(meses = null), 100_000, null), 109_200, hoje)
        assertEquals(StatusItem.PROXIMO, s.status)
    }

    @Test fun emDia() {
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(), 100_000, dia(30)), 102_000, hoje)
        assertEquals(StatusItem.EM_DIA, s.status)
    }

    @Test fun semHistoricoEPausado() {
        assertEquals(StatusItem.SEM_HISTORICO, CalculoVencimento.calcular(ItemComUltimaTroca(item(), null, null), 5_000, hoje).status)
        assertEquals(StatusItem.INATIVO, CalculoVencimento.calcular(ItemComUltimaTroca(item(ativo = false), 1_000, dia(9_000)), 999_999, hoje).status)
    }

    @Test fun usaATrocaMaisRecenteEntreRegistroEBaseManual() {
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(baseKm = 108_000, baseData = dia(5)), 100_000, dia(300)), 109_000, hoje)
        assertEquals(118_000, s.proximoKm)
        assertEquals(StatusItem.EM_DIA, s.status)
    }

    @Test fun kmAtualMenorQueATrocaNaoQuebra() {
        // Troca registrada com km maior que o hodômetro (erro de digitação): não pode dar progresso negativo estranho.
        val s = CalculoVencimento.calcular(ItemComUltimaTroca(item(meses = null), 120_000, null), 100_000, hoje)
        assertEquals(StatusItem.EM_DIA, s.status)
        assertTrue(s.progresso <= 0f)
    }

    @Test fun ordenaMaisUrgentesPrimeiro() {
        val lista = DadosDeTeste.situacoesCarro()
        assertEquals(StatusItem.VENCIDO, lista.first().status)
        assertEquals(StatusItem.INATIVO, lista.last().status)
    }

    /* ───── Consumo ───── */

    private fun abast(id: Long, km: Int, litros: Double, cheio: Boolean = true) =
        Abastecimento(id = id, veiculoId = 1, data = dia(100 - id), km = km, litros = litros,
            precoLitroMilesimos = 6_000, totalCentavos = (litros * 600).toLong(), tanqueCheio = cheio)

    @Test fun consumoEntreDoisTanquesCheios() {
        val r = CalculoConsumo.calcular(listOf(abast(1, 10_000, 40.0), abast(2, 10_500, 40.0)))
        assertEquals(12.5, r.first().consumoKmL!!, 0.001)
        assertNull(r.last().consumoKmL)
    }

    @Test fun parcialSomaNoTrechoSeguinte() {
        val r = CalculoConsumo.calcular(listOf(abast(1, 10_000, 40.0), abast(2, 10_200, 10.0, cheio = false), abast(3, 10_600, 30.0)))
        assertEquals(15.0, r.first().consumoKmL!!, 0.001)
        assertEquals(15.0, CalculoConsumo.media(r)!!, 0.001)
    }

    @Test fun semTanqueCheioNaoHaMedia() {
        assertNull(CalculoConsumo.media(CalculoConsumo.calcular(listOf(abast(1, 10_000, 40.0, false)))))
        assertNull(CalculoConsumo.media(CalculoConsumo.calcular(emptyList())))
    }

    /* ───── Gráfico de gastos ───── */

    private fun resumo(periodo: Periodo, anos: Int) = Estatisticas.resumir(
        periodo, hoje, DadosDeTeste.registros(anos), DadosDeTeste.abastecimentos(anos * 12),
        DadosDeTeste.despesas(), DadosDeTeste.leituras(), carro.kmAtual
    )

    @Test fun nuncaPassaDe12Barras() {
        for (periodo in Periodo.entries) for (anos in listOf(0, 1, 2, 5, 15)) {
            val r = Estatisticas.resumir(periodo, hoje, DadosDeTeste.registros(anos), emptyList(), emptyList(), emptyList(), carro.kmAtual)
            assertTrue("$periodo/$anos anos: ${r.barras.size} barras", r.barras.size in 1..12)
        }
    }

    @Test fun escalaPorTamanhoDoHistorico() {
        assertEquals(Escala.MES, resumo(Periodo.ANO, 5).escala)
        assertEquals(Escala.TRIMESTRE, resumo(Periodo.TUDO, 2).escala)
        assertEquals(Escala.ANO, resumo(Periodo.TUDO, 5).escala)
    }

    @Test fun barrasSomamOTotalNoTudo() {
        for (anos in listOf(1, 2, 5)) {
            val r = resumo(Periodo.TUDO, anos)
            assertEquals("$anos anos", r.gastoTotal, r.barras.sumOf { it.total })
        }
    }

    @Test fun ultimaBarraEhOPeriodoAtual() {
        val r = resumo(Periodo.ANO, 2)
        assertTrue(r.barras.last().titulo.endsWith(hoje.year.toString()))
    }

    @Test fun semDadosNaoQuebra() {
        val r = Estatisticas.resumir(Periodo.TUDO, hoje, emptyList(), emptyList(), emptyList(), emptyList(), 0)
        assertEquals(0L, r.gastoTotal)
        assertNull(r.custoPorKm)
        assertTrue(r.barras.isNotEmpty())
    }

    /* ───── Pix ───── */

    @Test fun pixBateComOExemploDoBancoCentral() {
        // Exemplo do Manual de Padrões para Iniciação do Pix (BR Code estático).
        val esperado = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
            "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D"
        assertEquals(esperado, Pix.payload("123e4567-e12b-12d1-a456-426655440000", "Fulano de Tal", "BRASILIA"))
    }

    @Test fun pixTiraAcentosELimitaTamanho() {
        val p = Pix.payload("a@b.com", "José da Conceição Albuquerque Neto", "São José dos Campos", 500)
        assertTrue(p, p.contains("5925Jose da Conceicao Albuque"))
        assertTrue(p, p.contains("6015Sao Jose dos Ca"))
        assertTrue(p, p.contains("54045.00"))
    }
}

package com.roberto.manutencao.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Datas de manutenção são guardadas como "meia-noite UTC do dia escolhido",
 * o mesmo formato que o calendário do Android devolve.
 */
object Datas {
    private val zona = ZoneOffset.UTC
    private val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun hoje(): Long = deLocalDate(LocalDate.now())

    fun paraLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun deLocalDate(data: LocalDate): Long =
        data.atStartOfDay(zona).toInstant().toEpochMilli()

    /** Converte um instante qualquer (ex.: System.currentTimeMillis) para o dia no formato acima. */
    fun doInstante(millis: Long): Long =
        deLocalDate(Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate())

    fun formatar(millis: Long): String = paraLocalDate(millis).format(formato)

    fun formatar(data: LocalDate): String = data.format(formato)
}

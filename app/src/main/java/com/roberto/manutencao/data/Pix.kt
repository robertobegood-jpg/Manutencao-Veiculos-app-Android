package com.roberto.manutencao.data

import java.text.Normalizer
import java.util.Locale

/**
 * Monta o "Pix copia e cola" (BR Code estático) no padrão do Banco Central:
 * campos EMV no formato ID + tamanho + valor, terminando com o CRC16.
 */
object Pix {

    fun payload(chave: String, nome: String, cidade: String, valorCentavos: Long? = null, mensagem: String = ""): String {
        val conta = campo("00", "br.gov.bcb.pix") +
            campo("01", chave.trim()) +
            (if (mensagem.isNotBlank()) campo("02", limpar(mensagem, 40)) else "")

        val semCrc = buildString {
            append(campo("00", "01"))
            append(campo("26", conta))
            append(campo("52", "0000"))
            append(campo("53", "986"))
            if (valorCentavos != null && valorCentavos > 0) {
                append(campo("54", String.format(Locale.US, "%.2f", valorCentavos / 100.0)))
            }
            append(campo("58", "BR"))
            append(campo("59", limpar(nome, 25)))
            append(campo("60", limpar(cidade, 15)))
            append(campo("62", campo("05", "***")))
            append("6304")
        }
        return semCrc + crc16(semCrc)
    }

    private fun campo(id: String, valor: String): String =
        id + valor.length.toString().padStart(2, '0') + valor

    /** Sem acentos e no tamanho máximo do campo. */
    private fun limpar(texto: String, maximo: Int): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^A-Za-z0-9 .,@+\\-]"), "")
            .take(maximo)

    /** CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF), em 4 dígitos hexadecimais. */
    private fun crc16(texto: String): String {
        var crc = 0xFFFF
        texto.toByteArray(Charsets.UTF_8).forEach { b ->
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase(Locale.ROOT).padStart(4, '0')
    }
}

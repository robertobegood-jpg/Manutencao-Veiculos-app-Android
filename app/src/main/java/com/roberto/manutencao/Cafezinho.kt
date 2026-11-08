package com.roberto.manutencao

/**
 * Dados do Pix que recebe os cafezinhos.
 * Para trocar, edite só estes três valores. Com a chave vazia, a opção fica escondida no app.
 */
object Cafezinho {
    /** E-mail, celular (+5511999999999), CPF/CNPJ (só números) ou chave aleatória. */
    const val CHAVE_PIX = "robertobegood+itau@gmail.com"

    /** Nome de quem recebe, como aparece no app do banco (até 25 letras). */
    const val NOME = "Roberto Santos da Silva"

    /** Cidade de quem recebe (até 15 letras). */
    const val CIDADE = "São Paulo"

    val configurado: Boolean get() = CHAVE_PIX.isNotBlank() && NOME.isNotBlank() && CIDADE.isNotBlank()
}

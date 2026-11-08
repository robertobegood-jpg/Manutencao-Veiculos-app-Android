package com.roberto.manutencao.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class TipoVeiculo(val rotulo: String) {
    CARRO("Carro"),
    MOTO("Moto")
}

@Entity(tableName = "veiculos")
data class Veiculo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoVeiculo,
    val apelido: String,
    val marca: String = "",
    val modelo: String = "",
    val ano: Int? = null,
    val placa: String = "",
    val kmAtual: Int = 0,
    val fotoUri: String? = null,
    val criadoEm: Long = System.currentTimeMillis()
)

/** Histórico de leituras do hodômetro, usado depois para estimar km rodados por dia. */
@Entity(
    tableName = "leituras_km",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("veiculoId")]
)
data class LeituraKm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    val km: Int,
    val data: Long = System.currentTimeMillis()
)

/** Algo que se troca de tempos em tempos: óleo, correia, pastilhas... */
@Entity(
    tableName = "itens_manutencao",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("veiculoId")]
)
data class ItemManutencao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    val nome: String,
    /** Trocar a cada X km (null = não controla por km). */
    val intervaloKm: Int? = null,
    /** Trocar a cada X meses (null = não controla por tempo). */
    val intervaloMeses: Int? = null,
    /** Última troca informada à mão, feita antes de usar o app. */
    val baseKm: Int? = null,
    val baseData: Long? = null,
    val ativo: Boolean = true,
    val ordem: Int = 0,
    /** Modelo ou especificação da peça, ex.: "5W30 sintético", "Fram PH6017". */
    val modelo: String = "",
    /** Código de referência da peça, para pedir na loja. */
    val referencia: String = ""
)

/** Uma ida à oficina (ou um serviço feito em casa). */
@Entity(
    tableName = "registros_manutencao",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("veiculoId")]
)
data class RegistroManutencao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    val data: Long,
    val km: Int,
    val local: String = "",
    val feitoPorMim: Boolean = false,
    val valorPecasCentavos: Long = 0,
    val valorMaoDeObraCentavos: Long = 0,
    val observacoes: String = ""
)

/** Quais itens foram trocados em cada registro. */
@Entity(
    tableName = "registro_itens",
    primaryKeys = ["registroId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = RegistroManutencao::class,
            parentColumns = ["id"],
            childColumns = ["registroId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemManutencao::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("registroId"), Index("itemId")]
)
data class RegistroItem(
    val registroId: Long,
    val itemId: Long
)

/** Item com a troca mais recente encontrada nos registros. */
data class ItemComUltimaTroca(
    @Embedded val item: ItemManutencao,
    val registroKm: Int?,
    val registroData: Long?
)

/** Foto (nota fiscal, recibo) ligada a um registro. O arquivo fica em files/anexos/. */
@Entity(
    tableName = "anexos",
    foreignKeys = [
        ForeignKey(
            entity = RegistroManutencao::class,
            parentColumns = ["id"],
            childColumns = ["registroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("registroId")]
)
data class Anexo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val registroId: Long,
    /** Só o nome do arquivo, dentro da pasta de anexos. */
    val arquivo: String,
    val criadoEm: Long = System.currentTimeMillis()
)

data class RegistroComItens(
    @Embedded val registro: RegistroManutencao,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RegistroItem::class,
            parentColumn = "registroId",
            entityColumn = "itemId"
        )
    )
    val itens: List<ItemManutencao>,
    @Relation(parentColumn = "id", entityColumn = "registroId")
    val anexos: List<Anexo> = emptyList()
)

/** Uma peça na lista de compras de um veículo. */
@Entity(
    tableName = "itens_compra",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemManutencao::class,
            parentColumns = ["id"],
            childColumns = ["itemManutencaoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("veiculoId"), Index("itemManutencaoId")]
)
data class ItemCompra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    /** Item de manutenção a que a peça se refere (opcional). */
    val itemManutencaoId: Long? = null,
    val nome: String,
    val modelo: String = "",
    val referencia: String = "",
    val quantidade: Double = 1.0,
    val unidade: String = "un",
    val precoUnitarioCentavos: Long = 0,
    val loja: String = "",
    val observacao: String = "",
    val comprado: Boolean = false,
    val compradoEm: Long? = null,
    /** Registro de manutenção gerado a partir desta compra, se houver. */
    val registroId: Long? = null,
    val criadoEm: Long = System.currentTimeMillis()
)

enum class Combustivel(val rotulo: String) {
    GASOLINA("Gasolina"),
    ETANOL("Etanol"),
    DIESEL("Diesel"),
    GNV("GNV")
}

/** Um abastecimento. O consumo é calculado entre dois tanques cheios. */
@Entity(
    tableName = "abastecimentos",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("veiculoId")]
)
data class Abastecimento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    val data: Long,
    val km: Int,
    val litros: Double,
    /** Preço por litro em milésimos de real (R$ 5,899 = 5899). */
    val precoLitroMilesimos: Long,
    val totalCentavos: Long,
    val tanqueCheio: Boolean = true,
    val combustivel: Combustivel = Combustivel.GASOLINA,
    val posto: String = "",
    val observacao: String = ""
)

enum class CategoriaDespesa(val rotulo: String) {
    IPVA("IPVA"),
    LICENCIAMENTO("Licenciamento"),
    SEGURO("Seguro"),
    MULTA("Multa"),
    PEDAGIO("Pedágio"),
    ESTACIONAMENTO("Estacionamento"),
    LAVAGEM("Lavagem"),
    OUTRO("Outro")
}

/** IPVA, seguro, multas e outros gastos que não são manutenção nem combustível. */
@Entity(
    tableName = "despesas",
    foreignKeys = [
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("veiculoId")]
)
data class Despesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val veiculoId: Long,
    val data: Long,
    val categoria: CategoriaDespesa,
    val descricao: String = "",
    val valorCentavos: Long,
    val observacao: String = ""
)

fun ItemCompra.subtotalCentavos(): Long = Math.round(quantidade * precoUnitarioCentavos)

fun RegistroComItens.totalCentavos(): Long =
    registro.valorPecasCentavos + registro.valorMaoDeObraCentavos

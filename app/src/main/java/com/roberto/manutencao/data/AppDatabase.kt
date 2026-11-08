package com.roberto.manutencao.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Veiculo::class,
        LeituraKm::class,
        ItemManutencao::class,
        RegistroManutencao::class,
        RegistroItem::class,
        ItemCompra::class,
        Abastecimento::class,
        Despesa::class,
        Anexo::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun veiculoDao(): VeiculoDao
    abstract fun leituraKmDao(): LeituraKmDao
    abstract fun manutencaoDao(): ManutencaoDao
    abstract fun compraDao(): CompraDao
    abstract fun gastosDao(): GastosDao
    abstract fun backupDao(): BackupDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "manutencao.db"
            )
                .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3, MIGRACAO_3_4, MIGRACAO_4_5)
                .build()

        /** Etapa 7: fotos de notas fiscais nos registros. */
        val MIGRACAO_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `anexos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`registroId` INTEGER NOT NULL, `arquivo` TEXT NOT NULL, `criadoEm` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`registroId`) REFERENCES `registros_manutencao`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anexos_registroId` ON `anexos` (`registroId`)")
            }
        }

        /** Etapa 5: abastecimentos e despesas avulsas. */
        val MIGRACAO_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `abastecimentos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`veiculoId` INTEGER NOT NULL, `data` INTEGER NOT NULL, `km` INTEGER NOT NULL, " +
                        "`litros` REAL NOT NULL, `precoLitroMilesimos` INTEGER NOT NULL, `totalCentavos` INTEGER NOT NULL, " +
                        "`tanqueCheio` INTEGER NOT NULL, `combustivel` TEXT NOT NULL, " +
                        "`posto` TEXT NOT NULL, `observacao` TEXT NOT NULL, " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_abastecimentos_veiculoId` ON `abastecimentos` (`veiculoId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `despesas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`veiculoId` INTEGER NOT NULL, `data` INTEGER NOT NULL, `categoria` TEXT NOT NULL, " +
                        "`descricao` TEXT NOT NULL, `valorCentavos` INTEGER NOT NULL, `observacao` TEXT NOT NULL, " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_despesas_veiculoId` ON `despesas` (`veiculoId`)")
            }
        }

        /** Etapa 4: modelo e referência nos itens, e a tabela da lista de compras. */
        val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `itens_manutencao` ADD COLUMN `modelo` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `itens_manutencao` ADD COLUMN `referencia` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `itens_compra` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`veiculoId` INTEGER NOT NULL, `itemManutencaoId` INTEGER, " +
                        "`nome` TEXT NOT NULL, `modelo` TEXT NOT NULL, `referencia` TEXT NOT NULL, " +
                        "`quantidade` REAL NOT NULL, `unidade` TEXT NOT NULL, " +
                        "`precoUnitarioCentavos` INTEGER NOT NULL, `loja` TEXT NOT NULL, `observacao` TEXT NOT NULL, " +
                        "`comprado` INTEGER NOT NULL, `compradoEm` INTEGER, `registroId` INTEGER, " +
                        "`criadoEm` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`itemManutencaoId`) REFERENCES `itens_manutencao`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_itens_compra_veiculoId` ON `itens_compra` (`veiculoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_itens_compra_itemManutencaoId` ON `itens_compra` (`itemManutencaoId`)")
            }
        }

        /** Etapa 2: cria as tabelas de manutenção e já coloca os itens sugeridos nos veículos existentes. */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `itens_manutencao` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`veiculoId` INTEGER NOT NULL, `nome` TEXT NOT NULL, " +
                        "`intervaloKm` INTEGER, `intervaloMeses` INTEGER, " +
                        "`baseKm` INTEGER, `baseData` INTEGER, " +
                        "`ativo` INTEGER NOT NULL, `ordem` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_itens_manutencao_veiculoId` ON `itens_manutencao` (`veiculoId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `registros_manutencao` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`veiculoId` INTEGER NOT NULL, `data` INTEGER NOT NULL, `km` INTEGER NOT NULL, " +
                        "`local` TEXT NOT NULL, `feitoPorMim` INTEGER NOT NULL, " +
                        "`valorPecasCentavos` INTEGER NOT NULL, `valorMaoDeObraCentavos` INTEGER NOT NULL, " +
                        "`observacoes` TEXT NOT NULL, " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_manutencao_veiculoId` ON `registros_manutencao` (`veiculoId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `registro_itens` (" +
                        "`registroId` INTEGER NOT NULL, `itemId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`registroId`, `itemId`), " +
                        "FOREIGN KEY(`registroId`) REFERENCES `registros_manutencao`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`itemId`) REFERENCES `itens_manutencao`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registro_itens_registroId` ON `registro_itens` (`registroId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registro_itens_itemId` ON `registro_itens` (`itemId`)")

                // Itens sugeridos para os veículos já cadastrados na etapa 1.
                val veiculos = mutableListOf<Pair<Long, TipoVeiculo>>()
                db.query("SELECT id, tipo FROM veiculos").use { cursor ->
                    while (cursor.moveToNext()) {
                        val tipo = TipoVeiculo.entries.firstOrNull { it.name == cursor.getString(1) }
                            ?: TipoVeiculo.CARRO
                        veiculos += cursor.getLong(0) to tipo
                    }
                }
                veiculos.forEach { (id, tipo) ->
                    ModelosManutencao.criarItens(id, tipo).forEach { item ->
                        db.execSQL(
                            "INSERT INTO itens_manutencao " +
                                "(veiculoId, nome, intervaloKm, intervaloMeses, baseKm, baseData, ativo, ordem) " +
                                "VALUES (?, ?, ?, ?, NULL, NULL, 1, ?)",
                            arrayOf<Any?>(item.veiculoId, item.nome, item.intervaloKm, item.intervaloMeses, item.ordem)
                        )
                    }
                }
            }
        }
    }
}

package ru.yandex.auto.eds.service.trucks.tree

import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.message.TrucksCatalogSchema._

trait DataPreparer {

  protected[tree] def mkMark(code: String, ruName: String): TrucksMarkMessage = {
    val builder = TrucksMarkMessage.newBuilder()
    builder.setVersion(AutoSchemaVersions.TRUCKS_CATALOG_CARDS_VERSION)
    builder.setCode(code)
    builder.setCyrillicName(ruName)
    builder.build()
  }

  protected[tree] def mkModel(code: String, ruName: String): TrucksModelMessage = {
    val builder = TrucksModelMessage.newBuilder()
    builder.setVersion(AutoSchemaVersions.TRUCKS_CATALOG_CARDS_VERSION)
    builder.setCode(code)
    builder.setCyrillicName(ruName)
    builder.build()
  }

  protected[tree] def mkGeneration(id: Long, ruName: String): TrucksGenerationMessage = {
    val builder = TrucksGenerationMessage.newBuilder()
    builder.setVersion(AutoSchemaVersions.TRUCKS_CATALOG_CARDS_VERSION)
    builder.setId(id)
    builder.setCyrillicName(ruName)
    builder.build()
  }

  protected[tree] def mkCatalogCard(
      mark: TrucksMarkMessage,
      model: TrucksModelMessage,
      generation: TrucksGenerationMessage
  ): TrucksCatalogCardMessage = {
    val builder = TrucksCatalogCardMessage.newBuilder()
    builder.setVersion(0)
    builder.setMark(mark)
    builder.setModel(model)
    builder.setGeneration(generation)
    builder.build()
  }

  protected[tree] def mkCatalogCard(mark: TrucksMarkMessage, model: TrucksModelMessage): TrucksCatalogCardMessage = {
    val builder = TrucksCatalogCardMessage.newBuilder()
    builder.setVersion(0)
    builder.setMark(mark)
    builder.setModel(model)
    builder.build()
  }
}

package ru.yandex.vertis.general.wizard.meta.services

import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object BonsaiServiceSpec extends DefaultRunnableSpec {
  private val instruments = Category(id = "instr", state = CategoryState.DEFAULT)
  private val string = Category(id = "string", parentId = "instr", state = CategoryState.DEFAULT)
  private val guitar = Category(id = "guitar", parentId = "string", state = CategoryState.DEFAULT)
  private val violin = Category(id = "violin", parentId = "string", state = CategoryState.DEFAULT)
  private val keyboards = Category(id = "keyboards", parentId = "instr", state = CategoryState.DEFAULT)
  private val wind = Category(id = "wind", parentId = "instr", state = CategoryState.DEFAULT)
  private val trumpet = Category(id = "trumpet", parentId = "wind", state = CategoryState.DEFAULT)
  private val animals = Category(id = "animals", state = CategoryState.DEFAULT)
  private val cats = Category(id = "cats", parentId = "animals", state = CategoryState.DEFAULT)
  private val electronics = Category(id = "electronics", state = CategoryState.DEFAULT)

  private val categories =
    Seq(instruments, string, guitar, violin, keyboards, wind, trumpet, animals, cats, electronics)

  private val snapshotSource: Seq[ExportedEntity] =
    categories.map(category => ExportedEntity(ExportedEntity.CatalogEntity.Category(category)))

  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  private val bonsai = LiveBonsaiService.create(bonsaiSnapshot)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BonsaiService")(
      testM("returns all children") {
        for {
          children <- bonsai.children("instr", onlyLeaf = false)
        } yield assert(children.toSet)(
          equalTo(
            Set(string, guitar, violin, keyboards, wind, trumpet)
          )
        )
      },
      testM("returns only leaf children") {
        for {
          children <- bonsai.children("instr", onlyLeaf = true)
        } yield assert(children.toSet)(
          equalTo(
            Set(guitar, violin, keyboards, trumpet)
          )
        )
      },
      testM("returns nothing") {
        for {
          children <- bonsai.children("electronics", onlyLeaf = false)
        } yield assert(children.toSet)(
          equalTo(
            Set.empty[Category]
          )
        )
      },
      testM("returns children of wind") {
        for {
          children <- bonsai.children("wind", onlyLeaf = false)
        } yield assert(children.toSet)(
          equalTo(
            Set(trumpet)
          )
        )
      },
      testM("returns children of cats") {
        for {
          children <- bonsai.children("cats", onlyLeaf = false)
        } yield assert(children.toSet)(
          equalTo(
            Set.empty[Category]
          )
        )
      },
      testM("returns children of string") {
        for {
          children <- bonsai.children("string", onlyLeaf = true)
        } yield assert(children.toSet)(
          equalTo(
            Set(guitar, violin)
          )
        )
      }
    )
}

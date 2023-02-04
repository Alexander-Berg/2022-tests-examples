package vertis.palma.dao

import ru.yandex.vertis.generators.ProducerProvider
import vertis.zio._
import vertis.palma.dao.model._
import vertis.palma.gen.ModelGenerators._
import vertis.palma.gen.ModelProtoGenerators._
import vertis.palma.model.CustomEquality.DictionaryItemEquality
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.{RIO, UIO, ZIO}

/** @author kusaeva
  */
class DictionariesDaoIntSpec extends ZioSpecBase with YdbTest with ProducerProvider {

  def ioTest(action: DictionariesDaoImpl => RIO[ServerEnv, _]): Unit = {
    val dao = new DictionariesDaoImpl(ydbWrapper, prometheusRegistry)
    ioTest(action(dao))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ioTest { dao =>
      dao.dictionaries.init() *>
        dao.relations.init() *>
        dao.indexes.init()
    }
  }

  private def genItemWithParent(field: String): (DictionaryItem, DictionaryItem) = {
    val parent = DictionaryItemGen.next.copy(relations = Nil)
    val relation = Relation(to = parent.id, field)
    val item = DictionaryItemGen.next.copy(relations = Seq(relation))

    (item, parent)
  }

  "DictionariesDao" should {

    "create if relations exists" in ioTest { dao =>
      val (anItem, parent) = genItemWithParent(field = StrGen.next)
      val index = IndexGen.next
      val item = anItem.copy(indexes = Seq(index))
      val dictionaryId = item.id.dictionaryId

      for {
        _ <- dao.create(parent)
        _ <- dao.create(item)
        loaded <- dao.read(item.id)
        related <- dao.listRelated(to = parent.id, dictionaryId, field = item.relations.head.field)
        page <- dao.list(dictionaryId, index = Some(index))
        _ <- check("item:")(loaded should equal(item))
        _ <- check("related:")(related.items should contain theSameElementsAs Seq(item))
        _ <- check("indexed:")(page.items should contain theSameElementsAs Seq(item))
      } yield ()
    }

    "fail on creation if relation doesn't exist" in {
      intercept[MissingRelationException] {
        ioTest { dao =>
          val parent = DictionaryItemGen.next.copy(relations = Nil)
          val field = StrGen.next
          val relation = Relation(to = parent.id, field)
          val item = DictionaryItemGen.next.copy(relations = Seq(relation))

          dao.create(item)
        }
      }
    }

    "fail on creation if item already exists" in {
      intercept[ItemAlreadyExistsException] {
        ioTest { dao =>
          val (item, parent) = genItemWithParent(field = StrGen.next)
          for {
            _ <- dao.create(parent)
            _ <- dao.create(item)
            _ <- dao.create(item)
          } yield ()
        }
      }
    }

    def updateAndCheck(
        dao: DictionariesDaoImpl
      )(updateEffect: DictionaryItem => RTask[Unit]): ZIO[ServerEnv, Throwable, Unit] = {

      val field = StrGen.next
      val (anItem, parent) = genItemWithParent(field)

      val index = IndexGen.next
      val item = anItem.copy(indexes = Seq(index))

      val dictionaryId = item.id.dictionaryId

      val (newItem, newParent) = genItemWithParent(field)
      val updatedIndex = IndexGen.next
      val updatedItem = newItem.copy(id = item.id, indexes = Seq(updatedIndex))

      for {
        _ <- dao.create(parent)
        _ <- dao.create(item)
        _ <- dao.create(newParent)

        _ <- updateEffect(updatedItem)

        loadedItem <- dao.read(item.id)
        loadedOldParent <- dao.read(parent.id)
        loadedNewParent <- dao.read(newParent.id)
        oldRelated <- dao.listRelated(parent.id, dictionaryId, field)
        related <- dao.listRelated(newParent.id, dictionaryId, field)
        oldPage <- dao.list(dictionaryId, index = Some(index))
        page <- dao.list(dictionaryId, index = Some(updatedIndex))

        _ <- check("item updated:")(loadedItem should equal(updatedItem))
        _ <- check("old parent:")(loadedOldParent should equal(parent))
        _ <- check("new parent:")(loadedNewParent should equal(newParent))
        _ <- check("old relation:")(oldRelated.items shouldBe Nil)
        _ <- check("updated relation:")(related.items should contain theSameElementsAs Seq(updatedItem))
        _ <- check("old index:")(oldPage.items shouldBe Nil)
        _ <- check("updated index:")(page.items should contain theSameElementsAs Seq(updatedItem))
      } yield ()
    }

    "update" in ioTest { dao =>
      updateAndCheck(dao)(dao.update)
    }

    "atomic update" in ioTest { dao =>
      updateAndCheck(dao) { newItem =>
        dao.update(newItem.id) { created =>
          UIO(newItem.copy(id = created.id))
        }
      }
    }

    "fail on update if item key was changed" in {
      intercept[ItemNotFoundException] {
        ioTest { dao =>
          val field = StrGen.next
          val (anItem, parent) = genItemWithParent(field)

          val index = IndexGen.next
          val item = anItem.copy(indexes = Seq(index))
          for {
            _ <- dao.create(parent)
            _ <- dao.create(item)

            _ <- dao.update(item.copy(id = item.id.copy(key = "UPDATED")))
          } yield ()
        }
      }
    }

    "fail on atomic update if item key was changed" in {
      intercept[UnexpectedItemModificationException] {
        ioTest { dao =>
          val field = StrGen.next
          val (anItem, parent) = genItemWithParent(field)

          val index = IndexGen.next
          val item = anItem.copy(indexes = Seq(index))
          for {
            _ <- dao.create(parent)
            _ <- dao.create(item)

            _ <- dao.update(item.id) { item =>
              UIO(item.copy(id = item.id.copy(key = "UPDATED")))
            }
          } yield ()
        }
      }
    }

    "fail on update when relation doesn't exist" in {
      intercept[MissingRelationException] {
        ioTest { dao =>
          val field = StrGen.next
          val (item, parent) = genItemWithParent(field)

          val (newItem, _) = genItemWithParent(field)
          val updatedItem = newItem.copy(id = item.id)

          for {
            _ <- dao.create(parent)
            _ <- dao.create(item)
            _ <- dao.update(updatedItem)
          } yield ()
        }
      }
    }

    "delete if no related items" in {
      intercept[ItemNotFoundException] {
        ioTest { dao =>
          val item = DictionaryItemGen.next.copy(relations = Nil)

          for {
            _ <- dao.create(item)
            _ <- dao.delete(item.id)
            _ <- dao.read(item.id)
          } yield ()
        }
      }
    }

    "delete all indexes and relations" in ioTest { dao =>
      val field = StrGen.next
      val (anItem, parent) = genItemWithParent(field)
      val index = IndexGen.next
      val item = anItem.copy(indexes = Seq(index))
      val dictionaryId = item.id.dictionaryId

      for {
        _ <- dao.create(parent)
        _ <- dao.create(item)
        related <- dao.listRelated(to = parent.id, dictionaryId, field)
        _ <- check("related:")(related.items should contain(item))
        page <- dao.list(dictionaryId, index = Some(index))
        _ <- check("indexed:")(page.items should contain(item))
        _ <- dao.delete(item.id)
        emptyRelated <- dao.listRelated(to = parent.id, dictionaryId, field)
        _ <- check("not related:")(emptyRelated.items shouldBe empty)
        emptyPage <- dao.list(dictionaryId, index = Some(index))
        _ <- check("not indexed:")(emptyPage.items shouldBe empty)
      } yield ()
    }

    "fail on delete when has related items" in {
      intercept[ItemDeleteException] {
        ioTest { dao =>
          val (item, parent) = genItemWithParent(field = StrGen.next)

          for {
            _ <- dao.create(parent)
            _ <- dao.create(item)
            _ <- dao.delete(parent.id)
          } yield ()
        }
      }
    }

    "list items" in ioTest { dao =>
      val dictionaryId = StrGen.next
      val items =
        DictionaryItemGen.next(3).map(i => i.copy(id = i.id.copy(dictionaryId = dictionaryId), relations = Nil))
      val pagination = Pagination(limit = 100)
      for {
        _ <- ZIO.foreach(items)(dao.create)
        page <- dao.list(dictionaryId, pagination)
        _ <- check(page.items should contain theSameElementsAs items)
      } yield ()
    }

    "list items by index" in ioTest { dao =>
      val dictionaryId = StrGen.next
      val indexName = StrGen.next

      val index = Index(name = indexName, value = StrGen.next)
      val otherIndex = Index(name = indexName, value = StrGen.next)

      val items = DictionaryItemGen
        .next(3)
        .map(i => i.copy(id = i.id.copy(dictionaryId = dictionaryId), relations = Nil, indexes = Seq(index)))
      val otherItems = DictionaryItemGen
        .next(3)
        .map(i => i.copy(id = i.id.copy(dictionaryId = dictionaryId), relations = Nil, indexes = Seq(otherIndex)))

      for {
        _ <- ZIO.foreach(items ++ otherItems)(dao.create)
        page <- dao.list(dictionaryId, index = Some(index))
        _ <- check(page.items should contain theSameElementsAs items)
        other <- dao.list(dictionaryId, index = Some(otherIndex))
        _ <- check(other.items should contain theSameElementsAs otherItems)
      } yield ()
    }

    "find related items" in ioTest { dao =>
      val parent = DictionaryItemGen.next.copy(relations = Nil)
      val field = StrGen.next
      val relation = Relation(to = parent.id, field)
      val dictionaryId = StrGen.next
      val items = DictionaryItemGen
        .next(10)
        .map(i => i.copy(id = i.id.copy(dictionaryId = dictionaryId), relations = Seq(relation)))

      for {
        _ <- dao.create(parent)
        _ <- ZIO.foreach(items)(dao.create)
        related <- dao.listRelated(to = parent.id, dictionaryId, field)
        _ <- check(related.items should contain theSameElementsAs items)
      } yield ()
    }

    "allow add few relations on same item" in ioTest { dao =>
      val (anItem, parent) = genItemWithParent(field = StrGen.next)
      val secondRelation = anItem.relations.head.copy(field = StrGen.next)
      val item = anItem.copy(relations = anItem.relations :+ secondRelation)
      val dictionaryId = item.id.dictionaryId

      for {
        _ <- dao.create(parent)
        _ <- dao.create(item)
        loaded <- dao.read(item.id)
        related <- dao.listRelated(to = parent.id, dictionaryId, field = item.relations.head.field)
        _ <- check("item:")(loaded should equal(item))
        _ <- check("related:")(related.items should contain theSameElementsAs Seq(item))
      } yield ()
    }

    "read all" in ioTest { dao =>
      val kitties = "kitties"
      val puppies = "puppies"

      val cats = dictionaryItemGen(kitties).next(4).map(_.copy(relations = Nil, indexes = Nil))
      val dogs = dictionaryItemGen(puppies).next(5).map(_.copy(relations = Nil, indexes = Nil))

      for {
        _ <- ZIO.foreachParN(4)(cats ++ dogs)(dao.create)
        catsStream = dao.all(kitties)
        result <- catsStream.runCollect
        _ <- check {
          result should contain theSameElementsAs cats
        }
      } yield ()
    }
  }
}

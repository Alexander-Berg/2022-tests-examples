package common.palma.test

import common.palma._
import ru.yandex.vertis.palma.samples.Painting
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import java.util.UUID
import java.time.Instant

abstract class BasePalmaSpec extends DefaultRunnableSpec {

  def layer: ULayer[Palma.Palma]

  def fixture = layer.map(_.get).build

  private val globalPrefix = "bzl_"
  private val codePrefix = globalPrefix + UUID.randomUUID().toString() + "_"

  private val painting1 = Painting(codePrefix + "1", "Ван-Гог")
  private val painting2 = Painting(codePrefix + "2", "Шишкин")
  private val painting3 = Painting(codePrefix + "3", "Малевич")
  private val paintingNonExists = Painting(codePrefix + "_nonExists", "Космонавт")

  def spec =
    suite(this.getClass().getSimpleName())(
      testM("create") {
        fixture.use { palma =>
          for {
            created <- palma.create(painting1)
          } yield assertTrue(created.value == painting1)
        }
      },
      testM("create conflict") {
        fixture.use { palma =>
          assertM {
            palma.create(painting1).run
          }(fails(isSubtype[Palma.AlreadyExists](anything)))
        }
      },
      testM("Get not found") {
        fixture.use { palma =>
          assertM {
            palma.get[Painting](paintingNonExists.code)
          }(isNone)
        }
      },
      testM("Get exist") {
        fixture.use { palma =>
          assertM {
            palma.get[Painting](painting1.code)
          }(isSome(isCase("entity", i => Some(i.value), equalTo(painting1))))
        }
      },
      testM("update") {
        fixture.use { palma =>
          for {
            updated <- palma.update[Painting](painting1.copy(author = "other"))
            updated2 <- palma.get[Painting](painting1.code)
            _ <- palma.update(painting1) // restore
          } yield assertTrue(updated.value != painting1) && assertTrue(updated == updated2.get)
        }
      },
      testM("update not found") {
        fixture.use { palma =>
          for {
            error <- palma.update[Painting](painting2).flip
          } yield assert(error)(isSubtype[Palma.NotFound](anything))
        }
      },
      testM("list") {
        fixture.use { palma =>
          for {
            list <- palma.list[Painting](Seq.empty, Some(Palma.Pagination(codePrefix, 1)), None)
          } yield assertTrue(list.items.head.value == painting1)
        }
      },
      testM("delete") {
        fixture.use { palma =>
          for {
            _ <- palma.delete[Painting](painting1.code)
            res <- palma.get[Painting](painting1.code)
          } yield assertTrue(res.isEmpty)
        }
      },
      testM("delete not found") {
        fixture.use { palma =>
          for {
            error <- palma.delete[Painting](painting2.code).flip
          } yield assert(error)(isSubtype[Palma.NotFound](anything))
        }
      },
      testM("list & sort") {
        fixture.use { palma =>
          for {
            list <- palma.list[Painting](Seq.empty, None, Some(Palma.Sorting("author")))
            list2 <- palma.list[Painting](Seq.empty, None, Some(Palma.Sorting("author", false)))
            values = list.items.map(_.value)
            values2 = list2.items.map(_.value)
          } yield assertTrue(values == values.sortBy(e => (e.author, e.code))) &&
            assertTrue(
              values2 == values2.sortBy(e => (e.author, e.code))(
                Ordering.Tuple2(Ordering.String.reverse, Ordering.String)
              )
            )
        }
      },
      testM("stream & cleanup") {
        // т.к. пальма внешний сервис, то надо иногда чистить за собой.
        // тут мы чистим записи, которые были обновлены 10 минут назад или раньше.
        fixture.use { palma =>
          for {
            _ <- palma.create(painting1)
            _ <- palma.create(painting2)
            _ <- palma.create(painting3)
            all <- palma.all[Painting].runCollect
            treshold = Instant.now.minusSeconds(600)
            _ <- ZIO.foreach(all) { item =>
              if (item.updateInfo.lastUpdate.isBefore(treshold)) palma.delete[Painting](item.value.code)
              else ZIO.unit
            }
          } yield assertTrue(all.nonEmpty)
        }
      }
    ) @@ sequential
}

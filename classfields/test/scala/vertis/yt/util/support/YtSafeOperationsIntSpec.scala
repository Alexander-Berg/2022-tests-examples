package vertis.yt.util.support

import common.yt.UnexpectedError
import common.yt.Yt.Attribute._
import org.scalatest.{Assertion, ParallelTestExecution}
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.vertis.generators.ProducerProvider._
import vertis.yt.model.attributes.{YtAttribute, YtAttributes}
import vertis.yt.zio.Aliases.TxTask
import vertis.yt.zio.YtZioTest
import vertis.yt.test.Generators._
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.test.ZioSpecBase.TestBody
import zio._
import zio.duration.{Duration => ZioDuration}

import java.time.temporal.ChronoUnit.DAYS
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.duration._

/** @author kusaeva
  */

class YtSafeOperationsIntSpec extends YtZioTest with YsonSupport with ParallelTestExecution {

  override protected val ioTestTimeout: ZioDuration = ZioDuration.fromScala(3.minute)

  private val YtTableGen = ytTableGen(testBasePath)

  "YtSafeOperations" should {
    "copy all attributes of original table to temporal table" in customTest { ops =>
      import ops.yt

      val optimize = YtAttributes.OptimizeForScan
      val expiration = ExpirationTime.withValue(Instant.now.plus(1L, DAYS).truncatedTo(ChronoUnit.MICROS))
      val userAttr = YtAttributeGen.next
      val attrs: Seq[YtAttribute] = Seq(optimize, userAttr, expiration)
      val table = YtTableGen.next.copy(attributes = attrs)

      def checkAttrs(tmpTablePath: YPath): TxTask[Unit] =
        for {
          tmpAttrs <- yt.cypressTx.getAttributes(tmpTablePath, attrs.map(_._1))
          _ <- check("attributes:") {
            def checkAttr(attr: YtAttribute): Assertion =
              tmpAttrs[YTreeNode](attr._1) shouldBe attr._2

            checkAttr(optimize)
            checkAttr(userAttr)
            checkAttr(expiration)
          }
        } yield ()

      yt.tx.withTx(s"create ${table.title}")(yt.cypressTx.createTable(table)) *>
        yt.cypressNoTx.getAttribute(table.path, Revision) >>= { revision =>
        yt.tx.withTx(s"check ${table.title}")(
          ops.safeRun(table.path, revision)(checkAttrs) *>
            checkAttrs(table.path)
        )
      }
    }
    "not swap tables if revision was changed" in {
      intercept[UnexpectedError] {
        customTest { ops =>
          import ops.yt

          val table = YtTableGen.next

          yt.tx.withTx(s"create ${table.title}")(yt.cypressTx.createTable(table)) *>
            yt.tx.withTx(s"check ${table.title}")(
              ops.safeRun(table.path, 0L)(_ => UIO.unit)
            )
        }
      }
    }
  }

  private def customTest(body: YtSafeOperations => TestBody) =
    ioTest {
      ytResources.use { r =>
        body {
          new YtSafeOperations {
            override def yt: YtZio = r.yt
          }
        }
      }
    }
}

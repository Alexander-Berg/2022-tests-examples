package auto.dealers.multiposting.logic.test.wallet

import java.time.OffsetDateTime

import cats.syntax.option._
import common.scalapb.ScalaProtobuf
import auto.dealers.multiposting.logic.wallet.MultipostingWalletService
import ru.auto.multiposting.filter_model.{AvitoWalletOperationFilter, PeriodFilter}
import auto.dealers.multiposting.logic.wallet.MultipostingWalletService.MultipostingWalletMissedFieldInRequest
import zio.test.Assertion._
import zio.test._

import scala.Option.empty

object RichAvitoWalletOperationFilterSpec extends DefaultRunnableSpec {

  import MultipostingWalletService.RichAvitoWalletOperationFilter

  private val timestamp = ScalaProtobuf.toTimestamp(OffsetDateTime.now)

  def spec: ZSpec[environment.TestEnvironment, Any] =
    suite(classOf[RichAvitoWalletOperationFilter].getName)(
      suite("from")(
        testM("""should extract "from" optional timestamp from filter""") {
          val filter = AvitoWalletOperationFilter(period = PeriodFilter(from = timestamp.some, to = empty).some)

          for {
            ts <- filter.from
          } yield assert(ts)(equalTo(timestamp))
        },
        testM("""should fail when "from" optional timestamp is empty""") {
          val testCases = Seq(
            AvitoWalletOperationFilter(period = empty),
            AvitoWalletOperationFilter(period = PeriodFilter(from = empty, to = empty).some)
          )

          checkAllM(Gen.fromIterable(testCases)) { filter =>
            for {
              ex: MultipostingWalletMissedFieldInRequest <- filter.from.flip
            } yield assert(ex)(hasMessage(equalTo("field 'from' required in request")))
          }
        }
      ),
      suite("to")(
        testM("""should extract "to" optional timestamp from filter""") {
          val filter = AvitoWalletOperationFilter(period = PeriodFilter(from = empty, to = timestamp.some).some)

          for {
            ts <- filter.to
          } yield assert(ts)(equalTo(timestamp))
        },
        testM("""should fail when "to" optional timestamp is empty""") {
          val testCases = Seq(
            AvitoWalletOperationFilter(period = empty),
            AvitoWalletOperationFilter(period = PeriodFilter(from = empty, to = empty).some)
          )

          checkAllM(Gen.fromIterable(testCases)) { filter =>
            for {
              ex: MultipostingWalletMissedFieldInRequest <- filter.to.flip
            } yield assert(ex)(hasMessage(equalTo("field 'to' required in request")))
          }
        }
      )
    )
}

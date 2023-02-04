package auto.dealers.multiposting.logic.test.wallet

import java.time.OffsetDateTime

import auto.dealers.multiposting.logic.wallet.MultipostingWalletService
import auto.dealers.multiposting.model.AvitoDayExpenses
import ru.auto.multiposting.wallet_model.AvitoWalletDailyOperation
import zio.test.Assertion._
import zio.test._

object RichAvitoDayExpensesSpec extends DefaultRunnableSpec {

  import MultipostingWalletService.RichAvitoDayExpenses

  override def spec: ZSpec[environment.TestEnvironment, Any] =
    suite(classOf[RichAvitoDayExpenses].getName)(
      test("""should map AvitoDayExpenses to AvitoWalletDailyOperation""") {
        val avitoDayExpenses = AvitoDayExpenses(
          day = OffsetDateTime.parse("2021-05-11T21:00Z"),
          placementSum = BigDecimal("1500.00"),
          vasSum = BigDecimal("2500.00"),
          otherExpensesSum = BigDecimal("3500.00")
        )

        assert(avitoDayExpenses.toAvitoWallerDailyOperation)(
          equalTo(
            AvitoWalletDailyOperation(
              date = "2021-05-12",
              placement = 15,
              vas = 25,
              other = 35
            )
          )
        )
      }
    )
}

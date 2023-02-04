package ru.auto.salesman.tasks.user.cashback

import com.typesafe.config.ConfigFactory
import ru.auto.salesman.client.{PassportClient, SenderClient}
import ru.auto.salesman.dao.user.TransactionDao
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Funds}
import ru.auto.salesman.model.user.{ProductContext, ProductRequest, Transaction}
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.service.user.PriceService.priceToFunds
import ru.auto.salesman.service.user.{TransactionService, UserPromocodesService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import zio.ZIO

import scala.util.Success

class ApplyCashbackTaskSpec extends BaseSpec with UserModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
  implicit val rc: RequestContext = AutomatedContext("unit-test")

  val transaction: Transaction = TransactionGen.next

  def productWithAmount(amount: Funds): ProductRequest =
    ProductRequestGen.next.copy(
      amount = amount,
      // We want the transaction to pass the filters
      product = ProductProvider.AutoruGoods.Placement,
      context = ProductContext.GoodsContext(ProductPriceGen.next)
    )

  val transactionService: TransactionService = mock[TransactionService]

  val userPromocodesService: UserPromocodesService = mock[UserPromocodesService]

  val passportClient: PassportClient = mock[PassportClient]

  val senderClient: SenderClient = mock[SenderClient]

  val task = new ApplyCashbackTask(
    transactionService,
    userPromocodesService,
    passportClient,
    senderClient,
    // Не создаём Params вручную - нас интересует применяются ли умолчания.
    ApplyCashbackTask.params(ConfigFactory.empty())
  )

  "ApplyCashbackTask" should {
    "apply cashback for amounts above threshold" in {
      // Just at the threshold
      val amount = priceToFunds(30000)
      // Cashback is 10%
      val cashback = priceToFunds(3000)
      setupTransactionService(
        transaction.copy(payload = List(productWithAmount(amount)))
      )
      (userPromocodesService.applyUserCashback _)
        .expects(*, amount, cashback)
      task.execute().isSuccess
    }
    "not apply cashback for amounts below threshold" in {
      // Just below threshold
      val amount = priceToFunds(29999)
      setupTransactionService(
        transaction.copy(payload = List(productWithAmount(amount)))
      )
      (userPromocodesService.applyUserCashback _)
        .expects(*, *, *)
        .never()
      task.execute() shouldBe Success(())
    }
  }

  def setupTransactionService(transaction: Transaction): Unit =
    (transactionService
      .scan(_: TransactionDao.Filter, _: Transaction => Unit))
      .expects(*, *)
      .onCall((_, handler) => ZIO.effect(handler(transaction)))
}

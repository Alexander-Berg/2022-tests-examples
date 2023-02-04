package auto.dealers.booking.scheduler

import ru.auto.api.price_model.KopeckPrice
import auto.dealers.booking.model._
import auto.dealers.booking.scheduler.Banker.Banker
import zio._
import zio.stm._

object TestBanker {

  val live: ULayer[TestBanker with Banker] = {
    val refundedStorage = TMap.make[Payment, KopeckPrice]()
    ZLayer.fromEffectMany {
      for {
        banker <- refundedStorage.map(new Service(_)).commit
      } yield Has.allOf[Banker.Service, TestBanker.Service](banker, banker)
    }
  }

  final case class Payment(userId: UserId, bankerTransactionId: BankerTransactionId)

  type TestBanker = Has[Service]

  class Service(refunded: TMap[Payment, KopeckPrice]) extends Banker.Service {

    override def refundPayment(
        userId: UserId,
        bankerTransactionId: BankerTransactionId,
        amount: KopeckPrice): Task[Unit] =
      refunded.put(Payment(userId, bankerTransactionId), amount).commit

    def amountRefunded(userId: UserId, bankerTransactionId: BankerTransactionId): UIO[KopeckPrice] =
      refunded.get(Payment(userId, bankerTransactionId)).map(_.getOrElse(KopeckPrice(0L))).commit
  }

  def amountRefunded(userId: UserId, bankerTransactionId: BankerTransactionId): URIO[TestBanker, KopeckPrice] =
    ZIO.accessM[TestBanker](_.get.amountRefunded(userId, bankerTransactionId))
}

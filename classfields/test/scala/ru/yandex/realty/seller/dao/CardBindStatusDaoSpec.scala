package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.seller.model.cards.{
  BindStatusForRequest,
  BindStatusPatch,
  BindStatusUpdate,
  BindStatuses,
  CardBindStatus
}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.{CardBindStatusWatchResult, WatchResult}
import ru.yandex.realty.sharding.Shard
import ru.yandex.realty.watching.BatchProcessingResult
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

trait CardBindStatusDaoSpec extends AsyncSpecBase with SellerModelGenerators {

  def dao: CardBindStatusDao

  "Card bind status dao" must {
    "correct watch statuses" in {
      val requestId = sellerStrIdGen.next
      val uid = posNum[Long].next

      dao.create(uid, requestId).futureValue

      val watcher: CardBindStatus => Future[CardBindStatusWatchResult] = { status =>
        val upd = BindStatusUpdate.status(status, BindStatuses.Bound)
        Future.successful(WatchResult(upd, None))
      }

      dao
        .updateStatus(
          BindStatusUpdate(uid, requestId, BindStatusPatch(BindStatuses.PaymentPerformed, Some(DateTimeUtil.now())))
        )
        .futureValue

      val result = dao.watchStatuses(10, Shard(0, 1))(watcher).futureValue // x % 1 == 0

      result shouldEqual BatchProcessingResult(processedCount = 1, failedCount = 0)

      val status = dao.getStatus(BindStatusForRequest(uid, requestId)).futureValue

      status should not be None
      status.get.status shouldEqual BindStatuses.Bound
    }
  }

}

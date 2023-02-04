package billing.cashback.logic.test

import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.client.testkit.TestFeatureToggles
import ru.yandex.vertis.billing.cashback.logic.{
  PaymentExtractorLive,
  PromocoderServiceLive,
  WithdrawCashbackServiceLive
}
import ru.yandex.vertis.billing.cashback.storage.ydb.{YdbLogQueueDao, YdbPaymentEventDao}
import zio.clock.Clock
import zio.test.{suite, testM, ZSpec}

class CancelCashbackSpec {
  /*
   * Cancel
   * 1. После withdraw можем сделать cancel
   * 2. После cancel можно снова сделать withdraw
   * 4. После начала следующего месяца - возврат в активную в данный момент фичу
   */
  // TODO: вынести cleanup в @@ beforeAll. И там же чистить данные в ydb.
//  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = (
//      suite("WithdrawCashbackSpec")(
//        testM ("Withdraw cashback, then cancel.") {
//
//        }
  // testM("Withdraw cashback, then cancel, then withdraw") {
  // }
//      )
//  ).provideCustomLayerShared(
//      TestYdb.ydb >+>
//          Ydb.txRunner >+>
//          YdbPaymentEventDao.live >+>
//          YdbLogQueueDao.live >+>
//          PromocoderClientStub.Test >+>
//          PaymentExtractorLive.live >+>
//          Clock.live >+>
//          PromocoderServiceLive.live >+>
//          Logging.live >+>
//          TestFeatureToggles.live >+>
//          WithdrawCashbackServiceLive.live
//  )
}

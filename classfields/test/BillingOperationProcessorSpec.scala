package vsmoney.dealer_finstat_writer.autoru_monetization

import auto.common.clients.cabinet.testkit.CabinetTest
import billing.finstat.autoru_dealers.AutoruDealersFinstat
import common.zio.events_broker.Broker
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.tracing.testkit.TestTracing
import ru.yandex.vertis.billing.billing_event.BillingOperation
import vsmoney.dealer_finstat_writer.autoru_monetization.BillingOperationTestInstances.Indexing
import vsmoney.dealer_finstat_writer.autoru_monetization.calls.PhoneShowConverter
import zio.ZLayer
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.unit
import zio.test.{assertCompletes, DefaultRunnableSpec, ZSpec}

object BillingOperationProcessorSpec extends DefaultRunnableSpec {

  val brokerNeverCalled = AutoruDealersFinstatBrokerMock.empty

  val billingOperationConvertor =
    CabinetTest.empty ++ Clock.live >>> PhoneShowConverter.live ++ Logging.live >>> BillingOperationConverter.live

  val testEnv = (
    ZLayer.service[
      Broker.Typed[AutoruDealersFinstat]
    ] ++ billingOperationConvertor ++ Prometheus.live ++ TestTracing.noOp ++ Logging.live
  ) >>> BillingOperationProcessor.live

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("BillingOperationProcessorSpec")(
      testM("Метод не падает, если передать туда что-нибудь") {
        for {
          _ <- BillingOperationProcessor
            .process(List(BillingOperation()))
            .provideCustomLayer(brokerNeverCalled >>> testEnv)
        } yield assertCompletes
      },
      testM("Если ничего не передать, то тоже не падает") {
        for {
          _ <- BillingOperationProcessor
            .process(List.empty)
            .provideCustomLayer(brokerNeverCalled >>> testEnv)
        } yield assertCompletes
      },
      testM("Отправляем сконвертированную стату по дилерам в брокер") {
        val brokerCalledWithExpected = AutoruDealersFinstatBrokerMock.Send(anything, unit)
        for {
          _ <- BillingOperationProcessor
            .process(List(Indexing.default))
            .provideCustomLayer(brokerCalledWithExpected >>> testEnv)
        } yield assertCompletes
      }
    )
  }
}

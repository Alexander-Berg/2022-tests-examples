package vsmoney.dealer_finstat_writer.autoru_monetization

import auto.common.clients.cabinet.Cabinet
import auto.common.clients.cabinet.Cabinet.Cabinet
import auto.common.clients.cabinet.testkit.CabinetTest
import common.zio.logging.Logging
import common.zio.ops.tracing.Tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import ru.auto.cabinet.api_model.ClientIdResponse
import ru.yandex.vertis.billing.billing_event.BillingOperation.WithdrawPayload.EventType
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.BillingDomain
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import vsmoney.dealer_finstat_writer.autoru_monetization.BillingOperationTestInstances._
import vsmoney.dealer_finstat_writer.autoru_monetization.calls.PhoneShowConverter
import zio.clock.Clock
import zio.test.Assertion.{anything, fails, isSubtype, _}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{ULayer, ZLayer}

object BillingOperationConverterSpec extends DefaultRunnableSpec {

  val cabinetNeverCalled = CabinetTest.empty

  val testEnv =
    ZLayer.service[
      Cabinet.Service
    ] ++ Clock.live ++ TestTracing.noOp >>> PhoneShowConverter.live ++ Logging.live >>> BillingOperationConverter.live

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("BillingOperationConverterSpec")(
      testM("Скипаем не autoru домены") {
        val input = Indexing.default.withDomain(BillingDomain.REALTY)
        for {
          converted <- BillingOperationConverter.toFinstatModel(input)
        } yield assertTrue(converted.isEmpty)
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Скипаем, то что не списания") {
        val transactionInfo = defaultTransactionInfo.withType(TransactionType.INCOMING)
        val input = Indexing.default.clearWithdrawPayload.withTransactionInfo(transactionInfo)
        for {
          converted <- BillingOperationConverter.toFinstatModel(input)
        } yield assertTrue(converted.isEmpty)
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Скипаем, то что не INDEXING или PHONE_SHOW") {
        val input = Indexing.default.withWithdrawPayload(Indexing.withdrawPayload.withEventType(EventType.CLICK))
        for {
          converted <- BillingOperationConverter.toFinstatModel(input)
        } yield assertTrue(converted.isEmpty)
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Если не заполнен timestamp, фейлимся") {
        val input = Indexing.default.clearTimestamp
        for {
          res <- BillingOperationConverter.toFinstatModel(input).run
        } yield assert(res)(fails(isSubtype[BillingOperationNotConsistent](anything)))
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Если не заполнен operationTimestamp, фейлимся") {
        val input = Indexing.default.clearOperationTimestamp
        for {
          res <- BillingOperationConverter.toFinstatModel(input).run
        } yield assert(res)(fails(isSubtype[BillingOperationNotConsistent](anything)))
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Конвертируем Indexing сообщение в котором заполнены обязательные данные") {
        val input = Indexing.default
        for {
          converted <- BillingOperationConverter.toFinstatModel(input).orDie
        } yield assertTrue(converted.get == Indexing.converted)
      }.provideCustomLayer(cabinetNeverCalled >>> testEnv),
      testM("Конвертируем PhoneShow сообщение в котором заполнены обязательные данные") {
        val cabinet: ULayer[Cabinet] = CabinetTest.GetClientIdByPoi(
          equalTo(PhoneShow.poiId),
          value(ClientIdResponse(PhoneShow.clientId))
        )

        {
          val input = PhoneShow.default
          for {
            converted <- BillingOperationConverter.toFinstatModel(input).orDie
          } yield assertTrue(converted.get == PhoneShow.converted)
        }.provideCustomLayer(cabinet >>> testEnv)
      }
    )
  }

}

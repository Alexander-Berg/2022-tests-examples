package ru.auto.comeback.services.test

import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import ru.auto.comeback.services.LivePaymentsService
import ru.auto.comeback.services.feature.ComebackFeatures
import ru.auto.multiposting.warehouse_model.WarehouseDailyState
import ru.auto.multiposting.warehouse_service.WarehouseServiceGrpc.WarehouseService
import ru.auto.multiposting.warehouse_service.{
  GetWarehouseDailyStateRequest,
  GetWarehouseDailyStateResponse,
  GetWarehouseUniqueCountersRequest,
  GetWarehouseUniqueCountersResponse
}
import ru.yandex.vertis.feature.model.Feature
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, ZSpec, _}

import scala.concurrent.Future

object PaymentsServiceSpec extends DefaultRunnableSpec {

  private val trueFeature = new Feature[Boolean] {
    override def name: String = "feature"
    override def value: Boolean = true
  }

  private val falseFeature = new Feature[Boolean] {
    override def name: String = "feature"
    override def value: Boolean = false
  }

  private def features(feature: Feature[Boolean]): ComebackFeatures.Service = new ComebackFeatures.Service {
    override val regularPaymentsRequired: Feature[Boolean] = feature
    override val shouldProcessComebackUpdates: Feature[Boolean] = falseFeature
    override val shouldSendComebackEmails: Feature[Boolean] = falseFeature
  }

  private def grpcClient(provideRegularPayments: Boolean): GrpcClient.Service[WarehouseService] =
    new GrpcClientLive[WarehouseService](new WarehouseServiceImpl(provideRegularPayments), null)

  private class WarehouseServiceImpl(provideRegularPayments: Boolean) extends WarehouseService {

    override def getWarehouseDailyState(
        request: GetWarehouseDailyStateRequest): Future[GetWarehouseDailyStateResponse] = {
      val days = if (provideRegularPayments) {
        for (_ <- 0 to 9) yield WarehouseDailyState(totalActive = 100)
      } else {
        for (_ <- 0 to 8) yield WarehouseDailyState(totalActive = 100)
      }
      Future.successful(GetWarehouseDailyStateResponse(days = days))

    }

    override def getWarehouseUniqueCounters(
        request: GetWarehouseUniqueCountersRequest): Future[GetWarehouseUniqueCountersResponse] = ???
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PaymentsService")(
      testM("requireRegularPayments returns true if payments are regular") {
        val service = new LivePaymentsService(grpcClient(true), features(trueFeature))
        service
          .requireRegularPayments(123)(ZIO.succeed(true), ZIO.succeed(false))
          .map(result => assert(result)(equalTo(true)))
      },
      testM("requireRegularPayments returns false if payments are irregular") {
        val service = new LivePaymentsService(grpcClient(false), features(trueFeature))
        service
          .requireRegularPayments(123)(ZIO.succeed(true), ZIO.succeed(false))
          .map(result => assert(result)(equalTo(false)))
      },
      testM("requireRegularPayments returns true if payments feature is off, even if payments are irregular") {
        val service = new LivePaymentsService(grpcClient(false), features(falseFeature))
        service
          .requireRegularPayments(123)(ZIO.succeed(true), ZIO.succeed(false))
          .map(result => assert(result)(equalTo(true)))
      }
    )
  }
}

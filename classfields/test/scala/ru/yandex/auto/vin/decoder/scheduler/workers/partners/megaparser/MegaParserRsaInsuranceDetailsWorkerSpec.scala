package ru.yandex.auto.vin.decoder.scheduler.workers.partners.megaparser

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.megaparser.client.MegaParserClient
import ru.yandex.auto.vin.decoder.partners.megaparser.converter.rsa.InsuranceDetailsToPreparedConverter
import ru.yandex.auto.vin.decoder.partners.megaparser.exceptions.CannotCreateOrderException
import ru.yandex.auto.vin.decoder.partners.megaparser.model._
import ru.yandex.auto.vin.decoder.partners.megaparser.model.rsa.InsuranceDetailsResponse
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportOrderState.{InsuranceReportOrder, ReportOrder}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.proto.{SchedulerModel, VinHistory}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.MixedRateLimiter
import ru.yandex.auto.vin.decoder.scheduler.workers.partners.megaparser.rsa.MegaParserRsaInsuranceDetailsWorker
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

class MegaParserRsaInsuranceDetailsWorkerSpec extends AnyWordSpecLike with MockitoSupport with BeforeAndAfterEach {

  implicit val t: Traced = Traced.empty
  implicit val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()
  implicit val ec = ExecutionContext.global

  val reportType = MegaParserRsaReportType.insuranceDetails
  val converter = mock[InsuranceDetailsToPreparedConverter[VinCode]]
  val client = mock[MegaParserClient]
  val rateLimiter = MixedRateLimiter(100, 100, 1)
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val dao = mock[VinWatchingDao]
  val queue = mock[WorkersQueue[VinCode, SchedulerModel.CompoundState]]
  val feature = mock[Feature[Boolean]]

  val vin = VinCode("X4X3D59430PS96744")
  val insurancesEvent = MegaParserRsaReportType.currentInsurances
  val detailsEvent = MegaParserRsaReportType.insuranceDetails

  val worker = new MegaParserRsaInsuranceDetailsWorker(
    client,
    converter,
    rateLimiter,
    dao,
    rawStorageManager,
    queue,
    feature,
    feature
  )

  override def beforeEach(): Unit = {
    reset(rawStorageManager)
    reset(dao)
  }

  "MegaParserRsaInsuranceDetailsWorker" should {
    "delay execution if current insurances not received yet" in {
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setShouldProcess(true)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val res = worker.action(state)
      assert(res.updater.get.delay().toDuration >= 30.seconds && res.updater.get.delay().toDuration <= 2.minute)

    }

    "cancel execution if invalid identifier in current insurances" in {
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())
        .setInvalid(true)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val res = worker.action(state)
      val updated = res.updater.get(state.toUpdate)
      assert(res.updater.get.delay().toDuration == 0.seconds)
      assert(!updated.state.getMegaparserRsaState.findReport(detailsEvent.toString).head.getShouldProcess)
    }

    "finish state" in {
      val b = CompoundState.newBuilder()

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      b.getMegaparserRsaStateBuilder.addInsuranceOrders(insOrder)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)
      val res = worker.action(state)
      val updated = res.updater.get(state.toUpdate)
      assert(!updated.state.getMegaparserRsaState.findReport(detailsEvent.toString).get.getShouldProcess)

    }

    "update current insurances if not fresh" in {
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(1)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val res = worker.action(state)
      val upd = res.updater.get.stateUpdate.apply(b.build())
      assert(res.updater.get.delay().toDuration == 0.seconds)
      assert(
        upd.getMegaparserRsaState.getOrdersList.asScala
          .find(_.getReportType == insurancesEvent.toString)
          .head
          .getShouldProcess
      )

    }

    "request megaparser current insurances if they aren't found" in {
      val b = CompoundState.newBuilder()

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val res = worker.action(state)
      assert(res.updater.get.delay().toDuration == 0.seconds)
      val upd = res.updater.get.stateUpdate.apply(b.build())
      assert(
        upd.getMegaparserRsaState.getOrdersList.asScala
          .find(_.getReportType == insurancesEvent.toString)
          .head
          .getShouldProcess
      )
    }

    "make multiple insurances requests for different insurances" in {

      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("1234").build())
                .build()
            )
          )
        )
      )

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.size == 2)
      assert(insOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(insOrders.asScala.forall(_.getReport.getRequestSent == 0))
    }

    "make only one request for new insurance" in {
      val b = CompoundState.newBuilder()

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      b.getMegaparserRsaStateBuilder.addInsuranceOrders(insOrder)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("1234").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.size == 2)

      val freshReport = insOrders.asScala.find(_.getNumbers == "123").get.getReport

      assert(!freshReport.getShouldProcess)
      assert(freshReport.getRequestSent == 0L)

      val newInsuranceReport = insOrders.asScala.find(_.getNumbers == "1234").get.getReport

      assert(newInsuranceReport.getShouldProcess)
    }

    "make only one request for old order" in {
      val b = CompoundState.newBuilder()

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis() - 31.day.toMillis)
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(insOrder)
        .addInsuranceOrders(insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("1234").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.size == 2)

      val freshReport = insOrders.asScala.find(_.getNumbers == "1234").get.getReport

      assert(!freshReport.getShouldProcess)
      assert(freshReport.getRequestSent == 0L)

      val newInsuranceReport = insOrders.asScala.find(_.getNumbers == "123").get.getReport

      assert(newInsuranceReport.getShouldProcess)
    }

    "make only one request when orders count equals insurances count, but got another insurance from MP" in {
      val b = CompoundState.newBuilder()

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(insOrder)
        .addInsuranceOrders(insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("12345").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.size == 3)

      val anotherReport = insOrders.asScala.find(_.getNumbers == "12345").get.getReport

      assert(anotherReport.getShouldProcess)

      val existsWrongProcessing =
        insOrders.asScala.filter(_.getNumbers != "12345").forall(!_.getReport.getShouldProcess)

      assert(existsWrongProcessing)
    }

    "finish state when OLD orders count greater insurances count, but got another insurance from MP and exists fresh order" in {
      val b = CompoundState.newBuilder()

      val insOrderOld = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis() - 50.days.toMillis)
        )

      val insOrderFresh = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234567")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      val anotherOrderFresh = InsuranceReportOrder
        .newBuilder()
        .setNumbers("12345")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(insOrderOld)
        .addInsuranceOrders(insOrderFresh)
        .addInsuranceOrders(anotherOrderFresh)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("12345").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(res.updater.get.delay().toDuration == 0.seconds)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(!updated.state.getMegaparserRsaState.findReport(detailsEvent.toString).head.getShouldProcess)
      assert(insOrders.size() == 3)
    }

    "make only one request when OLD orders count greater insurances count, but got another insurance from MP" in {
      val b = CompoundState.newBuilder()

      val insOrderOld = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis() - 50.days.toMillis)
        )

      val insOrderFresh = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234567")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setReportArrived(System.currentTimeMillis())
        )

      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(insOrderOld)
        .addInsuranceOrders(insOrderFresh)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("12345").build())
                .build()
            )
          )
        )
      )

      val state = WatchingStateHolder(vin, b.build(), 1)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.size == 3)

      val anotherReport = insOrders.asScala.find(_.getNumbers == "12345").get.getReport

      assert(anotherReport.getShouldProcess)

      val existsWrongProcessing =
        insOrders.asScala.filter(_.getNumbers != "12345").forall(!_.getReport.getShouldProcess)

      assert(existsWrongProcessing)
    }

    "create insurance details orders" in {

      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("1234").build())
                .build()
            )
          )
        )
      )

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(insOrders.asScala.forall(_.getReport.getRequestSent == 0))
      assert(insOrders.size() == 2)
    }

    "finish state when no ins in db" in {
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .build()
            )
          )
        )
      )

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(res.updater.get.delay().toDuration == 0.seconds)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(!updated.state.getMegaparserRsaState.findReport(detailsEvent.toString).head.getShouldProcess)
      assert(insOrders.size() == 0)
    }

    "re-create insurance details orders" in {
      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
        )
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(0, insOrder)
        .addInsuranceOrders(0, insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          List(
            Prepared.simulate(
              VinInfoHistory
                .newBuilder()
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("123").build())
                .addInsurances(VinHistory.Insurance.newBuilder().setSerial("ХХХ").setNumber("1234").build())
                .build()
            )
          )
        )
      )

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(insOrders.asScala.forall(_.getReport.getRequestSent == 0))
      assert(insOrders.size() == 2)
    }

    "make multiple orders for different insurances" in {

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setShouldProcess(true)
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(0, insOrder)
        .addInsuranceOrders(0, insOrder2)
      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(insOrders.asScala.forall(_.getReport.getOrderId == "444"))
      assert(insOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(insOrders.asScala.forall(_.getReport.getRequestSent != 0))
    }

    "make multiple orders for errors/successful insurance" in {
      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setSeries("ХХХ")
        .setNumbers("243454")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setShouldProcess(true)
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setSeries("ХХХ")
        .setNumbers("9921369390")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(0, insOrder)
        .addInsuranceOrders(1, insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      when(client.makeOrder(?, ?, ?)(?, ?))
        .thenReturn(Future.successful(makeOrderResponse("444")), Future.failed(CannotCreateOrderException("", "")))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList

      val successfulOrder = insOrders.get(0)
      assert(0 != successfulOrder.getReport.getRequestSent)
      assert(successfulOrder.getReport.getShouldProcess)
      assert("444" == successfulOrder.getReport.getOrderId)

      val failedOrder = insOrders.get(1)
      assert(0 == failedOrder.getReport.getRequestSent)
      assert(failedOrder.getReport.getShouldProcess)
      assert(failedOrder.getReport.getOrderId.isEmpty)
    }

    "get data for multiple insurances" in {

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setNumbers("123")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setOrderId("466")
            .setRequestSent(1)
            .setShouldProcess(true)
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setNumbers("1234")
        .setSeries("ХХХ")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setOrderId("522")
            .setRequestSent(1)
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(0, insOrder)
        .addInsuranceOrders(0, insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val raw = ResourceUtils.getStringFromResources("/megaparser/rsa/non_empty_200.json")
      val response = MegaParserReportResponse.parse[VinCode, InsuranceDetailsResponse](
        200,
        vin,
        raw,
        List("_rsa"),
        MegaParserRsaReportType.insuranceDetails.eventType
      )
      when(client.makeOrder(?, ?, ?)(?, ?)).thenReturn(Future.successful(makeOrderResponse("444")))
      when(client.getReport[VinCode, InsuranceDetailsResponse](?, ?)(?, ?)).thenReturn(Future.successful(response))

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val stsOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList
      assert(stsOrders.asScala.forall(_.getReport.getShouldProcess))
      assert(stsOrders.asScala.forall(_.getReport.getRequestSent != 0))
    }

    "handle error/successes" in {

      val insOrder = InsuranceReportOrder
        .newBuilder()
        .setSeries("ХХХ")
        .setNumbers("123")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setOrderId("466")
            .setRequestSent(1)
            .setShouldProcess(true)
        )

      val insOrder2 = InsuranceReportOrder
        .newBuilder()
        .setSeries("ХХХ")
        .setNumbers("1234")
        .setReport(
          ReportOrder
            .newBuilder()
            .setReportType("MEGA_PARSER_INSURANCE_DETAILS")
            .setOrderId("522")
            .setRequestSent(1)
            .setShouldProcess(true)
        )
      val b = CompoundState.newBuilder()
      b.getMegaparserRsaStateBuilder
        .addInsuranceOrders(0, insOrder)
        .addInsuranceOrders(0, insOrder2)

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(insurancesEvent)
        .setReportArrived(System.currentTimeMillis())

      b.getMegaparserRsaStateBuilder
        .getReportBuilder(detailsEvent)
        .setShouldProcess(true)

      val state = WatchingStateHolder(vin, b.build(), 1)
      val raw = ResourceUtils.getStringFromResources("/megaparser/rsa/non_empty_200.json")
      val response = MegaParserReportResponse.parse[VinCode, InsuranceDetailsResponse](
        200,
        vin,
        raw,
        List("_rsa"),
        MegaParserRsaReportType.insuranceDetails.eventType
      )
      when(client.getReport[VinCode, InsuranceDetailsResponse](?, ?)(?, ?))
        .thenReturn(Future.successful(response), Future.failed(new RuntimeException))
      when(converter.convert(?)(?)).thenReturn(Future.successful(VinInfoHistory.getDefaultInstance))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)
      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

      assert(res.updater.nonEmpty)
      assert(!res.updater.get.delay().isDefault)

      val updated = res.updater.get(state.toUpdate)
      val insOrders = updated.state.getMegaparserRsaState.getInsuranceOrdersList

      val successfulOrder = insOrders.get(0)
      assert(!successfulOrder.getReport.getShouldProcess)
      assert(successfulOrder.getReport.getReportArrived != 0)
      assert(successfulOrder.getReport.getCounter == 0)

      val failedOrder = insOrders.get(1)

      assert(failedOrder.getReport.getShouldProcess)
      assert(failedOrder.getReport.getReportArrived == 0)
      assert(failedOrder.getReport.getCounter == 1)
    }

  }

  private def makeOrderResponse(id: String) = MegaParserOrderResponse("", Queue("", None, None, None, None, id), Nil)
}

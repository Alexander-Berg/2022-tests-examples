package ru.yandex.auto.recalls.scheduler.tasks

import auto.carfax.common.clients.carfax.CarfaxClient
import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.sender.SenderClient
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito._
import org.mockito.internal.verification.Times
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CatalogModel
import ru.auto.api.CatalogModel.Mark
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.api.vin.VinReportModel.{RawVinEssentialsReport, VehicleInfo}
import ru.yandex.auto.recalls.core.db._
import ru.yandex.auto.recalls.core.enums.NotificationStatus
import ru.yandex.auto.recalls.core.testkit.RecallsPgDatabaseContainer
import auto.carfax.common.storages.pg.PostgresProfile.api._
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import slick.dbio.DBIO

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RecallLetterSendTaskSpec
  extends AnyFunSuite
  with MockitoSupport
  with RecallsPgDatabaseContainer
  with BeforeAndAfterAll {

  val sellAllReports = mock[Feature[Boolean]]
  implicit val tracer = NoopTracerFactory.create()

  def fixture =
    new {
      val senderClient: SenderClient = mock[SenderClient]
      val passportClient: PassportClient = mock[PassportClient]

      val featureRecallLetters: Feature[Boolean] = mock[Feature[Boolean]]
      val carfaxClient: CarfaxClient = mock[CarfaxClient]
      val userEssentials: UserEssentials = UserEssentials.newBuilder().setEmail("email").build()

      val vehicle: VehicleInfo = VehicleInfo
        .newBuilder()
        .setCarInfo(
          CarInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder().setName("Honda"))
            .setModelInfo(CatalogModel.Model.newBuilder().setName("Civic"))
        )
        .build()

      val rawVinEssentialsReport: RawVinEssentialsReport =
        RawVinEssentialsReport
          .newBuilder()
          .setVehicle(vehicle)
          .build()

      val rawEssentialsReportResponse: RawEssentialsReportResponse =
        RawEssentialsReportResponse.newBuilder().setReport(rawVinEssentialsReport).build()

      when(passportClient.getEssentials(?)(?)).thenReturn(Future.successful(userEssentials))
      when(featureRecallLetters.value).thenReturn(true)
      when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.successful(rawEssentialsReportResponse))
    }

  test("send recall") {
    val f = fixture
    import f._
    when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.successful(()))

    Await.result(generateData(), 1.minute)
    val task = new RecallLetterSendTask(
      senderClient,
      passportClient,
      database,
      carfaxClient,
      featureRecallLetters
    )
    Await.result(task.run, 1.minute)
    verify(senderClient, new Times(2)).sendLetter(?, ?, ?)(?)
    verify(passportClient, new Times(2)).getEssentials(?)(?)
    Await.result(task.run, 1.minute)
    verifyNoMoreInteractions(senderClient)
    verifyNoMoreInteractions(passportClient)

    val res: Notification =
      Await.result(
        database.runRead(onMaster = true)(Notification.entries.filter(_.userCardId === 3L).result.head),
        1.minute
      )
    assert(res.status.contains(NotificationStatus.NoActionNeeded))

  }

  test("send recall only once") {
    val f = fixture
    import f._
    when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future(Thread.sleep(1000)))

    val notificationsReq = Notification.entries.map(v => (v.recallId, v.userCardId)) ++=
      Seq((1L, 1L), (1L, 2L), (1L, 3L))
    Await.result(database.runWrite(DBIO.seq(Notification.entries.delete, notificationsReq)), 10.seconds)

    val task = new RecallLetterSendTask(
      senderClient,
      passportClient,
      database,
      carfaxClient,
      featureRecallLetters
    )
    Await.result(task.run, 1.minute)
    Await.result(task.run, 1.minute)
    verify(senderClient, new Times(2)).sendLetter(?, ?, ?)(?)
    verify(passportClient, new Times(2)).getEssentials(?)(?)
    verifyNoMoreInteractions(senderClient)
    verifyNoMoreInteractions(passportClient)

    val res: Notification =
      Await.result(
        database.runRead(onMaster = true)(Notification.entries.filter(_.userCardId === 3L).result.head),
        1.minute
      )
    assert(res.status.contains(NotificationStatus.NoActionNeeded))

  }

  test("mark letter as error on exception") {
    val f = fixture
    import f._
    when(carfaxClient.getEssentialsReport(?)(?)).thenReturn(Future.failed(new IllegalArgumentException))
    val notificationsReq = Notification.entries.map(v => (v.recallId, v.userCardId)) ++= Seq((1L, 4L))

    Await.result(database.runWrite(DBIO.seq(Notification.entries.delete, notificationsReq)), 10.seconds)

    val task = new RecallLetterSendTask(
      senderClient,
      passportClient,
      database,
      carfaxClient,
      featureRecallLetters
    )
    Await.result(task.run, 1.minute)
    Await.result(task.run, 1.minute)
    verifyNoMoreInteractions(senderClient)
    verifyNoMoreInteractions(passportClient)

    val res: Notification =
      Await.result(
        database.runRead(onMaster = true)(Notification.entries.filter(_.userCardId === 4L).result.head),
        1.minute
      )
    assert(res.status.contains(NotificationStatus.Failed))
  }

  private def generateData(): Future[Unit] = {

    val nowTimestamp = new Timestamp(System.currentTimeMillis())
    val userCardReq = UserCard.entries.map(v => (v.userId, v.vinCodeId, v.subscribed)) ++=
      Seq(
        ("user:1", 1L, Some(nowTimestamp)),
        ("user:2", 1L, Some(nowTimestamp)),
        ("user:3", 1L, None),
        ("user:4", 1L, Some(nowTimestamp))
      )
    val campaignReq = Campaign.entries.map(v => (v.manufacturer, v.url)) += (("", ""))
    val recallsReq = Recall.entries.map(v => (v.id, v.campaignId, v.title, v.marks, v.models, v.published)) += (
      (
        1,
        Some(1),
        "",
        "",
        "",
        Some(nowTimestamp)
      )
    )
    val recallsVinCodesReq = RecallVinCode.entries.map(v => (v.recallId -> v.vinCodeId)) += (1L -> 1L)
    val vinCodesReq = VinCodesTable.entries.map(v => (v.id, v.vinCode)) += ((1L, "vin"))
    val notificationsReq = Notification.entries.map(v => (v.recallId, v.userCardId)) ++=
      Seq((1L, 1L), (1L, 2L), (1L, 3L))

    val insertData =
      DBIO.seq(campaignReq, vinCodesReq, userCardReq, recallsReq, recallsVinCodesReq, notificationsReq).transactionally

    database.runAll(insertData)
  }
}

package vsquality.complaints.logic.test

import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.TestAspect._
import zio.{Has, IO, Task, ZIO, ZLayer}
import common.zio.doobie.testkit.TestPostgresql
import vsquality.complaints.logic.HoboTaskManager.HoboTaskManager
import vsquality.complaints.logic.{BlacklistManager, ComplaintsManager, HoboTaskManager}
import vsquality.complaints.model._
import ru.yandex.vertis.hobo.proto.model.{
  AutoruComplaintsResellerResolution,
  External,
  Payload,
  QueueId,
  Resolution => ProtoResolution,
  Task => ProtoTask
}
import vsquality.complaints.storage.{AnalyticsDao, ComplaintsDao, QueueDao}
import vsquality.complaints.storage.postgresql.{PgAnalyticsDao, PgBlacklistDao, PgComplaintsDao, PgQueueDao}
import zio.magic._
import doobie.util.transactor.Transactor
import common.zio.doobie.syntax._
import common.zio.logging.Logging
import ru.yandex.vertis.hobo.proto.model.Task.State
import vsquality.complaints.logic.BlacklistManager.BlacklistManager
import vsquality.complaints.logic.ComplaintsManager.ComplaintsManager
import vsquality.complaints.storage.AnalyticsDao.AnalyticsDao
import vsquality.complaints.storage.ComplaintsDao.ComplaintsDao
import vsquality.complaints.storage.QueueDao.QueueDao

import scala.util.Random

object HoboTaskManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val resolution = Some(
      ProtoResolution.defaultInstance.copy(autoruComplaintsReseller =
        Some(
          AutoruComplaintsResellerResolution.defaultInstance
            .copy(values = Seq(AutoruComplaintsResellerResolution.Value.RESELLER))
        )
      )
    )
    val offerId = "11111"
    val payload =
      Some(Payload.defaultInstance.copy(external = Some(External.defaultInstance.copy(ids = Seq(s"ATV:$offerId")))))

    val hoboKey = "hobokey1"

    val task: ProtoTask = ProtoTask.defaultInstance.copy(
      key = Some(hoboKey),
      resolution = resolution,
      payload = payload,
      queue = Some(QueueId.AUTO_RU_COMPLAINTS)
    )

    val complaintId = Random.nextLong().toString
    val offerOwnerId = Random.nextLong().toString
    val complainantId = Random.nextLong().toString
    val complaint = createComplaint(complaintId, offerId, offerOwnerId, complainantId)
    val record = toRecord(complaint)
    val context = Context(complaint.id, None, None, None, None)

    (suite("HoboTaskManager")(
      testM("process hobo completed task") {
        for {
          complaintsDao    <- ZIO.service[ComplaintsDao.Service]
          queueDao         <- ZIO.service[QueueDao.Service]
          analyticsDao     <- ZIO.service[AnalyticsDao.Service]
          _                <- complaintsDao.upsert(Seq(complaint)).transactIO
          _                <- queueDao.insert(record).transactIO
          _                <- analyticsDao.upsert(context).transactIO
          _                <- HoboTaskManager.process(task.copy(state = Some(State.COMPLETED)))
          updatedComplaint <- complaintsDao.get(complaintId).transactIO
          updatedRecord    <- queueDao.getRecord(offerId, Domain.AUTORU).transactIO
        } yield assertTrue(updatedComplaint.hoboKey.get == hoboKey && updatedRecord.status == Status.COOL_DOWN)
      },
      testM("delete canceled tasks from queue") {
        for {
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          _             <- complaintsDao.upsert(Seq(complaint)).transactIO
          _             <- queueDao.insert(record).transactIO
          _             <- HoboTaskManager.process(task.copy(state = Some(State.CANCELED)))
          sameComplaint <- complaintsDao.get(complaintId).transactIO
          isInQueue     <- queueDao.isOfferInQueue(offerId, Domain.AUTORU).transactIO
        } yield assertTrue(sameComplaint == complaint && !isInQueue)
      },
      testM("delete expired tasks from queue") {
        for {
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          _             <- complaintsDao.upsert(Seq(complaint)).transactIO
          _             <- queueDao.insert(record).transactIO
          _             <- HoboTaskManager.process(task.copy(state = Some(State.EXPIRED)))
          sameComplaint <- complaintsDao.get(complaintId).transactIO
          isInQueue     <- queueDao.isOfferInQueue(offerId, Domain.AUTORU).transactIO
        } yield assertTrue(sameComplaint == complaint && !isInQueue)
      },
      testM("do nothing if offer is not in queue") {
        for {
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          _             <- complaintsDao.upsert(Seq(complaint)).transactIO
          _             <- queueDao.insert(record).transactIO
          _ <- HoboTaskManager.process(
            task.copy(
              state = Some(State.COMPLETED),
              payload = Some(
                Payload.defaultInstance.copy(external = Some(External.defaultInstance.copy(ids = Seq(s"ATV:random"))))
              )
            )
          )
          sameComplaint <- complaintsDao.get(complaintId).transactIO
          isInQueue     <- queueDao.isOfferInQueue(offerId, Domain.AUTORU).transactIO
        } yield assertTrue(sameComplaint == complaint && isInQueue)
      }
    ) @@ sequential)
      .provideCustomLayer {

        ZLayer.fromSomeMagic[
          _root_.zio.test.environment.TestEnvironment,
          BlacklistManager with ComplaintsDao with QueueDao with Has[
            Transactor[Task]
          ] with ComplaintsManager with HoboTaskManager with AnalyticsDao
        ](
          TestPostgresql.managedTransactor,
          ZIO.succeed(modClientMock).toLayer,
          ZIO.succeed(producerMock).toLayer,
          Logging.live,
          PgComplaintsDao.live,
          PgQueueDao.live,
          PgBlacklistDao.live,
          PgAnalyticsDao.live,
          BlacklistManager.live,
          ComplaintsManager.live,
          HoboTaskManager.live
        )

      }
  }
}

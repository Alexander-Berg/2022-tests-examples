package vsquality.complaints.logic.test

import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.TestAspect._
import zio.{Has, Task, ZIO, ZLayer}
import common.zio.doobie.testkit.TestPostgresql
import vsquality.complaints.logic.{BlacklistManager, ComplaintsManager}
import vsquality.complaints.model._
import vsquality.complaints.storage.{ComplaintsDao, QueueDao}
import vsquality.complaints.storage.postgresql.{PgAnalyticsDao, PgBlacklistDao, PgComplaintsDao, PgQueueDao}
import zio.magic._
import doobie.util.transactor.Transactor
import common.zio.doobie.syntax._
import common.zio.logging.Logging
import vsquality.complaints.logic.BlacklistManager.BlacklistManager
import vsquality.complaints.logic.ComplaintsManager.ComplaintsManager
import vsquality.complaints.storage.ComplaintsDao.ComplaintsDao
import vsquality.complaints.storage.QueueDao.QueueDao

import java.time.Instant
import java.time.temporal.ChronoUnit

object ComplaintsManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    (suite("DefaultComplaintsManager")(
      testM("create complaint: visible instance exists in moderation") {
        for {
          manager       <- ZIO.service[ComplaintsManager.Service]
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("1", ExistsInModId, "1", "1")
          context   = Context("1", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          _           <- manager.createComplaintWithContext(complaint, context)
          _           <- manager.reprocess(complaint)
          complaintDb <- complaintsDao.get("1").transactIO
          record      <- queueDao.getRecord(ExistsInModId, complaint.domain).transactIO
        } yield assertTrue(complaint == complaintDb && record.status == Status.IN_PROGRESS)
      },
      testM("create complaint: instance is not in moderation") {
        for {
          manager       <- ZIO.service[ComplaintsManager.Service]
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("2", EmptyInModId, EmptyInModId, "2")
          context   = Context("2", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          _           <- manager.createComplaintWithContext(complaint, context)
          _           <- manager.reprocess(complaint)
          complaintDb <- complaintsDao.get("2").transactIO
          record      <- queueDao.getRecord(EmptyInModId, complaint.domain).transactIO
        } yield assertTrue(complaint == complaintDb && record.status == Status.AWAIT)
      },
      testM("create complaint: non-visible instance is in moderation") {
        for {
          manager       <- ZIO.service[ComplaintsManager.Service]
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("3", NonVisible, NonVisible, "3")
          context   = Context("3", None, Some(Application.IOS_APP), Some(""), isAuthorizedUser = Some(true))
          _           <- manager.createComplaintWithContext(complaint, context)
          _           <- manager.reprocess(complaint)
          complaintDb <- complaintsDao.get("3").transactIO
          isInQueue   <- queueDao.isOfferInQueue(NonVisible, complaint.domain).transactIO
        } yield assertTrue(complaint == complaintDb && !isInQueue)
      },
      testM("create complaint: realty instance with failed opinion is in moderation") {
        for {
          manager       <- ZIO.service[ComplaintsManager.Service]
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("4", FailedOpinion, FailedOpinion, "4").copy(domain = Domain.REALTY)
          context   = Context("4", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          _           <- manager.createComplaintWithContext(complaint, context)
          _           <- manager.reprocess(complaint)
          complaintDb <- complaintsDao.get("4").transactIO
          isInQueue   <- queueDao.isOfferInQueue(FailedOpinion, complaint.domain).transactIO
        } yield assertTrue(complaint == complaintDb && !isInQueue)
      },
      testM("create complaint from banned user without processing it") {
        for {
          manager       <- ZIO.service[ComplaintsManager.Service]
          complaintsDao <- ZIO.service[ComplaintsDao.Service]
          queueDao      <- ZIO.service[QueueDao.Service]
          blackList     <- ZIO.service[BlacklistManager.Service]
          complaint = createComplaint("5", ExistsInModId, ExistsInModId, "5")
          _ <- blackList.banUsers(List((complaint.complainant.id, complaint.domain)))
          context = Context("5", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          _           <- manager.createComplaintWithContext(complaint, context)
          _           <- manager.reprocess(complaint)
          complaintDb <- complaintsDao.get("5").transactIO
          isInQueue   <- queueDao.isOfferInQueue(ExistsInModId, complaint.domain).transactIO
        } yield assertTrue(complaint == complaintDb && !isInQueue)
      },
      testM("do not process other complaints for same offer if it's in queue") {
        for {
          manager  <- ZIO.service[ComplaintsManager.Service]
          queueDao <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("6", ExistsInModId, ExistsInModId, "6")
          context   = Context("6", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          _ <- queueDao.insert(toRecord(complaint)).transactIO
          secondComplaint = complaint.copy(id = "7")
          _      <- manager.createComplaintWithContext(secondComplaint, context)
          _      <- manager.reprocess(complaint)
          record <- queueDao.getRecord(ExistsInModId, complaint.domain).transactIO
        } yield assertTrue(
          record.id == complaint.id
            && record.status == Status.IN_PROGRESS
        )
      },
      testM(
        "process accumulated complaint with enough count, but do not process with enough count if already in process "
      ) {
        for {
          manager  <- ZIO.service[ComplaintsManager.Service]
          queueDao <- ZIO.service[QueueDao.Service]
          complaint = createComplaint("8", ExistsInModId, ExistsInModId, "8").copy(reasons = List(Reason.UserReseller))
          context   = Context("8", None, Some(Application.IOS_APP), None, isAuthorizedUser = Some(true))
          secondComplaint = complaint
            .copy(id = "9", createdAt = Instant.now.plusSeconds(60).truncatedTo(ChronoUnit.SECONDS))
          thirdComplaint = complaint
            .copy(id = "10", createdAt = Instant.now.plusSeconds(120).truncatedTo(ChronoUnit.SECONDS))
          fourthComplaint = complaint
            .copy(id = "11", createdAt = Instant.now.plusSeconds(120).truncatedTo(ChronoUnit.SECONDS))
          _               <- manager.createComplaintWithContext(complaint, context)
          _               <- manager.reprocess(complaint)
          isFirstInQueue  <- queueDao.isOfferInQueue(ExistsInModId, complaint.domain).transactIO
          _               <- manager.createComplaintWithContext(secondComplaint, context)
          _               <- manager.reprocess(secondComplaint)
          second          <- queueDao.getRecord(ExistsInModId, secondComplaint.domain).transactIO
          _               <- manager.createComplaintWithContext(thirdComplaint, context)
          _               <- manager.reprocess(thirdComplaint)
          isThirdInQueue  <- queueDao.isOfferInQueue(ExistsInModId, thirdComplaint.domain).transactIO
          _               <- manager.createComplaintWithContext(fourthComplaint, context)
          _               <- manager.reprocess(fourthComplaint)
          isFourthInQueue <- queueDao.isOfferInQueue(ExistsInModId, fourthComplaint.domain).transactIO

        } yield assertTrue(
          !isFirstInQueue && second.status == Status.IN_PROGRESS && !isThirdInQueue && !isFourthInQueue
        )
      }
    ) @@ sequential)
      .provideCustomLayer {

        ZLayer.fromSomeMagic[
          _root_.zio.test.environment.TestEnvironment,
          BlacklistManager with ComplaintsDao with QueueDao with Has[
            Transactor[Task]
          ] with ComplaintsManager
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
          ComplaintsManager.live
        )

      }
  }
}

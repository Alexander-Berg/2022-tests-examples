package vsquality.complaints.storage.test

import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.TestAspect._
import zio.ZIO
import common.zio.doobie.testkit.TestPostgresql
import vsquality.complaints.storage.ComplaintsDao
import vsquality.complaints.model._
import vsquality.complaints.model.types.{OfferId, UserId}
import vsquality.complaints.storage.postgresql.PgComplaintsDao
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Random
import common.zio.doobie.syntax._

object PgComplaintsDaoSpec extends DefaultRunnableSpec {

  def createComplaint(offerId: OfferId, offerOwnerId: UserId, complainantId: UserId) =
    Complaint(
      id = Random.nextLong().toString,
      offerId = offerId,
      offerOwner = User(offerOwnerId, UserType.Regular),
      complainant = User(complainantId, UserType.Regular),
      reasons = List(Reason.AbusiveCommunication),
      source = Source.Description,
      resolution = Resolution(confirmed = Some(true), false),
      comment = "ban them all",
      domain = Domain.REALTY,
      createdAt = Instant.now.truncatedTo(ChronoUnit.SECONDS),
      updatedAt = Instant.now.truncatedTo(ChronoUnit.SECONDS),
      hoboKey = None
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgComplaintDao")(
      testM("create complaint") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          c1 = createComplaint("1", "1", "1")
          count <- dao.upsert(Seq(c1)).transactIO
          res   <- dao.get(c1.id).transactIO
        } yield assertTrue(count == 1 && res == c1)
      },
      testM("stream offer complaints") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          ls  <- dao.getOfferComplaints("111", Domain.REALTY).runCollect
        } yield assertTrue(ls.length == 1000)
      },
      testM("stream offer owner complaints") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          ls  <- dao.getOffersOwnerComplaints("333", Domain.REALTY).runCollect
        } yield assertTrue(ls.length == 1000)
      },
      testM("stream complainant appeals") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          ls  <- dao.getComplainantAppeals("222", Domain.REALTY).runCollect
        } yield assertTrue(ls.length == 1000)
      },
      testM("update multiple complaints") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          complaints = (1 to 5).map(_ => createComplaint("111", "333", "222"))
          rows <- dao.upsert(complaints).transactIO
        } yield assertTrue(rows == 5)
      },
      testM("get potential complaints heirs") {
        for {
          dao <- ZIO.service[ComplaintsDao.Service]
          now = Instant.now()
          complaint1 = createComplaint("5", "333", "222")
            .copy(createdAt = now.minus(25, ChronoUnit.HOURS), resolution = Resolution(None, false))
          complaint2 = complaint1.copy(id = "1", createdAt = now.minus(5, ChronoUnit.HOURS)) // heir
          complaint3 = complaint1.copy(id = "2", createdAt = now)
          complaint4 = complaint2.copy(id = "3", resolution = Resolution(Some(true), false))
          complaint5 = complaint2.copy(id = "4", createdAt = now.minus(23, ChronoUnit.HOURS)) // heir
          _     <- dao.upsert(Seq(complaint1, complaint2, complaint3, complaint4, complaint5)).transactIO
          heirs <- dao.getPotentialHeirs(complaint3).transactIO
        } yield assertTrue(heirs.length == 2)
      }
    ) @@ sequential @@ beforeAll {
      for {
        dao <- ZIO.service[ComplaintsDao.Service]
        complaints = (1 to 1000).map(_ => createComplaint("111", "333", "222"))
        _ <- dao.upsert(complaints).transactIO.ignore
      } yield ()
    })
      .provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgComplaintsDao.live)
  }
}

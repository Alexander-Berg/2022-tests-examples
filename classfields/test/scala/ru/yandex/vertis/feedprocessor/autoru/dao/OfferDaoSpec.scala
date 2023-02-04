package ru.yandex.vertis.feedprocessor.autoru.dao

import java.sql._
import java.time.LocalDateTime
import org.scalatest.{BeforeAndAfter, Inside}
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.support.GeneratedKeyHolder
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.dao.OfferDao.{Offer, OfferError, OfferFilter}
import ru.yandex.vertis.feedprocessor.autoru.utils.OpsJdbc
import ru.yandex.vertis.feedprocessor.models.Paging
import ru.yandex.vertis.feedprocessor.util.{DatabaseSpec, DummyOpsSupport}

import scala.jdk.CollectionConverters._

/**
  * @author pnaydenov
  */
class OfferDaoSpec extends WordSpecBase with DatabaseSpec with BeforeAndAfter with Inside with DummyOpsSupport {
  implicit val opsJdbcMeters: OpsJdbc.Meters = new OpsJdbc.Meters(operationalSupport.prometheusRegistry)
  val offerDao: OfferDao = new OfferDaoImpl(tasksDb)

  before {
    tasksDb.master.jdbc.update("TRUNCATE offers")
    tasksDb.master.jdbc.update("TRUNCATE offer_errors")
  }

  private def createOffer(
      position: Int,
      feedId: String,
      taskId: Long,
      clientId: Long,
      status: Offer.Status.Value,
      now: LocalDateTime,
      offerId: Option[String] = None,
      feedprocessorId: Option[String] = None,
      vin: Option[String] = None,
      uniqueId: Option[String] = None): Long = {
    val createdAtTimestamp = Timestamp.valueOf(now)
    val sql =
      """
        INSERT INTO offers (
          position,
          feed_id,
          task_id,
          client_id,
          status,
          offer_id,
          feedprocessor_id,
          vin,
          unique_id,
          created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """
    val keyHolder = new GeneratedKeyHolder()
    tasksDb.master.jdbc.update(
      new PreparedStatementCreator {
        override def createPreparedStatement(con: Connection) = {
          val ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
          ps.setInt(1, Int.box(position))
          ps.setString(2, feedId)
          ps.setLong(3, taskId)
          ps.setLong(4, clientId)
          ps.setString(5, status.toString)
          offerId.fold(ps.setNull(6, Types.VARCHAR))(ps.setString(6, _))
          feedprocessorId.fold(ps.setNull(7, Types.VARCHAR))(ps.setString(7, _))
          vin.fold(ps.setNull(8, Types.VARCHAR))(ps.setString(8, _))
          uniqueId.fold(ps.setNull(9, Types.VARCHAR))(ps.setString(9, _))
          ps.setTimestamp(10, createdAtTimestamp)
          ps
        }
      },
      keyHolder
    )
    val key = keyHolder.getKey
    if (key eq null) throw new RuntimeException("Insert must affects one row")
    else key.longValue()
  }

  private def createOfferError(
      offerId: Long,
      `type`: OfferError.ErrorType.Value,
      message: String,
      now: LocalDateTime,
      context: Option[String] = None,
      columnName: Option[String] = None,
      originalValue: Option[String] = None): Long = {
    val createdAtTimestamp = Timestamp.valueOf(now)
    val sql =
      """
        INSERT INTO offer_errors (
          offer_id,
          type,
          message,
          context,
          column_name,
          original_value,
          created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      """
    val keyHolder = new GeneratedKeyHolder()
    tasksDb.master.jdbc.update(
      new PreparedStatementCreator {
        override def createPreparedStatement(con: Connection) = {
          val ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
          ps.setLong(1, Long.box(offerId))
          ps.setString(2, `type`.toString)
          ps.setString(3, message)
          context.fold(ps.setNull(4, Types.VARCHAR))(ps.setString(4, _))
          columnName.fold(ps.setNull(5, Types.VARCHAR))(ps.setString(5, _))
          originalValue.fold(ps.setNull(6, Types.VARCHAR))(ps.setString(6, _))
          ps.setTimestamp(7, createdAtTimestamp)
          ps
        }
      },
      keyHolder
    )
    val key = keyHolder.getKey
    if (key eq null) throw new RuntimeException("Insert must affects one row")
    else key.longValue()
  }

  "OfferDao" should {
    "findFirstOfferIdByTaskId" in {
      val o0 = createOffer(0, "", 1, 1, OfferDao.Offer.Status.Update, LocalDateTime.now())

      offerDao.findFirstOfferIdByTaskId(1).get shouldEqual o0
      offerDao.findFirstOfferIdByTaskId(5555) shouldBe empty
    }

    "findByTaskIdWithErrors" in {
      val expectedOffer = Offer(
        position = Some(1),
        feedId = "test_feed_id",
        taskId = 1L,
        clientId = 2,
        status = Offer.Status.Error,
        offerId = None,
        feedprocessorId = None,
        errorMessage = None,
        vin = None,
        uniqueId = None
      )

      val offerIdRes = {
        import expectedOffer._
        createOffer(position.get, feedId, taskId, clientId, status, LocalDateTime.now())
      }

      val expectedError = OfferError(
        offerId = offerIdRes,
        `type` = OfferDao.OfferError.ErrorType.Error,
        message = "foo",
        context = None,
        columnName = None,
        originalValue = None
      )

      {
        import expectedError._
        createOfferError(offerId, `type`, expectedError.message, LocalDateTime.now())
      }

      val errorFilter = {
        import OfferFilter._
        OfferFilter(Seq(None, Error, Notice))
      }
      val res = offerDao.findByTaskIdWithErrors(expectedOffer.taskId, Paging(page = 1, pageSize = 10), errorFilter)

      res.head shouldBe ((expectedOffer, Seq(expectedError)))
    }

    "deleteOffersOlderThanTaskId" in {
      val o2 = createOffer(0, "", 2, 2, OfferDao.Offer.Status.Update, LocalDateTime.now())
      val o3 = createOffer(0, "", 3, 3, OfferDao.Offer.Status.Update, LocalDateTime.now())

      offerDao.deleteOffersOlderThanTaskId(2)

      tasksDb.master.jdbc.queryForList("SELECT id FROM offers", classOf[Long]).asScala.toSet shouldEqual Set(o2, o3)
    }

    "deleteOfferErrorsOlderThanOfferId" in {
      val e2 = createOfferError(2, OfferDao.OfferError.ErrorType.Error, "foo", LocalDateTime.now())
      val e3 = createOfferError(3, OfferDao.OfferError.ErrorType.Error, "foo", LocalDateTime.now())

      offerDao.deleteOfferErrorsOlderThanOfferId(2)

      tasksDb.master.jdbc.queryForList("SELECT id FROM offer_errors", classOf[Long]).asScala.toSet shouldEqual Set(
        e2,
        e3
      )
    }
  }
}

package ru.auto.salesman.dao.jdbc.notification

import doobie.implicits._
import zio.duration.{Duration => ZIODuration}
import scala.concurrent.duration._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.scalacheck.Gen
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor._
import ru.auto.salesman.dao.impl.jdbc.notification.JdbcProlongationFailedNotificationDao
import ru.auto.salesman.model.notification.ProlongationFailedNotification
import ru.auto.salesman.model.notification.ProlongationFailedNotification.ProlongationFailedNotificationStatus
import ru.auto.salesman.model.user.ProductType
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class JdbcProlongationFailedNotificationDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate {

  def dao = new JdbcProlongationFailedNotificationDao(transactor, transactor)
  val dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")

  "JdbcProlongationFailedNotificationDao" should {
    "save notification to database" in {
      cleanDatabase()
      dao
        .insert(
          productType = ProductType.Goods,
          productId = "10"
        )
        .success
        .value

      val resultInSelect = dao
        .getUnProcessed(
          limit = 10,
          processingInterval = ZIODuration.fromMillis(System.currentTimeMillis())
        )
        .success
        .value

      resultInSelect.size shouldBe 1

      resultInSelect.head.productId shouldBe "10"
      resultInSelect.head.productType shouldBe ProductType.Goods
      resultInSelect.head.status shouldBe ProlongationFailedNotificationStatus.UnProcessed
      resultInSelect.head.details shouldBe None

    }

    "read notification from database included to the interval" in {
      forAll(Gen.listOfN(10, recordGeneration()), minSuccessful(1)) { generatedRecords =>
        val givenRecords =
          (1 to generatedRecords.size)
            .zip(generatedRecords)
            .map { case (index, record) =>
              if (index % 2 == 0)
                record.copy(
                  id = index,
                  status = ProlongationFailedNotificationStatus.UnProcessed,
                  epoch = (new DateTime()).minusHours(5)
                )
              else
                record.copy(
                  id = index,
                  status = ProlongationFailedNotificationStatus.UnProcessed,
                  epoch = (new DateTime())
                )
            }
        givenRecords.foreach(insertProlongationFailedNotification)
        val processedNotification = dao
          .getUnProcessed(
            limit = 10,
            processingInterval = ZIODuration.fromScala(3.hours)
          )
          .success
          .value
        processedNotification.size shouldBe 5
        processedNotification shouldBe givenRecords
          .filter(
            _.epoch.getMillis > (new DateTime()).minusHours(4).getMillis
          )
          .toList

      }
    }

    "read only unprocessed notification from database" in {
      forAll(Gen.listOfN(10, recordGeneration()), minSuccessful(1)) { generatedRecords =>
        cleanDatabase()

        val givenRecords =
          (1 to generatedRecords.size)
            .zip(generatedRecords)
            .map { case (index, record) =>
              if (index % 2 == 0)
                record.copy(
                  id = index,
                  status = ProlongationFailedNotificationStatus.UnProcessed
                )
              else
                record.copy(
                  id = index,
                  status = ProlongationFailedNotificationStatus.Processed
                )
            }

        givenRecords.foreach(insertProlongationFailedNotification)

        val unProcessedNotification = dao
          .getUnProcessed(
            limit = 10,
            processingInterval = ZIODuration.fromMillis(System.currentTimeMillis())
          )
          .success
          .value
        unProcessedNotification.size shouldBe 5
        unProcessedNotification shouldBe givenRecords
          .filter(
            _.status == ProlongationFailedNotificationStatus.UnProcessed
          )
          .toList

      }
    }
  }

  private def cleanDatabase(): Unit = {
    val deleteSql =
      sql"""
        delete from prolongation_failed_notification;
      """

    deleteSql.update.run
      .transact(transactor)
      .unit
      .success
      .value
  }

  def insertProlongationFailedNotification(
      record: ProlongationFailedNotification
  ) = {

    val insertSQL = sql"""
         INSERT INTO prolongation_failed_notification(id, product_type, product_id, 
            status, details,
            epoch,
            last_update)
         VALUES(${record.id}, ${record.productType.entryName}, ${record.productId},
          ${record.status.entryName}, ${record.details}, 
          ${dateTimeFormatter.print(record.epoch)},
          ${dateTimeFormatter.print(record.lastUpdate)}
          );
       """
    insertSQL.update.run
      .transact(transactor)
      .unit
      .success
      .value
  }

  def recordGeneration(): Gen[ProlongationFailedNotification] =
    for {
      productType <- Gen.oneOf(
        ProductType.values
      )
      productId <- Gen.uuid
      details <- Gen.option(Gen.alphaStr)
      epoch <- Gen.chooseNum(10000L, 1000000L)
      lastUpdate <- Gen.chooseNum(10000L, 1000000L)
    } yield
      ProlongationFailedNotification(
        id = 0L,
        productType = productType,
        productId = productId.toString.replace("-", ""),
        status = ProlongationFailedNotificationStatus.Processed,
        details = details,
        epoch = new DateTime(epoch),
        lastUpdate = new DateTime(lastUpdate)
      )
}

package ru.auto.salesman.tasks

import cakesolutions.kafka.KafkaProducerLike
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.scalacheck.Gen
import org.scalatest.Inspectors
import ru.auto.salesman.dao.ClientDao.ForIdWithDeleted
import ru.auto.salesman.dao.ClientsChangedBufferDao.DataSourceFilter
import ru.auto.salesman.dao.{ClientDao, ClientsChangedBufferDao, LoyaltyReportDao}
import ru.auto.salesman.model.{
  AdsRequestTypes,
  CityId,
  Client,
  ClientChangedBufferRecord,
  ClientStatuses,
  RegionId
}
import ru.auto.salesman.model.cashback.ApiModel.LoyaltyReport
import ru.auto.salesman.tasks.LoyaltyStatusUpdatesTask.TaskSummaryException
import ru.auto.salesman.tasks.LoyaltyStatusUpdatesTaskSpec._
import ru.auto.salesman.test.BaseSpec

class LoyaltyStatusUpdatesTaskSpec extends BaseSpec {

  private val clientChangedDao = mock[ClientsChangedBufferDao]
  private val clientDao = mock[ClientDao]
  private val loyaltyReportDao = mock[LoyaltyReportDao]
  private val kafkaProducer = mock[KafkaProducerLike[String, Array[Byte]]]

  private val task =
    new LoyaltyStatusUpdatesTask(
      clientChangedDao,
      clientDao,
      loyaltyReportDao,
      TestTopic,
      kafkaProducer
    )

  private val changedRecordGen =
    Gen
      .zip(Gen.posNum[Long], Gen.alphaStr)
      .map { case (id, source) =>
        ClientChangedBufferRecord(
          id,
          id,
          source
        )
      }

  private def getClientsResponse(clientId: Long) = List(
    Client(
      clientId,
      None,
      Some(15697L),
      None,
      RegionId(1L),
      CityId(21735L),
      ClientStatuses.Active,
      Set(AdsRequestTypes.CarsUsed),
      firstModerated = false,
      paidCallsAvailable = true,
      priorityPlacement = true
    )
  )

  "LoyaltyStatusUpdatesTask" when {

    "executes" should {

      "do nothing if there are no changes" in {
        (clientChangedDao.get _)
          .expects(*)
          .returningZ(Nil)

        task
          .execute()
          .success
      }

      "complete successfully" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(DataSourceFilter(Set("dealer_pony")))
            .returningZ(records)

          Inspectors.forEvery(records) { record =>
            (clientDao.get _)
              .expects(ForIdWithDeleted(record.clientId))
              .returningZ(getClientsResponse(record.clientId))
            (loyaltyReportDao.findCurrent _)
              .expects(record.clientId)
              .returningZ(
                Some(LoyaltyReport.newBuilder().setResolution(true).build())
              )
            (kafkaProducer.send _)
              .expects(*)
              .returningF(TestMeta)
            (clientChangedDao.delete _)
              .expects(record.clientId)
              .returningZ(())
          }

          task
            .execute()
            .success
        }
      }

      "fail if it cannot get client info from dao; process remaining items" in {
        forAll(Gen.listOfN(5, changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(DataSourceFilter(Set("dealer_pony")))
            .returningZ(records)

          val badClient = records.head
          val badReport = records.last
          val correctRecord = records(1)

          (clientDao.get _)
            .expects(ForIdWithDeleted(badClient.clientId))
            .returningZ(Nil)

          (clientDao.get _)
            .expects(*)
            .returningZ(getClientsResponse(correctRecord.clientId))
            .repeat(4)

          (loyaltyReportDao.findCurrent _)
            .expects(badReport.clientId)
            .returningZ(None)

          (loyaltyReportDao.findCurrent _)
            .expects(*)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
            .repeat(3)

          (kafkaProducer.send _)
            .expects(*)
            .returningF(TestMeta)
            .repeat(3)

          (clientChangedDao.delete _)
            .expects(*)
            .returningZ(())
            .repeat(3)

          task
            .execute()
            .failure
            .exception
            .getMessage shouldBe
            s"""
               |Not found clients: [${badClient.clientId}]
               |Not fount reports for clients: [${badReport.clientId}]
               |Other exceptions(first 3): []
               |""".stripMargin
        }
      }

      "fail if it can not get changes from dao" in {
        (clientChangedDao.get _)
          .expects(DataSourceFilter(Set("dealer_pony")))
          .throwingZ(DaoReadingError)

        task
          .execute()
          .failure
          .exception shouldBe DaoReadingError
      }

      "fail if it can not send data to kafka" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(DataSourceFilter(Set("dealer_pony")))
            .returningZ(records)

          (clientDao.get _)
            .expects(*)
            .returningZ(getClientsResponse(1L))
            .anyNumberOfTimes()

          (loyaltyReportDao.findCurrent _)
            .expects(*)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
            .anyNumberOfTimes()

          (kafkaProducer.send _)
            .expects(*)
            .throwingF(KafkaSendingError)
            .anyNumberOfTimes()

          task
            .execute()
            .failure
            .exception
            .getMessage shouldBe TaskSummaryException(
            Nil,
            Nil,
            Seq(KafkaSendingError.getMessage)
          ).getMessage
        }
      }

      "fail if it can't delete client id in dao after sending the data" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(DataSourceFilter(Set("dealer_pony")))
            .returningZ(records)

          (clientDao.get _)
            .expects(*)
            .returningZ(getClientsResponse(1L))
            .anyNumberOfTimes()

          (loyaltyReportDao.findCurrent _)
            .expects(*)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
            .anyNumberOfTimes()

          (kafkaProducer.send _)
            .expects(*)
            .returningF(TestMeta)
            .anyNumberOfTimes()

          (clientChangedDao.delete _)
            .expects(*)
            .throwingZ(DaoDeletingError)
            .anyNumberOfTimes()

          task
            .execute()
            .failure
            .exception
            .getMessage shouldBe TaskSummaryException(
            Nil,
            Nil,
            Seq(DaoDeletingError.getMessage)
          ).getMessage
        }
      }
    }
  }

}

object LoyaltyStatusUpdatesTaskSpec {

  private val TestTopic = "loyalty-updates-log"

  private val TestTopicPartition =
    new TopicPartition(TestTopic, 1)

  private val TestMeta =
    new RecordMetadata(TestTopicPartition, 0L, 0L, 0L, 0L, 0, 0)

  object DaoReadingError extends Exception("dao read error")
  object DaoDeletingError extends Exception("dao delete error")
  object KafkaSendingError extends Exception("kafka sending error")
}

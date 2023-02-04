package ru.auto.salesman.tasks

import cakesolutions.kafka.KafkaProducerLike
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.scalacheck.Gen
import org.scalatest.Inspectors
import ru.auto.amoyak.internal_service_model.AmoyakDto
import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao.commonFilter
import ru.auto.salesman.dao.{ClientsChangedBufferDao, LoyaltyReportDao}
import ru.auto.salesman.model.ClientChangedBufferRecord
import ru.auto.salesman.model.TariffText.CarsNew
import ru.auto.salesman.model.cashback.ApiModel.LoyaltyReport
import ru.auto.salesman.service.ClientTariffsTextService
import ru.auto.salesman.tasks.AmoyakDealerUpdatesTaskSpec._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.feature.TestDealerFeatureService

class AmoyakDealerUpdatesTaskSpec extends BaseSpec {

  private val clientChangedDao = mock[ClientsChangedBufferDao]
  private val clientTariffsTextService = mock[ClientTariffsTextService]
  private val loyaltyReportDao = mock[LoyaltyReportDao]
  private val kafkaProducer = mock[KafkaProducerLike[String, Array[Byte]]]

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

  private def amoyakDto(check: AmoyakDto => Boolean) =
    where((p: ProducerRecord[String, Array[Byte]]) =>
      check(AmoyakDto.parseFrom(p.value()))
    )

  "AmoyakDealerUpdatesTask" when {

    "executes with separate loyalty message" should {

      val task =
        new AmoyakDealerUpdatesTask(
          clientChangedDao,
          clientTariffsTextService,
          loyaltyReportDao,
          TestTopic,
          kafkaProducer,
          TestDealerFeatureService(sendSeparateLoyaltyMessageToAmo = true)
        )

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
            .expects(commonFilter)
            .returningZ(records)

          Inspectors.forEvery(records) { record =>
            (clientTariffsTextService.getTariffsText _)
              .expects(record.clientId)
              .returningZ(Set(CarsNew))
            (loyaltyReportDao.findCurrent _)
              .expects(record.clientId)
              .returningZ(
                Some(LoyaltyReport.newBuilder().setResolution(true).build())
              )
            (kafkaProducer.send _)
              .expects(amoyakDto(_.payload.isLoyaltyData))
              .returningF(TestMeta)
            (kafkaProducer.send _)
              .expects(amoyakDto(_.payload.isSalesmanData))
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

      "fail if it can not get changes from dao" in {
        (clientChangedDao.get _)
          .expects(commonFilter)
          .throwingZ(DaoReadingError)

        task
          .execute()
          .failure
          .exception shouldBe DaoReadingError
      }

      "fail if it can not send data to kafka" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(commonFilter)
            .returningZ(records)

          val record = records.head

          (clientTariffsTextService.getTariffsText _)
            .expects(record.clientId)
            .returningZ(Set(CarsNew))
          (loyaltyReportDao.findCurrent _)
            .expects(record.clientId)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
          (kafkaProducer.send _)
            .expects(*)
            .throwingF(KafkaSendingError)
            .noMoreThanTwice()

          task
            .execute()
            .failure
            .exception shouldBe KafkaSendingError
        }
      }

      "fail if it can't delete client id in dao after sending the data" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(commonFilter)
            .returningZ(records)

          val record = records.head

          (clientTariffsTextService.getTariffsText _)
            .expects(record.clientId)
            .returningZ(Set(CarsNew))
          (loyaltyReportDao.findCurrent _)
            .expects(record.clientId)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
          (kafkaProducer.send _)
            .expects(amoyakDto(_.payload.isLoyaltyData))
            .returningF(TestMeta)
          (kafkaProducer.send _)
            .expects(amoyakDto(_.payload.isSalesmanData))
            .returningF(TestMeta)
          (clientChangedDao.delete _)
            .expects(record.clientId)
            .throwingZ(DaoDeletingError)

          task
            .execute()
            .failure
            .exception shouldBe DaoDeletingError
        }
      }
    }

    "executes with single amoyak message" should {

      val task =
        new AmoyakDealerUpdatesTask(
          clientChangedDao,
          clientTariffsTextService,
          loyaltyReportDao,
          TestTopic,
          kafkaProducer,
          TestDealerFeatureService()
        )

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
            .expects(commonFilter)
            .returningZ(records)

          Inspectors.forEvery(records) { record =>
            (clientTariffsTextService.getTariffsText _)
              .expects(record.clientId)
              .returningZ(Set(CarsNew))
            (loyaltyReportDao.findCurrent _)
              .expects(record.clientId)
              .returningZ(
                Some(LoyaltyReport.newBuilder().setResolution(true).build())
              )
            (kafkaProducer.send _)
              .expects(amoyakDto(_.payload.isSalesmanData))
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

      "fail if it can not get changes from dao" in {
        (clientChangedDao.get _)
          .expects(commonFilter)
          .throwingZ(DaoReadingError)

        task
          .execute()
          .failure
          .exception shouldBe DaoReadingError
      }

      "fail if it can not send data to kafka" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(commonFilter)
            .returningZ(records)

          val record = records.head

          (clientTariffsTextService.getTariffsText _)
            .expects(record.clientId)
            .returningZ(Set(CarsNew))
          (loyaltyReportDao.findCurrent _)
            .expects(record.clientId)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
          (kafkaProducer.send _)
            .expects(*)
            .throwingF(KafkaSendingError)
            .noMoreThanTwice()

          task
            .execute()
            .failure
            .exception shouldBe KafkaSendingError
        }
      }

      "fail if it can't delete client id in dao after sending the data" in {
        forAll(Gen.nonEmptyListOf(changedRecordGen)) { records =>
          (clientChangedDao.get _)
            .expects(commonFilter)
            .returningZ(records)

          val record = records.head

          (clientTariffsTextService.getTariffsText _)
            .expects(record.clientId)
            .returningZ(Set(CarsNew))
          (loyaltyReportDao.findCurrent _)
            .expects(record.clientId)
            .returningZ(
              Some(LoyaltyReport.newBuilder().setResolution(true).build())
            )
          (kafkaProducer.send _)
            .expects(amoyakDto(_.payload.isSalesmanData))
            .returningF(TestMeta)
          (clientChangedDao.delete _)
            .expects(record.clientId)
            .throwingZ(DaoDeletingError)

          task
            .execute()
            .failure
            .exception shouldBe DaoDeletingError
        }
      }
    }
  }

}

object AmoyakDealerUpdatesTaskSpec {

  private val TestTopic = "amoyak-updates-test"

  private val TestTopicPartition =
    new TopicPartition(TestTopic, 1)

  private val TestMeta =
    new RecordMetadata(TestTopicPartition, 0L, 0L, 0L, 0L, 0, 0)

  object DaoReadingError extends Exception
  object DaoDeletingError extends Exception
  object KafkaSendingError extends Exception
}

package ru.auto.cabinet.tasks.impl.amoyak

import cats.data.NonEmptyList
import cats.syntax.option._
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import ru.auto.amoyak.CommonServiceModel.CustomerType
import ru.auto.amoyak.InternalServiceModel.AmoyakDto
import ru.auto.cabinet.dao.jdbc.{
  ClientsChangedBufferDao,
  CompanyDao,
  JdbcClientDao,
  JdbcPoiDataDao
}
import ru.auto.cabinet.environment
import ru.auto.cabinet.kafka.cakesolutions.kafka.KafkaProducerLike
import ru.auto.cabinet.model._
import ru.auto.cabinet.service.amoyak.AmoyakDtoEnricher
import ru.auto.cabinet.tasks.impl.amoyak.AmoyakClientUpdatesTask._
import ru.auto.cabinet.tasks.impl.amoyak.AmoyakClientUpdatesTaskSpec._
import ru.auto.cabinet.test.BaseSpec
import ru.auto.cabinet.trace.Context

import java.time.OffsetDateTime

class AmoyakClientUpdatesTaskSpec extends BaseSpec {
  implicit private val rc = Context.unknown

  private val clientDao = mock[JdbcClientDao]
  private val poiDao = mock[JdbcPoiDataDao]
  private val companyDao = mock[CompanyDao]
  private val clientChangedBufferDao = mock[ClientsChangedBufferDao]
  private val dtoConverter = mock[AmoyakDtoEnricher]

  private val kafkaProducer =
    mock[KafkaProducerLike[java.lang.Long, Array[Byte]]]

  private val task = new AmoyakClientUpdatesTask(
    clientDao,
    poiDao,
    companyDao,
    clientChangedBufferDao,
    dtoConverter,
    kafkaTopic,
    kafkaProducer
  )

  "AmoyakClientUpdatesTask" should {
    val changed = Seq(testClientChangedRecord)
    val changedNonEmpty = NonEmptyList.fromListUnsafe(changed.toList)

    "execute normally and send AmoyakDto to kafka for clients" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .returningF(List(testDetailedClient))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List.empty, *)
        .returningF(List.empty)

      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(testDetailedClient, *)
        .returningF(testAmoyakDto)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testRecordId, *)
        .returningF(())

      task.execute(rc).futureValue shouldBe ()
    }

    "execute normally and send AmoyakDto to kafka for companies" in {
      val changed = Seq(testCompanyChangedRecord)

      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(changed.map(_.clientId), *)
        .returningF(List(testCompany))

      (dtoConverter
        .toAmoyakDto(_: Company)(_: Context))
        .expects(testCompany, *)
        .returningF(testCompanyAmoyakDto)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testCompanyRecordId, *)
        .returningF(())

      task.execute(rc).futureValue shouldBe ()
    }

    "execute normally and send AmoyakDto to kafka for both clients and companies" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed :+ testCompanyChangedRecord)

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(
          NonEmptyList
            .fromListUnsafe(List(testClientChangedRecord))
            .map(_.clientId),
          *)
        .returningF(List(testDetailedClient))

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List(testCompanyChangedRecord).map(_.clientId), *)
        .returningF(List(testCompany))

      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(testDetailedClient, *)
        .returningF(testAmoyakDto)

      (dtoConverter
        .toAmoyakDto(_: Company)(_: Context))
        .expects(testCompany, *)
        .returningF(testCompanyAmoyakDto)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testRecordId, *)
        .returningF(())

      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testCompanyRecordId, *)
        .returningF(())

      task.execute(rc).futureValue shouldBe ()
    }

    "execute and send AmoyakDto to kafka for empty poi agents" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(Nil)

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .returningF(List(testDetailedClient))

      val detailedClientNoPoiFields = testDetailedClient.copy(clientProperties =
        testDetailedClient.clientProperties
          .copy(phone = None, website = None))

      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(detailedClientNoPoiFields, *)
        .returningF(testAmoyakDto)

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List.empty, *)
        .returningF(List.empty)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testRecordId, *)
        .returningF(())

      task.execute(rc).futureValue shouldBe ()
    }

    "fail when can not get changed clients" in {
      val testError = TestError("Error while getting changed clients")
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .throwingF(testError)

      task
        .execute(rc)
        .failed
        .futureValue shouldBe testError
    }

    "fail when can not get detailed clients" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      val testError = TestError("Error while getting detailed clients")
      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe testError
    }

    "fail when can not get companies" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(List(testCompanyChangedRecord))

      val testError = TestError("Error while getting companies")
      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List(testCompanyChangedRecord).map(_.clientId), *)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe testError
    }

    "fail when can not create client kafka records" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .returningF(List(testDetailedClient))

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      val testError = TestError("Error while converting to AmoyakDto")
      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(testDetailedClient, *)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe CreateKafkaClientRecordError(
        testId,
        testError)
    }

    "fail when can not create company kafka records" in {
      val changed = Seq(testCompanyChangedRecord)

      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List(testCompanyChangedRecord).map(_.clientId), *)
        .returningF(List(testCompany))

      val testError = TestError("Error while converting to AmoyakDto")
      (dtoConverter
        .toAmoyakDto(_: Company)(_: Context))
        .expects(testCompany, *)
        .throwingF(testError)

      task
        .execute(rc)
        .failed
        .futureValue shouldBe CreateKafkaCompanyRecordError(testId, testError)
    }

    "fail when can not send client kafka records" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .returningF(List(testDetailedClient))

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(testDetailedClient, *)
        .returningF(testAmoyakDto)

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List.empty, *)
        .returningF(List.empty)

      val testError = TestError("Error while sending AmoyakDto to kafka")
      (kafkaProducer.send _)
        .expects(*)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe SendClientToKafkaError(
        testId,
        testError)
    }

    "fail when can not send company kafka records" in {
      val changed = Seq(testCompanyChangedRecord)

      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List(testCompanyChangedRecord).map(_.clientId), *)
        .returningF(List(testCompany))

      (dtoConverter
        .toAmoyakDto(_: Company)(_: Context))
        .expects(testCompany, *)
        .returningF(testCompanyAmoyakDto)

      val testError = TestError("Error while sending AmoyakDto to kafka")
      (kafkaProducer.send _)
        .expects(*)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe SendCompanyToKafkaError(
        testId,
        testError)
    }

    "fail when can not delete changed client records" in {
      (clientChangedBufferDao
        .get()(_: Context))
        .expects(*)
        .returningF(changed)

      (clientDao
        .getDetailedBatch(_: NonEmptyList[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId), *)
        .returningF(List(testDetailedClient))

      (companyDao
        .findMany(_: Seq[Long])(_: Context))
        .expects(List.empty, *)
        .returningF(List.empty)

      (clientDao
        .getClientPoiIds(_: Seq[ClientId])(_: Context))
        .expects(changedNonEmpty.map(_.clientId).toList, *)
        .returningF(List(ClientPoi(testId, testPoiId)))

      (poiDao
        .get(_: NonEmptyList[PoiId])(_: Context))
        .expects(NonEmptyList.one(testPoiId), *)
        .returningF(NonEmptyList.one(testPoiData))

      (poiDao
        .getPhones(_: PoiId)(_: Context))
        .expects(*, *)
        .returningF(List(testPhone))

      (dtoConverter
        .toAmoyakDto(_: DetailedClient)(_: Context))
        .expects(testDetailedClient, *)
        .returningF(testAmoyakDto)

      (kafkaProducer.send _)
        .expects(*)
        .returningF(testMetaData)

      val testError = TestError("Error while deleting changed client records")
      (clientChangedBufferDao
        .delete(_: Long)(_: Context))
        .expects(testRecordId, *)
        .throwingF(testError)

      task.execute(rc).failed.futureValue shouldBe DeleteRecordError(
        testId,
        testError)
    }
  }

}

object AmoyakClientUpdatesTaskSpec {
  val testId = 1L
  val testPoiId = 2L
  val testName = "TestCompany".some
  val testDataSource = "clients"
  val testIsAgent = false
  val testAgencyId = 0L.some
  val testAgencyName = None
  val testCompanyId = None
  val testCompanyName = None
  val testRegionId = 1L
  val testCityId = 11L
  val testOriginId = "kitezh112"
  val testStatus = ClientStatuses.Active
  val testAddress = "333, Test Address st"
  val testWebsite = "website.com"
  val testPhone: PoiPhone = PoiPhone(1, "", 123456789, "", 0, 0)
  val testEmail = "test@test.test"
  val testResponsibleManagerEmail = None
  val testFirstModerationDate = environment.now.minusDays(1)
  val testCreateDate = testFirstModerationDate.minusDays(1)
  val multipostingEnabled = false
  val firstModerated = true

  val testCompanyTitle = "CompanyGroup"

  val testRecordId = 1L
  val testCompanyRecordId = 2L

  val testClientChangedRecord =
    ClientsChangedBufferRecord(testRecordId, testId, testDataSource)

  val testCompanyChangedRecord =
    ClientsChangedBufferRecord(testCompanyRecordId, testId, "companies")

  val testClientProperties =
    ClientProperties(
      testRegionId,
      testCityId,
      testOriginId,
      testStatus,
      environment.now,
      testAddress,
      Some(testWebsite),
      testEmail,
      testResponsibleManagerEmail,
      testFirstModerationDate.some,
      testCreateDate.some,
      multipostingEnabled,
      firstModerated = firstModerated,
      isAgent = testIsAgent,
      phone = Some(testPhone.phone)
    )

  val testDetailedClient =
    DetailedClient(
      testId,
      testName,
      testIsAgent,
      testAgencyId,
      testAgencyName,
      testCompanyId,
      testCompanyName,
      testClientProperties
    )

  val testCompany =
    Company(
      testId,
      testCompanyTitle,
      OffsetDateTime.now()
    )

  val testAmoyakDto = AmoyakDto.getDefaultInstance

  val testCompanyAmoyakDto =
    AmoyakDto.newBuilder().setCustomerType(CustomerType.COMPANY_GROUP).build()

  val testPoiData: PoiData = PoiData(
    testPoiId,
    Location(0, None),
    rating = None,
    properties = Some(PoiProperties("", Some(testWebsite), None)))

  val kafkaTopic = "amoyak-client-updates-task-test-kafka-topic"

  private val testTopicPartition =
    new TopicPartition(kafkaTopic, 1)

  val testMetaData =
    new RecordMetadata(testTopicPartition, 0L, 0L, 0L, 0L, 0, 0)

  case class TestError(msg: String) extends Exception(msg)
}

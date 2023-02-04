package ru.auto.salesman.dao

import java.util.UUID

import cats.data.NonEmptyList
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.Task
import ru.auto.salesman.dao.MatchApplicationDao.Filter.{
  CreatedSince,
  ForDateTimeInterval,
  Unique,
  WithClients,
  WithMatchApplicationId,
  WithPaidBillingStatuses,
  WithNewBillingStatus => ForNew
}
import ru.auto.salesman.dao.MatchApplicationDao.Update
import ru.auto.salesman.dao.MatchApplicationDaoSpec.TestMatchApplicationDao
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.LimitOffset
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.MatchApplicationId
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.Statuses.{
  New,
  Paid,
  Pending
}
import ru.auto.salesman.service.match_applications.MatchApplicationService.MoreThanOneMatchApplicationFoundException
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.MatchApplicationGenerators
import ru.auto.salesman.test.model.gens.MatchApplicationGenerators.RichMatchApplicationCreateRequestGen
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.util.time.DateTimeUtil.{now, DateTimeOrdering}

import scala.util.{Failure, Success, Try}

trait MatchApplicationDaoSpec extends BaseSpec with MatchApplicationGenerators {

  def dao: TestMatchApplicationDao

  "MatchApplicationDao" should {
    "create record if unique" in {
      val record = MatchApplicationCreateRequestGen.next

      dao.createIfNotExists(record).success.value shouldBe true
      dao.findAll().success.value should contain theSameElementsAs List(record)
    }

    "do not create record if not unique" in {
      val record = MatchApplicationCreateRequestGen.next

      dao.createIfNotExists(record).success.value shouldBe true
      dao.createIfNotExists(record).success.value shouldBe false

      dao.findAll().success.value should contain theSameElementsAs List(record)
    }

    "find records with New filter" in {
      val records = NonEmptyList.of(
        MatchApplicationCreateRequestGen
          .withBillingStatus(New)
          .next,
        MatchApplicationCreateRequestGen
          .withBillingStatus(Paid)
          .next
      )

      dao.create(records).success

      dao
        .find(ForNew, limitOffset = None)
        .success
        .value shouldBe List(records.head)
    }

    "find records with WithNewBillingStatus filter" in {
      val matchApplicationId1 = MatchApplicationIdGen.next
      val matchApplicationId2 = MatchApplicationIdGen.next

      val records = NonEmptyList.of(
        MatchApplicationCreateRequestGen
          .withMatchApplicationId(matchApplicationId1)
          .withBillingStatus(New)
          .next,
        MatchApplicationCreateRequestGen
          .withMatchApplicationId(matchApplicationId2)
          .withBillingStatus(Pending)
          .next
      )

      dao.create(records).success

      dao
        .find(MatchApplicationDao.Filter.WithNewBillingStatus)
        .success
        .value shouldBe List(records.head)
    }

    "find records with custom filters filter" in {
      val matchApplicationId1 = MatchApplicationIdGen.next
      val matchApplicationId2 = MatchApplicationIdGen.next

      val records = NonEmptyList.of(
        MatchApplicationCreateRequestGen
          .withMatchApplicationId(matchApplicationId1)
          .withClientId(1L)
          .withBillingStatus(Pending)
          .next,
        MatchApplicationCreateRequestGen
          .withMatchApplicationId(matchApplicationId2)
          .withClientId(20101L)
          .withBillingStatus(New)
          .next
      )

      dao.create(records).success

      dao
        .find(
          NonEmptyList.of(
            WithPaidBillingStatuses,
            CreatedSince(now().minusDays(14)),
            WithClients(NonEmptyList.of(1L, 2L))
          ),
          limitOffset = None
        )
        .success
        .value shouldBe List(records.head)
    }

    "find records with ForDateTimeInterval filter" in {
      val records = NonEmptyList.of(
        MatchApplicationCreateRequestGen
          .withCreateDate(DateTime.parse("2020-01-07"))
          .next,
        MatchApplicationCreateRequestGen
          .withCreateDate(DateTime.parse("2020-06-19"))
          .next
      )

      dao.create(records).success

      val interval = DateTimeInterval(
        DateTime.parse("2020-01-01"),
        DateTime.parse("2020-01-17")
      )

      dao
        .find(ForDateTimeInterval(interval), limitOffset = None)
        .success
        .value shouldBe List(records.head)
    }

    "find records with filter and LimitOffset" in {
      val recordsGen = MatchApplicationCreateRequestGen.withClientId(1L)
      val startDateTime = now()

      // To make difference between records by create date field
      val records =
        Gen
          .nonEmptyListOf(recordsGen)
          .next
          .zipWithIndex
          .map { case (record, i) =>
            record.copy(createDate = startDateTime.plusSeconds(i))
          }

      dao.create(NonEmptyList.fromListUnsafe(records)).success

      val limitOffset = LimitOffset(limit = Some(2), offset = Some(0))

      dao
        .find(WithClients(1L), Some(limitOffset))
        .success
        .value shouldBe records
        .sortBy(_.createDate)(DateTimeOrdering)
        .reverse
        .take(2)
    }

    "find records with multiple filters" in {
      val records = NonEmptyList.of(
        MatchApplicationCreateRequestGen
          .withClientId(1L)
          .withBillingStatus(New)
          .next,
        MatchApplicationCreateRequestGen
          .withClientId(2L)
          .withBillingStatus(New)
          .next,
        MatchApplicationCreateRequestGen
          .withClientId(1L)
          .withBillingStatus(Paid)
          .next
      )

      dao.create(records).success

      dao
        .find(NonEmptyList.of(WithClients(1L), ForNew), limitOffset = None)
        .success
        .value shouldBe List(records.head)
    }

    "count records with ForClient filter" in {
      val recordsGen = MatchApplicationCreateRequestGen.withClientId(1L)
      val records = Gen.listOfN(7, recordsGen).next

      dao.create(NonEmptyList.fromListUnsafe(records)).success

      dao
        .count(WithClients(1L))
        .success
        .value shouldBe 7
    }
  }

  "update records with filter" in {
    val matchApplicationId1 = MatchApplicationIdGen.next
    val matchApplicationId2 = MatchApplicationIdGen.next

    val records = NonEmptyList.of(
      MatchApplicationCreateRequestGen
        .withMatchApplicationId(matchApplicationId1)
        .withUserId(AutoruUser(100))
        .withClientId(200)
        .withBillingStatus(New)
        .next,
      MatchApplicationCreateRequestGen
        .withMatchApplicationId(matchApplicationId2)
        .withUserId(AutoruUser(300))
        .withClientId(400)
        .withBillingStatus(New)
        .next
    )

    dao.create(records).success

    dao
      .update(
        NonEmptyList.of(Unique(matchApplicationId1, 200, AutoruUser(100))),
        NonEmptyList.of(Update.BillingStatus(Paid), Update.BillingPrice(90000))
      )
      .success

    val allRecords = dao.findAll().success.value

    allRecords.count { rec =>
      rec.billingStatus == Paid && rec.billingPrice == 90000L
    } shouldBe 1

    allRecords.count { rec =>
      rec.billingStatus == New && rec.billingPrice == 0
    } shouldBe 1
  }

  "get total cost for filter" in {
    val records = NonEmptyList.of(
      MatchApplicationCreateRequestGen
        .withClientId(1L)
        .withBillingStatus(Paid, cost = 10000L)
        .next,
      MatchApplicationCreateRequestGen
        .withClientId(2L)
        .withBillingStatus(Paid, cost = 50000L)
        .next,
      MatchApplicationCreateRequestGen
        .withClientId(1L)
        .withBillingStatus(Paid, cost = 20000L)
        .next
    )

    dao.create(records).success

    dao
      .totalCost(NonEmptyList.of(WithClients(1L)))
      .success
      .value shouldBe 30000L
  }

  "find record with Unique filter" in {
    val clientId = 1L
    val userId = AutoruUser(123L)
    val matchApplicationId = MatchApplicationId(UUID.randomUUID().toString)
    val record = MatchApplicationCreateRequestGen
      .withClientId(clientId)
      .withUserId(userId)
      .withMatchApplicationId(matchApplicationId)
      .next

    dao.create(NonEmptyList.of(record)).success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value shouldBe List(record)
  }

  "mark match application read by match application id and client id" in {
    val clientId = 1L
    val userId = AutoruUser(123L)
    val matchApplicationId = MatchApplicationId(Gen.uuid.next)
    val record = MatchApplicationCreateRequestGen
      .withClientId(clientId)
      .withUserId(userId)
      .withMatchApplicationId(matchApplicationId)
      .withIsRead(false)
      .next

    dao.create(NonEmptyList.of(record)).success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe false

    dao
      .update(
        NonEmptyList
          .of(
            WithClients(clientId),
            WithMatchApplicationId(matchApplicationId)
          ),
        NonEmptyList.of(Update.ReadStatus(true))
      )
      .success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe true
  }

  "do not update if precondition failed" in {
    val clientId = 1L
    val userId = AutoruUser(123L)
    val matchApplicationId = MatchApplicationId(Gen.uuid.next)
    val record = MatchApplicationCreateRequestGen
      .withClientId(clientId)
      .withUserId(userId)
      .withMatchApplicationId(matchApplicationId)
      .withIsRead(false)
      .next

    dao.create(NonEmptyList.of(record)).success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe false

    def failedPrecondition(seq: Seq[MatchApplicationCreateRequest]): Try[Unit] =
      Failure(
        MoreThanOneMatchApplicationFoundException(matchApplicationId, clientId)
      )

    dao
      .updateIfPrecondition(
        NonEmptyList
          .of(
            WithClients(clientId),
            WithMatchApplicationId(matchApplicationId)
          ),
        NonEmptyList.of(Update.ReadStatus(true)),
        failedPrecondition
      )
      .failure
      .exception shouldBe MoreThanOneMatchApplicationFoundException(
      matchApplicationId,
      clientId
    )

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe false
  }

  "update if precondition succeed" in {
    val clientId = 1L
    val userId = AutoruUser(123L)
    val matchApplicationId = MatchApplicationId(Gen.uuid.next)
    val record = MatchApplicationCreateRequestGen
      .withClientId(clientId)
      .withUserId(userId)
      .withMatchApplicationId(matchApplicationId)
      .withIsRead(false)
      .next

    dao.create(NonEmptyList.of(record)).success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe false

    def truePrecondition(seq: Seq[MatchApplicationCreateRequest]): Try[Unit] =
      Success(())

    dao
      .updateIfPrecondition(
        NonEmptyList
          .of(
            WithClients(clientId),
            WithMatchApplicationId(matchApplicationId)
          ),
        NonEmptyList.of(Update.ReadStatus(true)),
        truePrecondition
      )
      .success

    dao
      .find(Unique(matchApplicationId, clientId, userId))
      .success
      .value
      .head
      .isRead shouldBe true
  }
}

object MatchApplicationDaoSpec {

  trait TestMatchApplicationDao extends MatchApplicationDao {
    def findAll(): Task[List[MatchApplicationCreateRequest]]

    def create(records: NonEmptyList[MatchApplicationCreateRequest]): Task[Unit]
  }

}

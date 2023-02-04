package ru.auto.salesman.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, Inspectors}
import ru.auto.salesman.dao.QuotaRequestDao.{Actual, AddedInPeriod, AllActual}
import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao.commonFilter
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.{ClientId, ProductId, QuotaEntities, QuotaRequest, RegionId}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{
  quotaEntitiesGen,
  ClientIdGen,
  PartsQuotaRequestGen,
  QuotaRequestGen
}
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.generators.BasicGenerators
import zio.ZIO
import ru.auto.salesman.dao.impl.jdbc.JdbcQuotaRequestDao.tableName

trait QuotaRequestDaoSpec extends BaseSpec with BeforeAndAfter with BasicGenerators {

  protected def dao: QuotaRequestDao
  protected def clientChangedBufferDao: ClientsChangedBufferDao

  protected def cleanTables(): Unit

  before {
    cleanTables()
  }

  private def expectedQuotas(
      client: ClientId,
      requests: Iterable[QuotaRequest],
      from: DateTime
  ) =
    requests
      .filterNot(_.from.isAfter(from))
      .groupBy(_.quotaType)
      .mapValues(_.maxBy(_.from.getMillis))
      .values

  "QuotaRequestDao" should {

    "be disabled correctly" in {
      dao.disableForClient(1).success
    }

    "provide empty set" in {
      dao.get(AllActual()).success.value shouldBe empty
    }

    "correctly add quotaRequest" in {
      val request = QuotaRequestGen.next

      dao.add(List(request)).success

      val result = dao.get(AllActual()).success.value
      result should have size 1
      result.head shouldBe request
    }

    "correctly add quotaRequest with different regions" in {
      val msk = RegionId(1L)
      val otherRegion = RegionId(213L)
      val settings =
        QuotaRequest.Settings(1000, 1, None, entity = QuotaEntities.Parts)
      val request = QuotaRequestGen.next.copy(settings = settings)
      val requests = List(
        request
          .copy(regionId = Some(msk), quotaType = ProductId.QuotaPriority),
        request.copy(
          regionId = Some(otherRegion),
          quotaType = ProductId.QuotaPriority
        )
      )

      dao.add(requests).success

      dao
        .get(AllActual(entity = QuotaEntities.Parts))
        .success
        .value should contain theSameElementsAs requests
    }

    "search added quota requests in period" in {

      val requests = QuotaRequestGen
        .next(10)
        .map { request =>
          val r = request.copy(
            settings = request.settings.copy(entity = quotaEntitiesGen.next)
          )
          dao.add(List(r)).get
          r
        }
        .toSeq

      val randDate = Gen.oneOf(requests.map(_.from))

      val dates = randDate.next(2)
      val (dateFrom, dateTo) =
        (dates.minBy(_.getMillis), dates.maxBy(_.getMillis))

      val interval = new DateTimeInterval(dateFrom, dateTo)

      for (quotaEntity <- QuotaEntities.values) {

        val expected = requests
          .filter(r => interval.contains(r.from))
          .filter(r => r.settings.entity == quotaEntity)
          .filter(r => r.settings.size != 0)
          .sortBy(_.from.getMillis)

        expected shouldEqual
          dao
            .get(AddedInPeriod(DateTimeInterval(dateFrom, dateTo), quotaEntity))
            .get
            .toArray
            .sortBy(_.from.getMillis)
      }
    }

    "correctly get quotas by filter" in {
      val clients = ClientIdGen.next(10).toList
      val expected = clients.map { c =>
        val requests = listNUnique(10, QuotaRequestGen)(q => (q.quotaType, q.from)).next
          .map(_.copy(clientId = c))
        dao.add(requests).get
        c -> expectedQuotas(c, requests, now())
      }.toMap

      dao
        .get(AllActual())
        .success
        .value
        .filter(q =>
          clients.contains(q.clientId)
        ) should contain theSameElementsAs expected.values.flatten

      Inspectors.forEvery(clients) { c =>
        dao
          .get(Actual(c))
          .success
          .value should contain theSameElementsAs expected(c)
      }
    }

    "request from future should not affect actual requests" in {
      val client = 1111L
      val request = QuotaRequestGen.next.copy(clientId = client)
      val futureRequest =
        request.copy(from = now().plusDays(1), quotaType = request.quotaType)

      dao.add(List(request))
      dao.add(List(futureRequest))

      val result = dao.get(Actual(client)).success.value
      result should have size 1
      result.head shouldBe request

      val futureResult =
        dao.get(Actual(client, futureRequest.from)).success.value
      futureResult should have size 1
      futureResult.head shouldBe futureRequest
    }

    "filter disabled quota request" in {
      val client = 555L
      val request = QuotaRequestGen.next.copy(
        settings = QuotaRequest.Settings(10, 1, None),
        clientId = client,
        from = DateTime.now().minusHours(2)
      )
      val disable = request.copy(
        settings = request.settings.copy(size = 0),
        from = request.from.plusHours(1)
      )

      dao.add(List(request)).success

      val before = dao.get(Actual(client)).success.value
      before should have size 1
      before.head shouldBe request

      dao.add(List(disable)).success

      val after = dao.get(Actual(client)).success.value
      after should have size 1
      after.head shouldBe disable

      dao
        .get(Actual(client, allowDisabled = false))
        .success
        .value shouldBe empty
    }

    "write to buffer table on dealer quota insert" in {
      val request = QuotaRequestGen.next
      val (before, after) = (for {
        b <- clientChangedBufferDao.get(commonFilter)
        _ <- ZIO.fromTry(dao.add(request :: Nil))
        a <- clientChangedBufferDao.get(commonFilter)
      } yield (b, a)).success.value

      assert(before.length < after.length)
      assert(
        after.exists(r =>
          r.clientId == request.clientId && r.dataSource == tableName(
            request.settings.entity
          )
        )
      )
    }

    "not write to buffer table on parts quota insert" in {
      val request = PartsQuotaRequestGen.next
      val (before, after) = (for {
        b <- clientChangedBufferDao.get(commonFilter)
        _ <- ZIO.fromTry(dao.add(request :: Nil))
        a <- clientChangedBufferDao.get(commonFilter)
      } yield (b, a)).success.value

      after should have size before.size
      after should contain theSameElementsAs before
    }

  }

}

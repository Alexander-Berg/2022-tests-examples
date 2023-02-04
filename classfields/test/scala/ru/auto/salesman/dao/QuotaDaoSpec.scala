package ru.auto.salesman.dao

import org.joda.time.DateTime
import org.scalatest.Inspectors
import ru.auto.salesman.dao.QuotaDao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.{ClientId, Quota, QuotaEntities, QuotaTypes}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.concurrent.duration.DurationInt

trait QuotaDaoSpec extends BaseSpec {

  protected def dao: QuotaDao

  private def expectedQuotas(
      client: ClientId,
      quotas: Iterable[Quota],
      from: DateTime
  ) =
    quotas
      .filter(_.clientId == client)
      .groupBy(_.quotaType)
      .mapValues(_.last)
      .values
      .filterNot(_.from.isAfter(from))
      .filter(_.to.isAfter(from))

  "QuotaDao" should {

    "provide empty set" in {
      val client = ClientIdGen.next

      dao.get(Active(client)).success.value shouldBe empty

      dao
        .get(LastActivation(client, QuotaTypes.values.head))
        .success
        .value shouldBe empty
    }

    "correctly add activated quota" in {
      val quota = QuotaGen.next

      dao.add(quota).success

      val quotas = dao.get(Active(quota.clientId)).success.value
      quotas should have size 1
      quotas.head.toQuota shouldBe quota
    }

    "correctly get last activations" in {
      val clients = ClientIdGen.next(10).toList
      val activated: List[Quota] = clients.flatMap { c =>
        (0 to 10).map { i =>
          val r =
            QuotaGen.next.copy(clientId = c)
          dao.add(r).get
          r
        }
      }

      Inspectors.forEvery(clients) { c =>
        val quotas = dao.get(Active(c)).success.value
        val expected = expectedQuotas(c, activated, now())
        quotas.map(_.toQuota) should contain theSameElementsAs expected
      }

      Inspectors.forEvery(clients) { c =>
        Inspectors.forEvery(QuotaTypes.values) { q =>
          val quota = dao.get(LastActivation(c, q)).success.value
          val expected = expectedQuotas(c, activated, now())
            .find(_.quotaType == q)
          quota.headOption.map(_.toQuota) shouldBe expected
        }
      }

      val expectedActual: List[Quota] = clients.flatMap { c =>
        expectedQuotas(c, activated, now())
      }
      // this check is wrong, check SALESMAN-474 for details
      dao
        .get(ActualActivations())
        .success
        .value
        .filter(q => clients.contains(q.clientId)) should contain
      theSameElementsAs(expectedActual)

      val toEpoch = now()
      val expectedLastActiveTo = activated
        .groupBy(q => (q.quotaType, q.clientId))
        .mapValues(_.last)
        .values
        .filter(_.to.isAfter(toEpoch))

      dao
        .get(LastActivationsTo(toEpoch.getMillis))
        .success
        .value
        .filter(q => clients.contains(q.clientId))
        .map(_.toQuota) should contain theSameElementsAs
        expectedLastActiveTo

      val expectedLastActivations = activated
        .groupBy(q => (q.quotaType, q.clientId))
        .mapValues(_.last)
        .values

      dao
        .get(AllLastActivations(QuotaEntities.Dealer))
        .success
        .value
        .filter(q => clients.contains(q.clientId))
        .map(_.toQuota) should contain theSameElementsAs
        expectedLastActivations
    }

    "get all active for time" in {
      dao.get(AllActive(now().plusDays(7))).success.value shouldBe empty

      Inspectors.forEvery(dao.get(AllActive(now().plusDays(1))).success.value) { q =>
        val quotaDuration = DateTimeInterval(q.from, q.to).duration
        quotaDuration shouldBe 7.days
      }

      val currentTime = now()
      Inspectors.forEvery(dao.get(AllActive(now())).success.value) { q =>
        currentTime.isAfter(q.from) && q.to.isAfter(currentTime) shouldBe true
      }
    }

    "get all active changed since" in {

      dao
        .get(AllActiveChangedSince(now().plusYears(20).getMillis))
        .success
        .value shouldBe empty

      val epoch = now().minusSeconds(40).getMillis
      val currentTime = now()

      Inspectors.forEvery(dao.get(AllActiveChangedSince(epoch)).success.value) { q =>
        currentTime.isAfter(q.from) && q.to.isAfter(
          currentTime
        ) && q.epoch > epoch shouldBe true
      }
    }
  }

}

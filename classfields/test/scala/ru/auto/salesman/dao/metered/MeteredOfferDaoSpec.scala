package ru.auto.salesman.dao.metered

import com.codahale.metrics.MetricRegistry
import nl.grons.metrics4.scala.Timer
import ru.auto.salesman.TaskManaged
import ru.auto.salesman.dao.OfferDao
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao._
import ru.auto.salesman.dao.metered.MeteredOfferDaoSpec.DummyOfferDao
import ru.auto.salesman.model.Offer
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.test.BaseSpec

import scala.language.reflectiveCalls
import scala.util.{Success, Try}

class MeteredOfferDaoSpec extends BaseSpec {

  private val dao = new DummyOfferDao with MeteredOfferDao {

    private var lastTimer: Option[String] = None

    def serviceName: String = ???

    def metricRegistry: MetricRegistry = ???

    override def timer(name: String): Timer =
      new Timer(new com.codahale.metrics.Timer()) {

        override def time[A](f: => A): A = {
          lastTimer = Some(name)
          super.time(f)
        }
      }

    def getLastTimer: Option[String] = lastTimer

    override def getSortedUsersWithActiveOffers(
        partition: Partition,
        withUserIdMoreThan: Option[Long]
    ): TaskManaged[Stream[Long]] = ???
  }

  "Metered offer dao" should {

    "time get()" in {
      dao.get(ForIdAndCategory(1, Cars)).success.value shouldBe Nil
      dao.getLastTimer.value shouldBe "get.ForIdAndCategory"
    }

    "time update()" in {
      dao
        .update(OfferIdCategory(1, Cars), OfferPatch())
        .success
        .value shouldBe (())
      dao.getLastTimer.value shouldBe "update.OfferIdCategory"
    }
  }
}

object MeteredOfferDaoSpec {

  private class DummyOfferDao extends OfferDao {

    def get(filter: Filter): Try[List[Offer]] = Success(Nil)

    def update(condition: Condition, patch: OfferPatch): Try[Unit] =
      Success(())

    def getSortedUsersWithActiveOffers(
        partition: Partition,
        withUserIdMoreThan: Option[UserId]
    ): TaskManaged[Stream[UserId]] = ???

  }
}

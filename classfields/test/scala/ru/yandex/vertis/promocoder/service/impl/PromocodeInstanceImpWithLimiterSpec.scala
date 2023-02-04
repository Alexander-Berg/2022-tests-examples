package ru.yandex.vertis.promocoder.service.impl

import java.util.NoSuchElementException

import org.junit.runner.RunWith
import org.scalatest.GivenWhenThen

import ru.yandex.vertis.promocoder.{Domains, FeatureSpecBase}
import ru.yandex.vertis.promocoder.dao.PromocodeInstanceDao
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmPromocodeDao, JvmPromocodeInstanceDao}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Constraints, Promocode, PromocodeInstance, User}
import ru.yandex.vertis.promocoder.service.FeaturesShippingPromocodeInstanceServiceSpec.NoOpPromocodeService
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.FailedActivationsLimitExceededException
import ru.yandex.vertis.promocoder.service.PromocodeService.Filter.{ByAlias, ByCode}
import ru.yandex.vertis.promocoder.service.limiter.{LimiterService, LocalLimiterServiceImpl}
import ru.yandex.vertis.promocoder.service.{PromocodeInstanceService, PromocodeService}
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PromocodeInstanceImpWithLimiterSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  type Record = (User, Promocode)

  val EmptyConstraints = Constraints(
    deadline = DateTimeUtil.now().plusDays(2),
    totalActivations = Int.MaxValue,
    userActivations = Int.MaxValue
  )

  implicit val rc: RequestContext = AutomatedContext("PromocodeInstanceServiceSpec")

  import scala.concurrent.ExecutionContext.Implicits.global

  def getService(promocodes: Iterable[Promocode], instances: Iterable[Record]): PromocodeInstanceService = {
    val promocodeDao = new JvmPromocodeDao()
    Future.sequence(promocodes.map(promocodeDao.upsert)).futureValue
    val dao = new JvmPromocodeInstanceDao(promocodeDao)
    Future.sequence {
      instances.map { case (user, promocode) =>
        dao.insert(PromocodeInstanceDao.Source(user, promocode))
      }
    }.futureValue

    val limiterServiceInstance = new LocalLimiterServiceImpl(Domains.AutoRu, 1, 1.seconds)

    new PromocodeInstanceServiceImpl(dao, new PromocodeServiceWithLimiter(promocodes))
      with PromocodeInstanceServiceWithLimiter {
      override def limiterService: LimiterService = limiterServiceInstance
      implicit override def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
  }

  Feature("promocode instance service with limiter") {
    Scenario("too many failed activation requests") {
      val user = "user"
      Given("promocode with empty constraints")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints)
      val promocode2 = PromocodeGen.next.copy(constraints = EmptyConstraints)
      val promocode3 = PromocodeGen.next.copy(constraints = EmptyConstraints)

      Given("empty service")
      val service = getService(Iterable(promocode, promocode3), Iterable.empty)

      When("user activate promocode")
      Then("activation successes")
      service.activate(user, promocode.code)

      When("activation of non-existent promocode")
      Then("should fail")
      shouldFailWith[NoSuchElementException] {
        service.activate(user, promocode2.code)
      }

      When("activation of promocode with limit of failed activations reached")
      Then("should fail")
      shouldFailWith[FailedActivationsLimitExceededException] {
        service.activate(user, promocode3.code)
      }

      Thread.sleep(1000)

      When("user activate promocode")
      Then("activation successes")
      val instance = service.activate(user, promocode3.code).futureValue
      instance.code shouldBe promocode3.code
      instance.user shouldBe user
      instance.status shouldBe PromocodeInstance.Statuses.Created
    }
  }

  class PromocodeServiceWithLimiter(codes: Iterable[Promocode]) extends NoOpPromocodeService {
    private var promocodes = codes.toList

    override def create(promocode: Promocode)(implicit rc: RequestContext): Future[Unit] = {
      Future.successful {
        promocodes = promocodes :+ promocode
      }
    }

    override def get(
        filter: PromocodeService.Filter,
        options: PromocodeService.Options
      )(implicit rc: RequestContext): Future[Promocode] = {

      Future {
        filter match {
          case ByCode(code) =>
            promocodes.find(promocode => promocode.code == code).get
          case ByAlias(alias) =>
            promocodes.find(promocode => promocode.code == alias).get
        }
      }

    }
  }

}

package ru.yandex.vertis.promocoder.service.impl

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.promocoder.dao.impl.jvm.{
  JvmFeatureInstanceArchiveDao,
  JvmFeatureInstanceDao,
  JvmPromocodeAliasDao,
  JvmPromocodeDao,
  JvmPromocodeInstanceDao
}
import ru.yandex.vertis.promocoder.model.Promocode
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.CommissionControllerSpec.Setup
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.ActiveForUser
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Mode
import ru.yandex.vertis.promocoder.service.FeaturesShippingPromocodeInstanceServiceSpec.NoOpPromocodeService
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter.ById
import ru.yandex.vertis.promocoder.service.{CommissionControllerSpec, StaticDecider}
import ru.yandex.vertis.promocoder.util.{CharsGenerator, DefaultPromocodeGenerator, TimeService}

import scala.concurrent.ExecutionContext

/** Runnable spec on [[CommissionControllerImpl]]
  *
  * @author alex-kovalenko
  */
class CommissionControllerImplSpec extends CommissionControllerSpec with ScalaFutures with ModelGenerators {

  implicit override def executionContext: ExecutionContext = ExecutionContext.global

  def initialize(promocode: Promocode): Setup =
    initializeInternal(promocode)

  private def initializeInternal(promocode: Promocode, deciderResponse: Boolean = true): Setup = {
    val promocodeDao = new JvmPromocodeDao
    val promocodes = new PromocodeServiceImpl(
      promocodeDao,
      new JvmPromocodeAliasDao,
      new DefaultPromocodeGenerator(CharsGenerator.Default)
    )
    val promocodeInstances =
      new PromocodeInstanceServiceImpl(new JvmPromocodeInstanceDao(promocodeDao), new NoOpPromocodeService)
    val featureInstances =
      new FeatureInstanceServiceImpl(new JvmFeatureInstanceDao(new TimeService()), new JvmFeatureInstanceArchiveDao)
    val decider = new StaticDecider(deciderResponse)
    val controller = new CommissionControllerImpl(promocodes, promocodeInstances, featureInstances, decider)

    val instance = (for {
      _ <- promocodes.create(promocode)
      result <- promocodeInstances.activate(user, promocode)
    } yield result).futureValue

    Setup(controller, promocodeInstances, featureInstances, instance)
  }

  Feature("CommissionController with CommissionDecider") {
    Scenario("decider does not allows to ship commission") {
      Given("referring promocode")
      val promocode = {
        val feature = FeatureGen.next.copy(referring = Some(ReferringGen.next.copy(user = user)))
        PromocodeGen.next.copy(features = Iterable(feature))
      }
      val Setup(controller, promocodeInstances, featureInstances, instance) =
        initializeInternal(promocode, deciderResponse = false)

      When("process commission")
      Then("do nothing")
      for {
        _ <- controller.ship(instance)
        _ <- promocodeInstances
          .get(ById(instance.id))
          .map(_.toSet shouldBe Set(instance))
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map(_.size shouldBe 0)
      } yield succeed
    }
  }
}

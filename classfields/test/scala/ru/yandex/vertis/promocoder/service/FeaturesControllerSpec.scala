package ru.yandex.vertis.promocoder.service

import org.scalatest.GivenWhenThen
import ru.yandex.vertis.promocoder.FeatureSpecBase
import ru.yandex.vertis.promocoder.model.FeatureInstance.PromocodeOrigin
import ru.yandex.vertis.promocoder.model.PromocodeInstance.Statuses
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Promocode, PromocodeInstance}
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Mode
import ru.yandex.vertis.promocoder.service.FeaturesControllerSpec.Setup
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.DurationInt

/** Specs on [[FeaturesController]]
  *
  * @author alex-kovalenko
  */
trait FeaturesControllerSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  implicit val rc: RequestContext = AutomatedContext("FeaturesControllerSpec")

  type Record = (Promocode, PromocodeInstance)

  def initialize: Setup

  Feature("FeaturesController") {
    val user = "user"
    val emptyConstraints = ConstraintsGen.next.copy(
      deadline = DateTimeUtil.now().plusDays(1),
      totalActivations = Int.MaxValue,
      userActivations = Int.MaxValue,
      blacklist = Set.empty
    )
    def checkAfterCreation(setup: Setup, promocode: Promocode, instance: PromocodeInstance): Unit = {
      setup.promocodeInstances.get(PromocodeInstanceService.Filter.ById(instance.id)).futureValue.toList match {
        case first :: Nil if first.status == Statuses.Shipped =>
        case other => fail(s"Unexpected $other")
      }

      val features =
        setup.featureInstances.get(FeatureInstanceService.Filter.ActiveForUser(user), Mode.Default).futureValue

      features.size shouldBe promocode.features.size

      features.map(_.tag).toSet should
        contain theSameElementsAs promocode.features.map(_.tag).toSet
      features.map(_.user).toSet should (have size 1 and contain(user))

      features.map(_.origin).foreach {
        case PromocodeOrigin(instanceId) if instanceId == instance.id =>
        case other => fail(s"Unexpected feature origin $other")
      }
    }

    Scenario("create from promocode") {
      Given("one promocode with instance")
      val setup @ Setup(promocodes, promocodeInstances, _, controller) = initialize

      val promocode = {
        val p = PromocodeGen.next
        p.copy(
          constraints = emptyConstraints,
          features = p.features.map(f => f.copy(lifetime = 1.day, startTime = None))
        )
      }
      promocodes.create(promocode).futureValue
      val instance = promocodeInstances.activate(user, promocode).futureValue

      When("get PromocodeSource")
      val source = FeaturesController.PromocodeSource(promocode, instance)

      Then("create features and update instance status")
      controller.ship(source).futureValue
      checkAfterCreation(setup, promocode, instance)
    }

    Scenario("create from instance") {
      Given("one promocode with instance")
      val setup @ Setup(promocodes, promocodeInstances, _, controller) = initialize
      val promocode = {
        val p = PromocodeGen.next
        p.copy(
          constraints = emptyConstraints,
          features = p.features.map(f => f.copy(lifetime = 1.day, startTime = None))
        )
      }
      promocodes.create(promocode).futureValue
      val instance = promocodeInstances.activate(user, promocode).futureValue

      When("get InstanceSource")
      val source = FeaturesController.InstanceSource(instance)

      Then("create features and update instance status")
      controller.ship(source).futureValue

      checkAfterCreation(setup, promocode, instance)
    }
  }
}

object FeaturesControllerSpec {

  case class Setup(
      promocodes: PromocodeService,
      promocodeInstances: PromocodeInstanceService,
      featureInstances: FeatureInstanceService,
      controller: FeaturesController)
}

package ru.yandex.vertis.promocoder.service

import org.scalatest.featurespec.AsyncFeatureSpecLike
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.promocoder.model.PromocodeInstance.ReferringStatuses
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Commission, Promocode, PromocodeInstance}
import ru.yandex.vertis.promocoder.service.CommissionControllerSpec.Setup
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.ActiveForUser
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Mode
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter.ById
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}

/** Specs on [[CommissionController]]
  *
  * @author alex-kovalenko
  */
trait CommissionControllerSpec extends Matchers with AsyncFeatureSpecLike with GivenWhenThen with ModelGenerators {

  implicit val rc: RequestContext = AutomatedContext("FeaturesControllerSpec")
  val user = "user"

  def initialize(promocode: Promocode): Setup

  Feature("CommissionController") {

    Scenario("process commission of non-referring promocode") {
      Given("non-referring promocode")
      val promocode = {
        val p = PromocodeGen.next
        p.copy(features = p.features.map(_.copy(referring = None)))
      }
      val Setup(controller, promocodeInstances, featureInstances, instance) =
        initialize(promocode)

      When("process commission")
      Then("do nothing")
      for {
        _ <- controller.ship(instance)
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map(_.size shouldBe 0)
        _ <- promocodeInstances
          .get(ById(instance.id))
          .map(_.toSet shouldBe Set(instance))
      } yield succeed
    }

    Scenario("process complex referring promocode") {
      Given("promocode with features with different referring")
      val ref1 :: ref2 :: Nil =
        listNUnique(2, ReferringGen)(_.feature.tag).next.map(_.copy(user = user))
      val promocode = {
        val features =
          FeatureGen.next.copy(referring = None) ::
            FeatureGen.next.copy(referring = Some(ref1)) ::
            FeatureGen.next(2).map(_.copy(referring = Some(ref2))).toList
        PromocodeGen.next.copy(features = features)
      }
      val Setup(controller, promocodeInstances, featureInstances, instance) =
        initialize(promocode)

      When("process commission")
      for {
        _ <- controller.ship(instance)
        _ = Then("update referring status and create features for commission")

        _ <- promocodeInstances
          .get(ById(instance.id))
          .map(_.toList match {
            case head :: Nil if head.referringStatus.contains(ReferringStatuses.Shipped) =>
            case other => fail(s"Unexpected $other")
          })
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map { features =>
            features.size shouldBe 2
            features.map(_.tag).toSet should (have size 2 and
              contain theSameElementsAs Set(ref1.feature.tag, ref2.feature.tag))
          }
      } yield succeed
    }

    Scenario("process fixed commission") {
      Given("promocode with fixed commission")
      val commission = 100
      val ref = ReferringGen.next.copy(user = user, commission = Commission.Fix(commission))
      val promocode = PromocodeGen.next.copy(features = Iterable(FeatureGen.next.copy(referring = Some(ref))))
      val Setup(controller, _, featureInstances, instance) =
        initialize(promocode)

      When("process commission")
      Then("correctly accrue fixed commission")
      for {
        _ <- controller.ship(instance)
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map(_.toList match {
            case feature :: Nil
                if feature.tag == ref.feature.tag
                  && feature.count == commission =>
            case other => fail(s"Unexpected $other")
          })

      } yield succeed
    }

    Scenario("process percent commission") {
      Given("promocode with fixed commission")
      val percent = 75
      val count = 200
      val ref = ReferringGen.next.copy(user = user, commission = Commission.Percent(percent))
      val promocode =
        PromocodeGen.next.copy(features = Iterable(FeatureGen.next.copy(count = count, referring = Some(ref))))
      val Setup(controller, _, featureInstances, instance) =
        initialize(promocode)
      val expectedCount = 150

      When("process commission")
      Then("correctly accrue percent commission")
      for {
        _ <- controller.ship(instance)
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map(_.toList match {
            case feature :: Nil
                if feature.tag == ref.feature.tag
                  && feature.count == expectedCount =>
            case other => fail(s"Unexpected $other")
          })
      } yield succeed
    }

    Scenario("process promocode with a lot of features with same referring") {
      Given("promocode with fixed commission")
      val commission = 100
      val fCount = 10
      val ref = ReferringGen.next.copy(user = user, commission = Commission.Fix(commission))

      val promocode = PromocodeGen.next.copy(features = FeatureGen.next(fCount).map(_.copy(referring = Some(ref))))

      val Setup(controller, _, featureInstances, instance) =
        initialize(promocode)

      When("process commission")
      Then("correctly accrue fixed commission")
      for {
        _ <- controller.ship(instance)
        _ <- featureInstances
          .get(ActiveForUser(user), Mode.Default)
          .map(_.toList match {
            case feature :: Nil if feature.count == fCount * commission =>
            case other => fail(s"Unexpected $other")
          })

      } yield succeed
    }
  }
}

object CommissionControllerSpec {

  case class Setup(
      controller: CommissionController,
      promocodeInstances: PromocodeInstanceService,
      featureInstances: FeatureInstanceService,
      instance: PromocodeInstance)
}

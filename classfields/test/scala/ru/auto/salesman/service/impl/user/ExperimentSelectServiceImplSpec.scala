package ru.auto.salesman.service.impl.user

import cats.data.NonEmptySet
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.user.{Experiment, ExperimentInfo, Experiments}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, RegionId}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.AutomatedContext

class ExperimentSelectServiceImplSpec extends BaseSpec with ServiceModelGenerators {
  import ExperimentSelectServiceImplSpec._

  implicit val rc = AutomatedContext("test")

  val experimentSelectService = new ExperimentSelectServiceImpl()

  "ExperimentSelectServiceImpl" should {

    "select first experiment" in {
      val experiments = Experiments(
        expBoxes,
        List(
          Experiment(
            "exp1",
            geoIds = None,
            experimentProducts = None
          ),
          Experiment(
            "exp2",
            geoIds = None,
            experimentProducts = None
          )
        )
      )
      experimentSelectService
        .getExperiment(
          Some(experiments),
          ProductProvider.AutoruGoods.Placement,
          geoIds = Set.empty
        )
        .success
        .value shouldBe Some(
        ExperimentInfo(activeExperimentId = Some("exp1"), expBoxes)
      )

    }

    "select experiment by productId and regionId" in {
      val experiments = Experiments(
        expBoxes,
        List(
          Experiment(
            "exp1",
            geoIds = Some(NonEmptySet.one(RegionId(44))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          ),
          Experiment(
            "exp2",
            geoIds = Some(NonEmptySet.one(RegionId(42))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          )
        )
      )

      experimentSelectService
        .getExperiment(
          Some(experiments),
          autoru = ProductProvider.AutoruGoods.Badge,
          geoIds = Set(RegionId(42))
        )
        .success
        .value shouldBe Some(
        ExperimentInfo(activeExperimentId = Some("exp2"), expBoxes)
      )
    }

    "select experiment for geo Id from offer" in {
      val offer = EnrichedOfferGen.next.copy(
        geoId = List(RegionId(43))
      )

      val experiments = Experiments(
        expBoxes,
        List(
          Experiment(
            "exp1",
            geoIds = Some(NonEmptySet.one(RegionId(44))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          ),
          Experiment(
            "exp2",
            geoIds = Some(NonEmptySet.one(RegionId(42))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          ),
          Experiment(
            "exp3",
            geoIds = Some(NonEmptySet.one(RegionId(43))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          )
        )
      )

      experimentSelectService
        .getExperimentForOffer(
          Some(experiments),
          ProductProvider.AutoruGoods.Badge,
          offer = Some(offer)
        )
        .success
        .value shouldBe Some(
        ExperimentInfo(activeExperimentId = Some("exp3"), expBoxes)
      )

    }

    "select experiment for all products and all region if in experiment was not selected region and product" in {
      val experiments = Experiments(
        expBoxes,
        List(
          Experiment(
            "exp1",
            geoIds = Some(NonEmptySet.one(RegionId(44))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          ),
          Experiment(
            "exp2",
            geoIds = None,
            experimentProducts = None
          ),
          Experiment(
            "exp3",
            geoIds = Some(NonEmptySet.one(RegionId(43))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          )
        )
      )
      experimentSelectService
        .getExperiment(
          allUserExperiments = Some(experiments),
          geoIds = Set(RegionId(11), RegionId(12)),
          autoru = ProductProvider.AutoruGoods.Placement
        )
        .success
        .value shouldBe Some(
        ExperimentInfo(activeExperimentId = Some("exp2"), expBoxes)
      )
    }

    "didn't select experiment by parameters" in {
      val experiments = Experiments(
        expBoxes,
        List(
          Experiment(
            "exp1",
            geoIds = Some(NonEmptySet.one(RegionId(44))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          ),
          Experiment(
            "exp32",
            geoIds = Some(NonEmptySet.one(RegionId(43))),
            experimentProducts = Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
          )
        )
      )

      experimentSelectService
        .getExperiment(
          allUserExperiments = Some(experiments),
          geoIds = Set(RegionId(42), RegionId(12)),
          autoru = ProductProvider.AutoruGoods.Placement
        )
        .success
        .value shouldBe Some(
        ExperimentInfo(activeExperimentId = None, expBoxes)
      )
    }

    "return None if allUserExperiments is None" in {
      experimentSelectService
        .getExperiment(
          allUserExperiments = None,
          geoIds = Set(RegionId(42), RegionId(12)),
          autoru = ProductProvider.AutoruGoods.Placement
        )
        .success
        .value shouldBe None
    }
  }

  implicit def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}

object ExperimentSelectServiceImplSpec {
  val expBoxes = "testBoxes"
}

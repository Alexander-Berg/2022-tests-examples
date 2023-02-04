package ru.auto.api.managers.price

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators.ProductGen
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.gen.SalesmanModelGenerators

class UserRecommendationPriorityDeciderSpec
  extends BaseSpec
  with ScalaCheckPropertyChecks
  with SalesmanModelGenerators {

  "RecommendationPriorityDecider" should {
    "set recommendation priority 0 for Boost for new draft" in {
      forAll(bool, bool) { (vipActive, vipAvailable) =>
        val context =
          new RecommendationContext(vipActive, vipAvailable, isNewDraft = true, isReseller = false)
        val decider = new UserRecommendationPriorityDecider(context)
        val res = decider.getRecommendationPriority(Boost)
        res shouldBe 0
      }
    }

    "set recommendation priority if vip active" in {
      forAll(ProductGen, bool, bool) { (product, vipAvailable, isNewDraft) =>
        val context = new RecommendationContext(vipActive = true, vipAvailable, isNewDraft, isReseller = false)
        val decider = new UserRecommendationPriorityDecider(context)
        val res = decider.getRecommendationPriority(product)
        product match {
          case PackageVip => res shouldBe 14
          case ShowInStories => res shouldBe 11
          case _ => res shouldBe 0
        }
      }
    }

    "set recommendation priority if vip not active but available, and it's not new draft" in {
      forAll(ProductGen) { product =>
        val context =
          new RecommendationContext(vipActive = false, vipAvailable = true, isNewDraft = false, isReseller = false)
        val decider = new UserRecommendationPriorityDecider(context)
        val res = decider.getRecommendationPriority(product)
        product match {
          case PackageVip => res shouldBe 14
          case PackageTurbo => res shouldBe 12
          case ShowInStories => res shouldBe 11
          case TopList => res shouldBe 10
          case SpecialOffer => res shouldBe 8
          case Highlighting => res shouldBe 6
          case Boost => res shouldBe 4
          case Badge => res shouldBe 2
          case _ => res shouldBe 0
        }
      }
    }

    "set default recommendation priority if vip not active and not available, and it's not new draft" in {
      forAll(ProductGen) { product =>
        val context =
          new RecommendationContext(vipActive = false, vipAvailable = false, isNewDraft = false, isReseller = false)
        val decider = new UserRecommendationPriorityDecider(context)
        val res = decider.getRecommendationPriority(product)
        product match {
          case PackageTurbo => res shouldBe 14
          case PackageExpress => res shouldBe 12
          case ShowInStories => res shouldBe 11
          case TopList => res shouldBe 10
          case SpecialOffer => res shouldBe 8
          case Highlighting => res shouldBe 6
          case Boost => res shouldBe 4
          case Badge => res shouldBe 2
          case _ => res shouldBe 0
        }
      }
    }
  }
}

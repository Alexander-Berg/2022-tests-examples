package ru.auto.salesman.service.impl.user

import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  FeatureCount,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureUnits
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil
import PromocoderServiceImplSpec.createFeature
import org.scalacheck.Gen
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.product.AutoruProduct

class PromocoderServiceImplSpec extends BaseSpec with ServiceModelGenerators {

  implicit val rc: RequestContext = AutomatedContext("PromocoderServiceSpec")

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  protected val promocoderClient = mock[PromocoderClient]

  def service: PromocoderServiceImpl =
    new PromocoderServiceImpl(promocoderClient)

  "PromocoderService" should {

    "get features with no constraint" in {
      forAll(
        list(10, 20, ProductGen),
        list(10, 20, featureInstanceGen)
      ) { (products, features) =>
        val tags = products.map(_.name).toSet
        val expectedFeatures =
          features.filter(f => tags(f.tag) && f.payload.constraint.isEmpty)
        PromocoderServiceImpl
          .productFeatures(features, products, None)
          .flatMap(_._2) should contain theSameElementsAs expectedFeatures
      }
    }

    "get features with constraints" in {
      forAll(
        list(10, 20, ProductGen),
        list(10, 20, featureInstanceGen),
        OfferIdentityGen
      ) { (products, features, offerId) =>
        val tags = products.map(_.name).toSet
        val expectedFeatures = features.filter(f =>
          tags(f.tag) && f.payload.constraint.forall(_.offer == offerId)
        )
        PromocoderServiceImpl
          .productFeatures(features, products, Some(offerId))
          .flatMap(_._2) should contain theSameElementsAs expectedFeatures
      }
    }

    "return free feature with earliest deadline" in {
      forAll(ProductGen) { product =>
        forAll(
          featureInstanceFixedPriceGen(product),
          featureInstance100PercentGen(product),
          bundleFeaturePayloadGen
        ) { (nonFree, free, bundle) =>
          val freeFeatures = Iterator
            .iterate(free)(f => f.copy(deadline = f.deadline.plusDays(1)))
            .take(5)
            .toList

          val bundleFeature =
            createFeature(bundle).copy(deadline = free.deadline.minusDays(1))

          PromocoderServiceImpl
            .findFullFreeFeature(nonFree :: bundleFeature :: freeFeatures)
            .shouldBe(Some(free))
        }
      }
    }

    "return free promocode even if bundle has earlier deadline" in {
      forAll(ProductGen) { product =>
        forAll(
          featureInstanceFixedPriceGen(product),
          featureInstance100PercentGen(product),
          bundleFeaturePayloadGen
        ) { (nonFree, free, bundle) =>
          val bundleFeature =
            createFeature(bundle).copy(deadline = free.deadline.minusDays(1))

          PromocoderServiceImpl
            .findFullFreeFeature(List(nonFree, free, bundleFeature))
            .shouldBe(Some(free))
        }
      }
    }

    "return None if there is no any feature fully compensating price" in {
      forAll(ProductGen) { p =>
        forAll(Gen.listOfN(5, featureInstanceFixedPriceGen(p))) { fs =>
          PromocoderServiceImpl
            .findFullFreeFeature(fs)
            .shouldBe(None)
        }
      }
    }

    "return bundle feature if there is no any other feature fully compensating price" in {
      forAll(ProductGen) { p =>
        forAll(
          Gen.listOfN(5, featureInstanceFixedPriceGen(p)),
          bundleFeaturePayloadGen.map(createFeature)
        ) { (fs, bundle) =>
          PromocoderServiceImpl
            .findFullFreeFeature(bundle :: fs)
            .shouldBe(Some(bundle))
        }
      }
    }

    "match offers-history-reports-10 with feature" in {
      forAll(featureInstancePercentGen(OffersHistoryReports(10))) { feature =>
        getMatchedFeature(OffersHistoryReports(10), feature) shouldBe Some(
          feature
        )
      }
    }

    "match offers-history-reports-1 with feature" in {
      forAll(featureInstancePercentGen(OffersHistoryReports(1))) { feature =>
        getMatchedFeature(OffersHistoryReports(1), feature) shouldBe Some(
          feature
        )
      }
    }

    "not match offers-history-reports-10 and feature for offers-history-reports-1" in {
      forAll(featureInstancePercentGen(OffersHistoryReports(1))) { feature =>
        getMatchedFeature(OffersHistoryReports(10), feature) shouldBe None
      }
    }

  }

  def getMatchedFeature(
      product: AutoruProduct,
      feature: FeatureInstance
  ): Option[FeatureInstance] =
    PromocoderServiceImpl
      .productFeatures(List(feature), List(product), offerId = None)
      .get(product)
      .flatMap(_.headOption)
}

object PromocoderServiceImplSpec {

  def createFeature(payload: FeaturePayload): FeatureInstance =
    FeatureInstance(
      "id",
      FeatureOrigin("origin"),
      "product.name",
      "user",
      FeatureCount(1L, FeatureUnits.Items),
      DateTimeUtil.now().minusDays(1),
      DateTimeUtil.now().plusDays(1),
      payload
    )
}

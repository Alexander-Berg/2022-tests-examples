package ru.auto.api.managers.enrich.enrichers

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.BaseSpec
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.OfferGen
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.services.salesman.SalesmanUserClient
import ru.auto.salesman.model.user.ApiModel.ProductContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class ActiveServiceEnricherSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with SalesmanModelGenerators {

  private val salesmanUserClient = mock[SalesmanUserClient]
  private val enricher = new ActiveServiceEnricher(salesmanUserClient)
  private val options = EnrichOptions(prolongable = true)

  "ActiveServiceEnricher" when {
    "enriching goods" should {

      "prolongable = true, enrich prolongPrice with prolongPrice which is > 0" in {
        val prolongPrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.GOODS),
                productPriceGen(price = priceGen(prolongPrice = Gen.const(prolongPrice)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getGoods.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = true, enrich prolongPrice with basePrice, when salesman prolongPrice = 0" in {
        val basePrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.GOODS),
                productPriceGen(price = priceGen(basePrice = Gen.const(basePrice), prolongPrice = Gen.const(0)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getGoods.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = false, empty prolong price" in {
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(Some(p.salesName), Gen.const(ProductContext.ContextCase.GOODS)),
              prolongableGen = Gen.const(false)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val service = enrichedOffer.getServicesList.asScala.head

            service.hasAutoProlongPrice shouldBe false
            service.getDays shouldBe 0
            service.getProlongable shouldBe false
          }
        }
      }
    }
    "enriching bundles" should {

      "prolongable = true, enrich prolongPrice with prolongPrice which is > 0" in {
        val prolongPrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.BUNDLE),
                productPriceGen(price = priceGen(prolongPrice = Gen.const(prolongPrice)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getBundle.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = true, enrich prolongPrice with basePrice, when salesman prolongPrice = 0" in {
        val basePrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.BUNDLE),
                productPriceGen(price = priceGen(basePrice = Gen.const(basePrice), prolongPrice = Gen.const(0)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getBundle.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = false, empty prolong price" in {
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(Some(p.salesName), Gen.const(ProductContext.ContextCase.BUNDLE)),
              prolongableGen = Gen.const(false)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val service = enrichedOffer.getServicesList.asScala.head

            service.hasAutoProlongPrice shouldBe false
            service.getDays shouldBe 0
            service.getProlongable shouldBe false
          }
        }
      }
    }
    "enriching subscriptions" should {

      "prolongable = true, enrich prolongPrice with prolongPrice which is > 0" in {
        val prolongPrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.SUBSCRIPTION),
                productPriceGen(price = priceGen(prolongPrice = Gen.const(prolongPrice)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getSubscription.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = true, enrich prolongPrice with basePrice, when salesman prolongPrice = 0" in {
        val basePrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.SUBSCRIPTION),
                productPriceGen(price = priceGen(basePrice = Gen.const(basePrice), prolongPrice = Gen.const(0)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getSubscription.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = false, empty prolong price" in {
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(Some(p.salesName), Gen.const(ProductContext.ContextCase.SUBSCRIPTION)),
              prolongableGen = Gen.const(false)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val service = enrichedOffer.getServicesList.asScala.head

            service.hasAutoProlongPrice shouldBe false
            service.getDays shouldBe 0
            service.getProlongable shouldBe false
          }
        }
      }
    }
    "enriching subscriptions bundle" should {

      "prolongable = true, enrich prolongPrice with prolongPrice which is > 0" in {
        val prolongPrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.SUBSCRIPTION_BUNDLE),
                productPriceGen(price = priceGen(prolongPrice = Gen.const(prolongPrice)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getSubscriptionBundle.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = true, enrich prolongPrice with basePrice, when salesman prolongPrice = 0" in {
        val basePrice = 99900
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(
                Some(p.salesName),
                Gen.const(ProductContext.ContextCase.SUBSCRIPTION_BUNDLE),
                productPriceGen(price = priceGen(basePrice = Gen.const(basePrice), prolongPrice = Gen.const(0)))
              ),
              prolongableGen = Gen.const(true)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val productPrice = product.getContext.getSubscriptionBundle.getProductPrice

            val expectedDuration = productPrice.getDuration.getSeconds

            val service = enrichedOffer.getServicesList.asScala.head

            service.getAutoProlongPrice.getValue shouldBe 999
            service.getDays shouldBe expectedDuration / 86400
            service.getProlongable shouldBe true
          }
        }
      }

      "prolongable = false, empty prolong price" in {
        forAll(ModelGenerators.ProductGen, minSuccessful(10)) { p =>
          forAll(
            OfferGen.map(
              _.toBuilder
                .clearServices()
                .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
                .build
            ),
            productResponseGen(
              Gen.const(p),
              productContextGen(Some(p.salesName), Gen.const(ProductContext.ContextCase.SUBSCRIPTION_BUNDLE)),
              prolongableGen = Gen.const(false)
            ),
            minSuccessful(10)
          ) { (offer, product) =>
            val enrichedOffer = enricher.enrich(offer, Iterable(product))
            val service = enrichedOffer.getServicesList.asScala.head

            service.hasAutoProlongPrice shouldBe false
            service.getDays shouldBe 0
            service.getProlongable shouldBe false
          }
        }
      }
    }
  }

  "ActiveServiceEnricher" should {
    "set prolongation" in {
      forAll(ModelGenerators.ProductGen) { p =>
        forAll(
          OfferGen.map(
            _.toBuilder
              .clearServices()
              .addAllServices(Iterable(PaidService.newBuilder.setService(p.salesName).build).asJava)
              .build
          ),
          bool.flatMap { prolongable =>
            productResponseGen(Gen.const(p)).map(_.toBuilder.setProlongable(prolongable).build)
          }
        ) { (offer, product) =>
          val enrichedOffer = enricher.enrich(offer, Iterable(product))
          enrichedOffer.getServicesList.asScala.head.getProlongable shouldBe product.getProlongable
        }
      }
    }
  }
}

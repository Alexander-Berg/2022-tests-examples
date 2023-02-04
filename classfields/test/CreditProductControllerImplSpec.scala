package ru.yandex.vertis.shark.controller.impl

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.zio_baker.model.GeobaseId
import ru.yandex.vertis.shark.Mock._
import ru.yandex.vertis.shark.controller.CreditProductController
import ru.yandex.vertis.shark.dictionary.CreditProductDictionary
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.{
  AutoruCreditApplication,
  ConsumerCreditProduct,
  CreditProduct,
  DealerCreditProductStub
}
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.Task
import zio.test.Assertion.{equalTo, fails, isSubtype}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.{failure, value}

object CreditProductControllerImplSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CreditProductController")(
      listEnrichSuite,
      listFullEnrichSuite,
      getEnrichedSuite
    )

  private lazy val listEnrichSuite: ZSpec[Any, Throwable] =
    suite("list enriched")(
      testM("not empty list") {

        val geobaseIds = Seq(MoscowRegionId, SpbRegionId, CrimeaRegionId)
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()
        val filter = CreditProductDictionary.FilterByIds(Seq(creditProduct.id))

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.List(equalTo(filter), value(Seq(creditProduct)))

        val creditProductEnricherMock =
          CreditProductEnricherMock
            .Enrich(equalTo((creditProduct, geobaseIds, commonObjectPayload)), value(creditProduct))

        val creditProductControllerLayer =
          creditProductDictionaryMock ++ creditProductEnricherMock >>> CreditProductController.live

        CreditProductController
          .list(filter, geobaseIds, commonObjectPayload)
          .map { result =>
            assertTrue(result.size == 1, result.head == creditProduct)
          }
          .provideLayer(creditProductControllerLayer)
      },
      testM("empty list") {

        val geobaseIds: Seq[GeobaseId] = Seq.empty
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()
        val filter = CreditProductDictionary.FilterByIds(Seq(creditProduct.id))

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.List(equalTo(filter), value(Seq.empty))

        val creditProductControllerLayer =
          CreditProductEnricherMock.empty ++ creditProductDictionaryMock >>> CreditProductController.live

        CreditProductController
          .list(filter, geobaseIds, commonObjectPayload)
          .map { result =>
            assertTrue(result.isEmpty)
          }
          .provideLayer(creditProductControllerLayer)
      }
    )

  private lazy val listFullEnrichSuite: ZSpec[Any, Throwable] =
    suite("list enriched with full")(
      testM("not empty list") {

        val geobaseIds = Seq(MoscowRegionId, SpbRegionId, CrimeaRegionId)
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()
        val filter = CreditProductDictionary.FilterByIds(Seq(creditProduct.id))

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.List(equalTo(filter), value(Seq(creditProduct)))

        val creditProductEnricherMock =
          CreditProductEnricherMock
            .Enrich(equalTo((creditProduct, geobaseIds, commonObjectPayload)), value(creditProduct))

        val creditProductControllerLayer =
          creditProductDictionaryMock ++ creditProductEnricherMock >>> CreditProductController.live

        CreditProductController
          .listFullOnly(filter, geobaseIds, commonObjectPayload)
          .map { result =>
            assertTrue(result.size == 1, result.head == creditProduct)
          }
          .provideLayer(creditProductControllerLayer)
      },
      testM("empty list") {

        val geobaseIds: Seq[GeobaseId] = Seq.empty
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()
        val filter = CreditProductDictionary.FilterByIds(Seq(creditProduct.id))

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.List(equalTo(filter), value(Seq(creditProduct)))

        val creditProductEnricherMock =
          CreditProductEnricherMock
            .Enrich(equalTo((creditProduct, geobaseIds, commonObjectPayload)), value(sampleCreditProductStub()))

        val creditProductControllerLayer =
          creditProductDictionaryMock ++ creditProductEnricherMock >>> CreditProductController.live

        CreditProductController
          .listFullOnly(filter, geobaseIds, commonObjectPayload)
          .map { result =>
            assertTrue(result.isEmpty)
          }
          .provideLayer(creditProductControllerLayer)
      }
    )

  private lazy val getEnrichedSuite: ZSpec[Any, Throwable] =
    suite("get enriched")(
      testM("existing credit product") {

        val geobaseIds = Seq(MoscowRegionId, SpbRegionId, CrimeaRegionId)
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.GetOrFail(equalTo(creditProduct.id), value(creditProduct))

        val creditProductEnricherMock =
          CreditProductEnricherMock
            .Enrich(equalTo((creditProduct, geobaseIds, commonObjectPayload)), value(creditProduct))

        val creditProductControllerLayer =
          creditProductDictionaryMock ++ creditProductEnricherMock >>> CreditProductController.live

        CreditProductController
          .get(creditProduct.id, geobaseIds, commonObjectPayload)
          .map { result =>
            assertTrue(result == creditProduct)
          }
          .provideLayer(creditProductControllerLayer)
      },
      testM("not found credit product") {

        val geobaseIds = Seq(MoscowRegionId, SpbRegionId, CrimeaRegionId)
        val commonObjectPayload = sampleOffer().some.map(_.commonObjectPayload)

        val creditProduct = sampleCreditProduct()
        val exception = new IllegalStateException("No credit product")

        val creditProductDictionaryMock =
          CreditProductDictionaryMock.GetOrFail(equalTo(creditProduct.id), failure(exception))

        val creditProductEnricherMock =
          CreditProductEnricherMock
            .Enrich(equalTo((creditProduct, geobaseIds, commonObjectPayload)), value(creditProduct))

        val creditProductControllerLayer =
          creditProductDictionaryMock ++ creditProductEnricherMock >>> CreditProductController.live

        val res: Task[CreditProduct] = CreditProductController
          .get(creditProduct.id, geobaseIds, commonObjectPayload)
          .provideLayer(creditProductControllerLayer)

        assertM(res.run)(fails(isSubtype[IllegalStateException](Assertion.anything)))
      }
    )

  private def sampleCreditProduct(): ConsumerCreditProduct =
    generate[ConsumerCreditProduct].sample.get
      .copy(rateLimit = None)

  private def sampleCreditProductStub(): DealerCreditProductStub =
    generate[DealerCreditProductStub].sample.get

  private def sampleOffer(): AutoruCreditApplication.Offer =
    generate[AutoruCreditApplication.Offer].sample.get
}

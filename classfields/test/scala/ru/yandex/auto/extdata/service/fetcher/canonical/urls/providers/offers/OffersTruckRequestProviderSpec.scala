package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.offers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.eds.service.trucks.TrucksCatalogGroupingService
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType}
import ru.yandex.auto.extdata.service.canonical.router.model.Params.{
  CategoryParam,
  MarkParam,
  ModelParam,
  SaleHashParam,
  SaleIdParam,
  SectionParam
}
import ru.yandex.auto.message.TrucksOffersSchema.TruckAdMessage

@RunWith(classOf[JUnitRunner])
class OffersTruckRequestProviderSpec
    extends OffersRequestProviderSpecBase[TruckAdMessage]
    with OffersTruckAndMotoCommon {
  private val offers = Seq(
    TruckAdMessage
      .newBuilder()
      .setVersion(1)
      .setStateKey(StateKeyNew)
      .setAutoCathegory(Category)
      .setId(autoruId(OfferId1))
      .setAutoruHashCode(OfferHash1)
      .setVerbaMarkCode(Mark1)
      .setVerbaModelCode(Model1)
      .build(),
    TruckAdMessage
      .newBuilder()
      .setVersion(1)
      .setStateKey(StateKeyBeaten)
      .setAutoCathegory(Category)
      .setId(autoruId(OfferId2))
      .setAutoruHashCode(OfferHash2)
      .build()
  )

  private def trucksCatalogGroupingService = {
    val service = mock[TrucksCatalogGroupingService]
    when(service.findType(?)).thenReturn(None)

    service
  }

  private val provider = new OffersTruckRequestProvider(
    trucksCatalogGroupingService,
    preparedQuery(offers)
  )

  "OffersTruckRequestProvider" should {
    "correctly generate requests" in {

      val expected = Seq(
        CanonicalUrlRequest(CanonicalUrlRequestType.Card)
          .withParam(new SectionParam("new"))
          .withParam(new CategoryParam(Category))
          .withParam(new SaleIdParam(OfferId1))
          .withParam(new SaleHashParam(OfferHash1))
          .withParam(new ModelParam(Model1))
          .withParam(new MarkParam(Mark1)),
        CanonicalUrlRequest(CanonicalUrlRequestType.CardOld)
          .withParam(new SectionParam("used"))
          .withParam(new CategoryParam(Category))
          .withParam(new SaleIdParam(OfferId2))
          .withParam(new SaleHashParam(OfferHash2))
      )

      provider.get().toSeq should contain theSameElementsAs expected
    }
  }
}

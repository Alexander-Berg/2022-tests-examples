package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.offers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.eds.service.moto.MotoCatalogGroupingService
import ru.yandex.auto.extdata.service.canonical.router.model.Params.{
  CategoryParam,
  MarkParam,
  ModelParam,
  SaleHashParam,
  SaleIdParam,
  SectionParam
}
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType}
import ru.yandex.auto.message.MotoOffersSchema.MotoAdMessage

@RunWith(classOf[JUnitRunner])
class OffersMotoRequestProviderSpec extends OffersRequestProviderSpecBase[MotoAdMessage] with OffersTruckAndMotoCommon {

  private val offers = Seq(
    MotoAdMessage
      .newBuilder()
      .setVersion(1)
      .setStateKey(StateKeyNew)
      .setCategory(Category)
      .setId(autoruId(OfferId1))
      .setAutoruHashCode(OfferHash1)
      .setMarkVerbaCode(Mark1)
      .setModelVerbaCode(Model1)
      .build(),
    MotoAdMessage
      .newBuilder()
      .setVersion(1)
      .setStateKey(StateKeyBeaten)
      .setCategory(Category)
      .setId(autoruId(OfferId2))
      .setAutoruHashCode(OfferHash2)
      .build()
  )

  private def motoCatalogGroupingService = {
    val service = mock[MotoCatalogGroupingService]
    when(service.getTypeByCode).thenReturn((_: String) => None)

    service
  }

  private val provider = new OffersMotoRequestProvider(
    motoCatalogGroupingService,
    preparedQuery(offers)
  )

  "OffersMotoRequestProvider" should {
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

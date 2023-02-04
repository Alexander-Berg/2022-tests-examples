package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.listing

import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.DummyPreparedQueryProvider
import ru.yandex.auto.core.catalog.model.trucks.TrucksTypeImpl
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.eds.service.trucks.TrucksCatalogGroupingService
import ru.yandex.auto.extdata.service.canonical.router.model
import ru.yandex.auto.message.TrucksOffersSchema.TruckAdMessage
import ru.yandex.auto.sitemap.SitemapRegion
import ru.yandex.extdata.core.lego.Provider

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TruckListingRequestProviderSpec extends BaseListingRequestProviderSpec[TruckAdMessage] {
  override protected def providerFromMessages(
      messages: Seq[TruckAdMessage]
  ): Provider[Seq[model.CanonicalUrlRequest]] = {
    val groupingService = mock[TrucksCatalogGroupingService]

    def mockedType(code: String): Option[TrucksTypeImpl] = {
      val res = mock[TrucksTypeImpl]
      when(res.getCode).thenReturn(code)

      Some(res).filter(_ => AllowedCategories.contains(code))
    }

    when(groupingService.findType(?)).thenAnswer(new Answer[Option[TrucksTypeImpl]] {
      override def answer(invocation: InvocationOnMock): Option[TrucksTypeImpl] =
        mockedType(invocation.getArgument[String](0))
    })

    new TruckListingRequestProvider(
      groupingService,
      new DummyPreparedQueryProvider(messages).get(),
      mock[SitemapRegion]
    ) {
      override protected def allowedRegions: Set[String] = AllowedRegions
    }

  }

  override protected def messageByCategory(categoryCode: String): TruckAdMessage =
    TruckAdMessage
      .newBuilder()
      .setVersion(1)
      .setAutoCathegory(categoryCode)
      .setIsRevoked(false)
      .build()

  override protected def messageWithState(state: State.Search, msg: TruckAdMessage): TruckAdMessage =
    msg.toBuilder.setStateKey(state.name()).build()

  override protected def messageWithMark(mark: String, msg: TruckAdMessage): TruckAdMessage =
    msg.toBuilder.setVerbaMarkCode(mark).build()

  override protected def messageWithModel(model: String, msg: TruckAdMessage): TruckAdMessage =
    msg.toBuilder.setVerbaModelCode(model).build()

  override protected def messageWithRegions(msg: TruckAdMessage, regionCodes: String*): TruckAdMessage =
    msg.toBuilder.addAllAllRegionCodes(regionCodes.asJava).build()
}

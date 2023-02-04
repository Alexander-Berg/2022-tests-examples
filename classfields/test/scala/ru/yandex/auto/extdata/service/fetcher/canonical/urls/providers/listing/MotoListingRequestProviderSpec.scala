package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.listing

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.DummyPreparedQueryProvider
import ru.yandex.auto.core.catalog.model.moto.MotoCategoryImpl
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.eds.service.moto.MotoCatalogGroupingService
import ru.yandex.auto.extdata.service.canonical.router.model
import ru.yandex.auto.message.MotoOffersSchema.MotoAdMessage
import ru.yandex.auto.sitemap.SitemapRegion
import ru.yandex.extdata.core.lego.Provider

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MotoListingRequestProviderSpec extends BaseListingRequestProviderSpec[MotoAdMessage] {

  override protected def providerFromMessages(
      messages: Seq[MotoAdMessage]
  ): Provider[Seq[model.CanonicalUrlRequest]] = {
    def mockedCategory(code: String): Option[MotoCategoryImpl] = {
      AllowedCategories.find(code == _).map { c =>
        val res = mock[MotoCategoryImpl]
        when(res.getCode).thenReturn(c)
        res
      }
    }

    val groupingService = mock[MotoCatalogGroupingService]
    //noinspection ConvertibleToMethodValue
    when(groupingService.getTypeByCode).thenReturn(mockedCategory(_))

    new MotoListingRequestProvider(groupingService, new DummyPreparedQueryProvider(messages).get(), mock[SitemapRegion]) {
      override protected def allowedRegions: Set[String] = AllowedRegions
    }
  }

  override protected def messageByCategory(categoryCode: String): MotoAdMessage =
    MotoAdMessage
      .newBuilder()
      .setVersion(1)
      .setCategory(categoryCode)
      .setIsRevoked(false)
      .build()

  override protected def messageWithState(state: State.Search, msg: MotoAdMessage): MotoAdMessage =
    msg.toBuilder.setStateKey(state.name()).build()

  override protected def messageWithMark(mark: String, msg: MotoAdMessage): MotoAdMessage =
    msg.toBuilder.setMarkVerbaCode(mark).build()

  override protected def messageWithModel(model: String, msg: MotoAdMessage): MotoAdMessage =
    msg.toBuilder.setModelVerbaCode(model).build()

  override protected def messageWithRegions(msg: MotoAdMessage, regionCodes: String*): MotoAdMessage =
    msg.toBuilder.addAllAllRegionCodes(regionCodes.asJava).build()
}

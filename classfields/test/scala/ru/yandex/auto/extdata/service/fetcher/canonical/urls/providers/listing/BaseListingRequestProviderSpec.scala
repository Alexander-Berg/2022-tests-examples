package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.listing

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.DummyPreparedQueryProvider
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.extdata.service.canonical.router.model.Params.{CategoryParam, MarkParam, ModelParam, SectionParam}
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType}
import ru.yandex.auto.index.consumer.PreparedQuery
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.ProviderUtils._

trait BaseListingRequestProviderSpec[M] extends WordSpec with Matchers with MockitoSupport {
  protected val MskGeoId: String = "213"
  protected val AllowedRegions: Set[String] = Set(MskGeoId)
  protected val AllowedCategories: Seq[String] = Seq("cat1") //, "cat2", "cat3")

  private val Mark = "mark"
  private val Model = "model"

  protected def preparedQuery(messages: Seq[M]): PreparedQuery[M] =
    new DummyPreparedQueryProvider[M](messages).get()

  protected def providerFromMessages(messages: Seq[M]): Provider[Seq[CanonicalUrlRequest]]

  protected def messageByCategory(categoryCode: String): M
  protected def messageWithState(state: Search, msg: M): M
  protected def messageWithMark(mark: String, msg: M): M
  protected def messageWithModel(model: String, msg: M): M
  protected def messageWithRegions(msg: M, regionCodes: String*): M

  private val stateParams = Seq(SectionParam(Search.NEW), SectionParam(Search.USED), SectionParam.All)

  "ListingProvider" should {
    "correctly build base requests" in {
      val messages = AllowedCategories
        .map(messageByCategory)
        .flatMap(m => Search.values().toSeq.map(messageWithState(_, m)))
        .map(messageWithRegions(_, MskGeoId))

      val expected =
        AllowedCategories
          .map(c => CanonicalUrlRequest(CanonicalUrlRequestType.ListingType, Set(new CategoryParam(c))))
          .flatMap(r => stateParams.map(r.withParam))
          .map(_.withRegionCodeParam(MskGeoId))

      providerFromMessages(messages).get().toSeq should contain theSameElementsAs expected
    }

    "correctly build requests with mark" in {
      val messages = AllowedCategories
        .map(messageByCategory)
        .flatMap(m => Search.values().toSeq.map(messageWithState(_, m)))
        .map(messageWithRegions(_, MskGeoId))
        .map(messageWithMark(Mark, _))

      val base =
        AllowedCategories
          .map(c => CanonicalUrlRequest(CanonicalUrlRequestType.ListingType, Set(new CategoryParam(c))))
          .flatMap(r => stateParams.map(r.withParam))
          .map(_.withRegionCodeParam(MskGeoId))

      val withMark = base.map(_.withParam(new MarkParam(Mark)))
      val expected = base ++ withMark

      providerFromMessages(messages).get().toSeq should contain theSameElementsAs expected
    }

    "correctly build requests with mark and model" in {
      val messages = AllowedCategories
        .map(messageByCategory)
        .flatMap(m => Search.values().toSeq.map(messageWithState(_, m)))
        .map(messageWithRegions(_, MskGeoId))
        .map(messageWithMark(Mark, _))
        .map(messageWithModel(Model, _))

      val base =
        AllowedCategories
          .map(c => CanonicalUrlRequest(CanonicalUrlRequestType.ListingType, Set(new CategoryParam(c))))
          .flatMap(r => stateParams.map(r.withParam))
          .map(_.withRegionCodeParam(MskGeoId))

      val withMark = base.map(_.withParam(new MarkParam(Mark)))
      val withModel = withMark.map(_.withParam(new ModelParam(Model)))
      val expected = base ++ withMark ++ withModel

      providerFromMessages(messages).get().toSeq should contain theSameElementsAs expected
    }

    "build only base if only model present" in {
      val messages = AllowedCategories
        .map(messageByCategory)
        .flatMap(m => Search.values().toSeq.map(messageWithState(_, m)))
        .map(messageWithRegions(_, MskGeoId))
        .map(messageWithModel(Model, _))

      val expected =
        AllowedCategories
          .map(c => CanonicalUrlRequest(CanonicalUrlRequestType.ListingType, Set(new CategoryParam(c))))
          .flatMap(r => stateParams.map(r.withParam))
          .map(_.withRegionCodeParam(MskGeoId))

      providerFromMessages(messages).get().toSeq should contain theSameElementsAs expected
    }

    "do nothing for unknown regions" in {
      val messages = AllowedCategories
        .map(messageByCategory)
        .flatMap(m => Search.values().toSeq.map(messageWithState(_, m)))
        .map(messageWithRegions(_, "-1"))

      providerFromMessages(messages).get().toSeq shouldBe empty
    }
  }

}

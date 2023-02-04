package ru.yandex.realty.traffic.service.urls

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.RequestParameter.RoomsValue.OneRoom
import ru.yandex.realty.canonical.base.params.RequestParameter.{
  Category,
  NewFlat,
  Rgid,
  RoomsTotal,
  SiteId,
  SiteName,
  Type
}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import zio.test.{assertTrue, ZSpec}
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class ThumbsExperimentSpec extends JUnitRunnableSpec {

  val offers: Seq[Request] =
    for {
      requestType <- Seq(RequestType.Search, RequestType.NewbuildingSearch)
      rgid <- Seq(NodeRgid.MOSCOW, NodeRgid.SVERDLOVSKAYA_OBLAST)
      typ <- Seq(OfferType.SELL, OfferType.RENT)
      category <- Seq(CategoryType.APARTMENT, CategoryType.COMMERCIAL)
      siteId <- Seq(None, Some(SiteId(1L)))
      siteName <- Seq(None, Some(SiteName("ЖК")))
      newFlat <- Seq(None, Some(NewFlat(true)))
      rooms <- Seq(None, Some(RoomsTotal(OneRoom)))
    } yield Request.Raw(
      requestType,
      Seq(Rgid(rgid), Type(typ), Category(category)) ++ siteId.toSeq ++ siteName.toSeq ++ newFlat.toSeq ++ rooms.toSeq
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ThumbsExperiment")(
      test("hasAppropriateRgid") {
        val res = offers.filter(ThumbsExperiment.hasAppropriateRgid)
        assertTrue(res.size == 128)
      },
      test("isSiteOffer") {
        val res = offers.filter(ThumbsExperiment.isSiteOffer)
        assertTrue(res.size == 4)
      },
      test("isSiteOfferFromDeveloper") {
        val res = offers.filter(ThumbsExperiment.isSiteOfferFromDeveloper)
        assertTrue(res.size == 4)
      },
      test("isExperiment") {
        val res = offers.filter(ThumbsExperiment.isExperiment)
        println("isExperiment")
        assertTrue(res.size == 2)
      }
    )

}

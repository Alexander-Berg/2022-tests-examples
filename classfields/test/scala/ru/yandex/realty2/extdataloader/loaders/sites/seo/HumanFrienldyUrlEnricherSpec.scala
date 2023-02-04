package ru.yandex.realty2.extdataloader.loaders.sites.seo

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.canonical.base.params.RequestParameter.{Id, Rgid, SiteName, Type}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.clients.router.FrontendRouterClient
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.urls.router.model.{RouterUrlRequest, RouterUrlResponse, ViewType}

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class HumanFrienldyUrlEnricherSpec extends SpecBase {

  implicit private val traced: Traced = Traced.empty

  "HumanFrienldyUrlEnricher" should {
    "set correct human friendly url" in {
      val routerClient = mock[FrontendRouterClient]
      val enricher = new HumanFrienldyUrlEnricher(routerClient)

      val site = new Site(1214L)
      site.setName("Имя ЖК")
      val location = new Location()
      location.setSubjectFederation(Regions.MOSCOW, NodeRgid.MOSCOW)
      site.setLocation(location)

      val request = RouterUrlRequest(
        Request.Raw(
          RequestType.Newbuilding,
          Seq(
            Id(site.getId),
            SiteName(site.getName),
            Rgid(site.getLocation.getSubjectFederationRgid),
            Type(OfferType.SELL)
          )
        ),
        ViewType.Desktop
      )

      val resultUrl = "/moskva/kupit/novostrojka/imya-zhk-1214/"

      (routerClient
        .batchBuildUrl(_: Iterable[RouterUrlRequest])(_: Traced))
        .expects(Set(request), *)
        .returning(Future.successful(Seq(RouterUrlResponse(request, Some(resultUrl)))))

      enricher.setHumanFriendlyUrl(Seq(site).asJava)(Traced.empty)

      site.getHumanFriendlyUrl shouldBe resultUrl
    }
  }
}

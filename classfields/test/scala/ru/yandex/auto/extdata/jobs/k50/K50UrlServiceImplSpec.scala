package ru.yandex.auto.extdata.jobs.k50

import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.eds.service.CanonicalUrlService
import ru.yandex.auto.extdata.jobs.k50.services.impl.K50UrlServiceImpl
import ru.yandex.auto.extdata.service.canonical.router.model.Params.{CategoryParam, SectionParam}
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType, Params}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.auto.extdata.service.util.MockitoSyntax._

class K50UrlServiceImplSpec extends FlatSpec with MockitoSupport with Matchers {

  private val geoId = 210
  private val mark = "bmw"
  private val model = "320"

  private val canonicalUrlService = mock[CanonicalUrlService]
  private val request = CanonicalUrlRequest(
    CanonicalUrlRequestType.ListingType,
    Set(new Params.GeoIdParam(geoId), new Params.MarkParam(mark))
  ).withParam(CategoryParam.Cars)
  private val newModelRequest = request.withParam(new Params.ModelParam(model)).withParam(SectionParam.New)
  private val usedModelRequest = request.withParam(new Params.ModelParam(model)).withParam(SectionParam.Used)
  private val usedMarkRequest = request.withParam(SectionParam.Used)

  when(canonicalUrlService.getCanonical(newModelRequest)).answer { _ =>
    Some("good url for new model")
  }
  when(canonicalUrlService.getCanonical(usedModelRequest)).answer { _ =>
    Some("good url for used model")
  }
  when(canonicalUrlService.getCanonical(usedMarkRequest)).answer { _ =>
    Some("good url for used mark")
  }

  "K50UrlServiceImpl" should "return correct url" in {
    val urlService = new K50UrlServiceImpl(canonicalUrlService)
    urlService.markModelListing(geoId, mark, Some(model), Search.NEW) shouldBe Some("good url for new model")
    urlService.markModelListing(geoId, mark, Some(model), Search.USED) shouldBe Some("good url for used model")
    urlService.markModelListing(geoId, mark, None, Search.USED) shouldBe Some("good url for used mark")
  }
}

package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.TimeLimits
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.service.canonical.router.RouterRequestsProvider
import ru.yandex.auto.extdata.service.canonical.router.model.CanonicalUrlRequestType._
import ru.yandex.auto.extdata.service.canonical.router.model.Params._
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, RequestParam}

@RunWith(classOf[JUnitRunner])
class ProviderWithMobileUrlsSpec extends FlatSpecLike with Matchers with TimeLimits {
  "Provider with mobile urls" should "add IsMobileParam(true) to each request in initial provider" in failAfter(
    100 millis
  ) {
    val defaultProvider = new RandomRequestsProvider()
    val providerWithMobileParam = new RandomRequestsProvider with ProviderWithMobileUrls
    val defaultRequests = defaultProvider.get()
    val requestsWithMobile = providerWithMobileParam.get()

    requestsWithMobile.size shouldEqual defaultRequests.size * 2
    requestsWithMobile.count(_.params.exists(_.name == "isMobile")) shouldEqual defaultRequests.size
  }

  private val reqTypeGen = Gen.oneOf(ListingType, CatalogType, UnofficialDealer, OfficialDealer)

  /**
    * Does not generate IsMobileParam
    */
  private val paramGen: Gen[RequestParam[_]] = for {
    intParam <- Gen.choose(0, 1000000)
    stringParam <- Gen.alphaLowerStr
    param <- Gen.oneOf(
      new CategoryParam(stringParam),
      new SectionParam(stringParam),
      new MarkParam(stringParam),
      new ModelParam(stringParam),
      new SuperGenParam(intParam),
      new ConfigurationParam(intParam),
      new DealerCodeParam(stringParam)
    )
  } yield param

  private val paramsGen: Gen[Set[RequestParam[_]]] = for {
    n <- Gen.choose(1, 4)
    params <- Gen.containerOfN[Set, RequestParam[_]](n, paramGen)
  } yield params

  private val requestGen: Gen[CanonicalUrlRequest] = for {
    requestType <- reqTypeGen
    params <- paramsGen
  } yield CanonicalUrlRequest(requestType, params)

  private val requestsGen: Gen[Seq[CanonicalUrlRequest]] = for {
    n <- Gen.choose(30, 100)
    requests <- Gen.containerOfN[Seq, CanonicalUrlRequest](n, requestGen)
  } yield requests

  private val requests = Stream.continually(requestsGen.sample).flatten.head

  /**
    * Generates different sets of requests on each restart, but identical inside 1 session
    * Used for testing puproses
    */
  class RandomRequestsProvider extends RouterRequestsProvider {
    override def get(): Seq[CanonicalUrlRequest] = {
      requests
    }

    override def name: String = ""
  }
}

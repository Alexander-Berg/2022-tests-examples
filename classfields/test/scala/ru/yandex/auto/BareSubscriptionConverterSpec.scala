package ru.yandex.auto

import org.junit.{Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.auto.api.ApiOfferModel
import ru.yandex.auto.clone.unifier.processor.JavaProcessor
import ru.yandex.auto.converters.OfferToSubscriptionConverter
import ru.yandex.auto.core.model.{AbstractAd, CarAd, UnifiedCarInfo}
import ru.yandex.auto.core.region.RegionService
import ru.yandex.auto.core.search2.conversion.yocto.YoctoFunctionalSubscriptionsEntityConverter
import ru.yandex.auto.core.search2.conversion.yocto.util.SimpleNameValueDocumentBuilder
import ru.yandex.auto.core.stock.http.Currency
import ru.yandex.auto.searcher.search.api.cars.ApiSearchResultBuilder
import ru.yandex.auto.searcher.search.grouphandler.CarAdMessageWrapper
import ru.yandex.auto.utils.{CarAdConverter, JsonFormat}
import ru.yandex.common.util.functional.Functiable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.convert.decorateAsScala._
import scala.concurrent.Await
import scala.io.Source.fromInputStream
import scala.concurrent.duration._

@Ignore
@ContextConfiguration(locations = Array("/context/shard-unifier-bare-test.xml"))
class BareSubscriptionConverterSpec extends AbstractJUnit4SpringContextTests with SimpleAssertions {

  @Autowired implicit var apiOfferBuilder: ApiSearchResultBuilder = _
  @Autowired implicit var regionService: RegionService = _
  @Autowired var processor: JavaProcessor = _

  private val yoctoSubscriptionConverter: Functiable[AbstractAd, SimpleNameValueDocumentBuilder] =
    new YoctoFunctionalSubscriptionsEntityConverter(classOf[CarAd])

  private lazy val offerToSubscriptionConverter =
    new OfferToSubscriptionConverter(processor, yoctoSubscriptionConverter)

  private def read(name: String) = {
    val s = getClass.getResourceAsStream("/offers/" + name)
    val string = fromInputStream(s).mkString
    val builder = ApiOfferModel.Offer.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(string, builder)
    builder.build()
  }

  @Ignore
  @Test
  def test() {
    val apiOffer = read("offer_no_id.json")
    val unifiedOffer = offerToSubscriptionConverter.unifyAndProcess(apiOffer)
    assert(apiOffer.toString, ConvertUnifiedOffer.toApi(unifiedOffer).toString)
  }

  @Test
  def testConverter() {
    val documentId = "123"
    val apiOffer = read("offer_VAZ_2107.json")
    val eventualDocument = offerToSubscriptionConverter
      .convert(documentId, apiOffer)

    val document = Await.result(eventualDocument, 2.minutes)

    println("document " + document)
    assert(document.getId, documentId)
    assert(document.getTermList.asScala.find(_.getName == "pl_lat").map(_.getPoint.getValue).get, "55753216")
    assert(document.getTermList.asScala.find(_.getName == "pl_lon").map(_.getPoint.getValue).get, "37622505")
    assert(document.getTermList.asScala.find(_.getName == "state").map(_.getPoint.getValue).get, "USED")
    assert(document.getTermList.asScala.find(_.getName == "year").map(_.getPoint.getValue).get, "2011")
    assert(document.getTermList.asScala.find(_.getName == "run").map(_.getPoint.getValue).get, "126800")
    assert(document.getTermList.asScala.filter(_.getName == "vendor").map(_.getPoint.getValue), Seq("11", "1"))

  }

}

object ConvertUnifiedOffer {

  def toApi(info: UnifiedCarInfo)(implicit builder: ApiSearchResultBuilder): ApiOfferModel.Offer = {
    val msg = new CarAdMessageWrapper(CarAdConverter.toCarAd(info)(identity).toMessage)
    builder.buildOffer(msg, Currency.RUR)
  }
}

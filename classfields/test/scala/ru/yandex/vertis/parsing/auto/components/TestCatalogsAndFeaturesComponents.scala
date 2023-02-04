package ru.yandex.vertis.parsing.auto.components

import akka.actor.TypedActor.dispatcher
import ru.yandex.vertis.parsing.auto.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.auto.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.clients.callcenterHelperApi.{CallCenterHelperClient, OfferUpload, OfferUploadDataElem}
import ru.yandex.vertis.parsing.components.TestApplicationSupport
import ru.yandex.vertis.parsing.components.extdata.{CatalogsSupport, TestExtDataSupport}
import ru.yandex.vertis.parsing.components.io.IOSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
object TestCatalogsAndFeaturesComponents
  extends TestApplicationSupport
  with IOSupport
  with TestExtDataSupport
  with CatalogsSupport
  with TestBunkerConfigSupport
  with ParsersSupport
  with TestFeaturesSupport
  with CallCenterHelperClient {

  override def getBatchNewUrlForAddOffersCallCenter(
      offerUploadList: List[OfferUpload]
  )(implicit trace: Traced): Future[List[OfferUploadDataElem]] =
    Future(
      List(OfferUploadDataElem(hash = "1234", url = Some("https/testUrl"), error = Some("Not error"), success = true))
    )
}

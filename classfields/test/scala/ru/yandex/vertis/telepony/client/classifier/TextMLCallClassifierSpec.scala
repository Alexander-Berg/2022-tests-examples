package ru.yandex.vertis.telepony.client.classifier

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.service.Classifier
import ru.yandex.vertis.telepony.service.impl.classifier.TextMLCallClassifier

/**
  * @author neron
  */
trait TextMLCallClassifierSpec extends SpecBase with SimpleLogging {

  def client: TextMLCallClassifier

  "TextMLCallClassifier" should {
    "classify" in {
      val response = client.classify(Classifier.Text("карпрайс")).futureValue
      log.info(s"$response")
    }
  }

}

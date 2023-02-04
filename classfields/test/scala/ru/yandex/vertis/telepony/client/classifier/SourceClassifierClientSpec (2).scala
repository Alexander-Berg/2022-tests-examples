package ru.yandex.vertis.telepony.client.classifier

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.RefinedSource
import ru.yandex.vertis.telepony.model.classifier.SourceClassifierRequest
import ru.yandex.vertis.telepony.service.impl.classifier.{Classifiers, SourceClassifierClient}

/**
  * @author neron
  */
trait SourceClassifierClientSpec extends SpecBase with SimpleLogging {

  def client: SourceClassifierClient

  "SourceClassifierClient" should {
    "not ban caller that having one call" in {
      val call = Generator.SourceClassifierCallGen.next
//      val blockedCall = Generator.SourceClassifierBlockedCallGen.next
//      val unmatchedCall = Generator.SourceClassifierUnmatchedGen.next
      val req = SourceClassifierRequest(
        RefinedSource("+79817757575"),
        Seq(call),
        Seq(),
        Seq()
      )
      val response = client.checkSource(req, Classifiers.GoodRecallClassifier).futureValue
      log.info(s"RESPONSE: $response")
    }

  }

}

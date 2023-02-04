package ru.yandex.vertis.telepony.client.classifier
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.service.Classifier
import ru.yandex.vertis.telepony.service.impl.classifier.TextMLCallClassifier
import ru.yandex.vertis.telepony.service.logging.LoggingClassifier
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder

/**
  * @author neron
  */
@Ignore
class TextMLCallClassifierImplIntSpec
  extends TestKit(ActorSystem("TextMLCallClassifierSpec"))
  with TextMLCallClassifierSpec {

  implicit val am = ActorMaterializer()
  implicit val ec = Threads.lightWeightTasksEc

  private val httpClient =
    new HttpClientImpl("default-spec-client", PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2), None)

  override def client: TextMLCallClassifier =
    new TextMLCallClassifier(
      httpClient = httpClient,
      requestPath = "http://quality-autoru-telepony-calls-int.vrts-slb.test.vertis.yandex.net/api/v1/predict"
    ) with LoggingClassifier[Classifier.Text, TextMLCallClassifier.Response]

}

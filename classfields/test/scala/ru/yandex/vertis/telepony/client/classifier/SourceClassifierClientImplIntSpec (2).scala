package ru.yandex.vertis.telepony.client.classifier

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.service.impl.SourceClassifierClientImpl
import ru.yandex.vertis.telepony.service.impl.classifier.SourceClassifierClient
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder

/**
  * @author neron
  */
class SourceClassifierClientImplIntSpec
  extends TestKit(ActorSystem("SourceClassifierClientSpec"))
  with SourceClassifierClientSpec {

  implicit val am = Materializer(system)
  implicit val ec = Threads.lightWeightTasksEc

  private val httpClient =
    new HttpClientImpl("default-spec-client", PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2), None)

  override def client: SourceClassifierClient =
    new SourceClassifierClientImpl(
      httpClient = httpClient,
      host = "quality-autoru-telepony-calls-and-callers-int.vrts-slb.test.vertis.yandex.net",
      port = 80
    )

}

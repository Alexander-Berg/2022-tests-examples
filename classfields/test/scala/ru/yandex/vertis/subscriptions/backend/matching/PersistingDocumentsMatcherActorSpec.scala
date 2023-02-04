package ru.yandex.vertis.subscriptions.backend.matching

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.{fixture, Matchers}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.MatcherApi
import ru.yandex.vertis.subscriptions.MatcherApi.Request.MatchDocuments
import ru.yandex.vertis.subscriptions.Model.DocumentsPortion
import ru.yandex.vertis.subscriptions.model.LegacyGenerators._
import ru.yandex.vertis.subscriptions.storage.DocumentsDao

import scala.collection.JavaConversions.asJavaIterable
import scala.concurrent.Future

class PersistingDocumentsMatcherActorSpec
  extends TestKit(ActorSystem("test", ConfigFactory.empty()))
  with ImplicitSender
  with Matchers
  with fixture.WordSpecLike
  with MockitoSupport {

  class FixtureParam {
    val dao = mock[DocumentsDao]
    val documents = documentGen.next(3)

    val request = MatcherApi.Request
      .newBuilder()
      .setId(idGen.next)
      .setService(serviceGen.next)
      .setMatchDocuments(
        MatchDocuments
          .newBuilder()
          .setDocumentsPortion(
            DocumentsPortion
              .newBuilder()
              .addAllDocuments(documents)
              .build()
          )
          .build()
      )
      .build()
  }

  override def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(new FixtureParam))
  }

  "PersistingDocumentsMatcherActor" should {

    "forward request only when documents should not be stored" in { env =>
      import env._

      val actor = system.actorOf(Props(new PersistingDocumentsMatcherActor(dao, testActor, false)))
      actor ! request

      expectMsg(request)
      verifyZeroInteractions(dao)
    }

    "send request to base matcher when documents were stored successfully" in { env =>
      pending
      import env._

      when(dao.store(?)).thenReturn(Future.successful())

      val actor = system.actorOf(Props(new PersistingDocumentsMatcherActor(dao, testActor, true)))
      actor ! request

      expectMsg(request)
    }

    "send respond with error to sender when failed to store documents" in { env =>
      pending
      import env._

      when(dao.store(?)).thenReturn(Future.failed(new RuntimeException("error")))

      val actor = system.actorOf(Props(new PersistingDocumentsMatcherActor(dao, ActorRef.noSender, true)))
      actor ! request

      expectMsgPF() {
        case resp: MatcherApi.Response =>
          resp.getRequestId == request.getId

        case _ => false
      }
    }
  }

}

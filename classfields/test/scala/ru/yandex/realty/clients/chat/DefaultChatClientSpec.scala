package ru.yandex.realty.clients.chat

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.generator.ChatApiGenerators._
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.request.ImplicitRequest
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class DefaultChatClientSpec
  extends AsyncSpecBase
  with ImplicitRequest
  with HttpClientMock
  with GeneratorDrivenPropertyChecks {

  import ChatApiMessageBuilders._
  import DefaultChatClient._

  protected val chatClient = new DefaultChatClient(httpService)

  implicit val trace: Traced = Traced.empty

  "ChatClient" should {
    "create room" in {

      forAll(set(0, 10, chatUserRefGen), roomIdGen, properties, room) { (users, roomId, props, result) =>
        val roomParameters = buildCreateRoomParameters(roomId, users.map(_.getId), props)

        httpClient.expect(POST, s"$routePrefix/rooms")
        httpClient.expectProto(roomParameters)

        httpClient.respondWith(StatusCodes.OK, result)

        val callResult = chatClient.createRoom(roomParameters).futureValue
        callResult shouldBe result
      }
    }

//    "get chat rooms" in {
//      forAll(chatUserRefGen, list(0, 10, room)) { (user, result) =>
//        httpClient.expect(
//          GET,
//          s"$routePrefix/rooms?$userQueryParameter=${user.toPlain}&$pageParameter=0&$pageSizeParameter=10"
//        )
//
//        httpClient.respondWith(StatusCodes.OK, result)
//
//        val callResult = chatClient.getRooms(user, Page(0, 10)).futureValue
//        callResult shouldBe result
//      }
//    }
  }
}

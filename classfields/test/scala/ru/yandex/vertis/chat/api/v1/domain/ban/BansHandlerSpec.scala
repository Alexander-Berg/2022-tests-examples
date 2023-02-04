package ru.yandex.vertis.chat.api.v1.domain.ban

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.chat.api.HandlerSpecBase

import ru.yandex.vertis.chat.components.dao.authority.{BanHistoryPage, JvmAuthorityService}
import ru.yandex.vertis.chat.api.v1.proto.ApiProtoFormats._
import ru.yandex.vertis.chat.components.time.TestTimeSupport
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.components.dao.authority.AuthorityGenerators.banScopeKnown
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.service.impl.jvm.JvmChatState
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

class BansHandlerSpec extends HandlerSpecBase with ProducerProvider with TestTimeSupport {

  private val service = new JvmAuthorityService(JvmChatState.empty(), timeService)
  private val route = seal(new BansHandler(service).route)
  import BansHandler._

  s"GET $root/history" should {
    "return full user ban history" in {
      val user: UserId = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, Some("perm"), None).futureValue
      tickTime
      service.clearBan(user, scope, operator, Some("clear")).futureValue
      tickTime
      service.ban(user, scope, operator, Some("tmp"), Some(timeService.getNow))
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root/bans/history")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val answer = responseAs[BanHistoryPage]
          answer should be(service.getBansHistory(None, None, 1, 10).futureValue)
          answer.bans should be(answer.bans.sorted)
        }
      }
    }
  }

  s"GET $root/history" should {
    "return ban history by initiator" in {
      val user: UserId = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, Some("perm"), None).futureValue
      tickTime
      service.clearBan(user, scope, operator, Some("clear")).futureValue
      tickTime
      service.ban(user, scope, operator, Some("tmp"), Some(timeService.getNow))
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root/bans/history")
          .withUser(operator)
          .withSomePassportUser
          .withQueryParam(InitiatorParam, operator)
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val answer = responseAs[BanHistoryPage]
          answer should be(service.getBansHistory(None, Some(operator), 1, 10).futureValue)
          answer.bans should be(answer.bans.sorted)
        }
      }
    }
  }

  s"GET $root/history" should {
    "return particular user bans history" in {
      val user: UserId = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, Some("perm"), None).futureValue
      tickTime
      service.clearBan(user, scope, operator, Some("clear")).futureValue
      tickTime
      service.ban(user, scope, operator, Some("tmp"), Some(timeService.getNow))
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root/bans/history")
          .withUser(operator)
          .withSomePassportUser
          .withQueryParam(UserParam, user)
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val answer = responseAs[BanHistoryPage]
          answer should be(service.getBansHistory(Some(user), None, 1, 10).futureValue)
          answer.bans should be(answer.bans.sorted)
        }
      }
    }
  }

  s"GET $root/history" should {
    "return user bans by particular initiator" in {
      val user: UserId = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, Some("perm"), None).futureValue
      tickTime
      service.clearBan(user, scope, operator, Some("clear")).futureValue
      tickTime
      service.ban(user, scope, operator, Some("tmp"), Some(timeService.getNow))
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root/bans/history")
          .withUser(operator)
          .withSomePassportUser
          .withQueryParam(InitiatorParam, operator)
          .withQueryParam(UserParam, user)
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val answer = responseAs[BanHistoryPage]
          answer should be(service.getBansHistory(Some(user), Some(operator), 1, 10).futureValue)
          answer.bans should be(answer.bans.sorted)
        }
      }
    }
  }

  implicit val intToStr: Int => String = _.toString

  s"GET $root/history" should {
    "pagination test" in {
      val user: UserId = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, Some("perm"), None).futureValue
      tickTime
      service.clearBan(user, scope, operator, Some("clear")).futureValue
      tickTime
      service.ban(user, scope, operator, Some("tmp"), Some(timeService.getNow))
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root/bans/history")
          .withUser(operator)
          .withSomePassportUser
          .withQueryParam(InitiatorParam, operator)
          .withQueryParam(UserParam, user)
          .withQueryParam("page", 3)
          .withQueryParam("page_size", 1)
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val answer = responseAs[BanHistoryPage]
          answer should be(service.getBansHistory(Some(user), Some(operator), 3, 1).futureValue)
          answer.bans should be(answer.bans.sorted)
        }
      }
    }
  }

}

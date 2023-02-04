package ru.yandex.vertis.chat.api.v1.domain.user

import akka.http.scaladsl.model.StatusCodes
import play.api.libs.json.Json
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.api.v1.proto.ApiProtoFormats._
import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.dao.authority.{BlockedUser, JvmAuthorityService}
import ru.yandex.vertis.chat.components.dao.authority.AuthorityGenerators._
import ru.yandex.vertis.chat.components.dao.chat.limits.UserRoomsAndMessagesStats
import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.security.BlockUserParameters
import ru.yandex.vertis.chat.components.dao.statistics.{InMemoryStatisticsService, InStorageStatisticsService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.time.TestTimeSupport
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.jvm.JvmChatState
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.generators.BasicGenerators._
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
import ru.yandex.vertis.chat.components.dao.authority.BanScope

/**
  * Runnable specs on [[UserHandler]].
  *
  * @author 747mmhg
  */
class UserHandlerSpec extends HandlerSpecBase with ProducerProvider with TestTimeSupport {

  private val state = JvmChatState.empty()

  private val statisticsService = new InStorageStatisticsService
    with DomainAutoruSupport
    with InMemoryStatisticsService
    with NopCacheMetricsImpl {
    override val chatStorage: DMap[ChatStorage] = DMap.forAllDomains(JvmStorage(state))
  }

  private val service = new JvmAuthorityService(JvmChatState.empty(), timeService)
  private val route = seal(new UserHandler(service, timeService, statisticsService).route)

  s"PUT $root<id>/ban" should {
    "ban user" in {
      val user = userId.next
      val operator = userId.next
      val comment = readableString.next
      supportedMediaTypes.foreach { mediaType =>
        Put(s"$root$user/ban?comment=$comment")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          service.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe true
        }
      }
    }

    "ban user temporary - this one should be unbanned" in {
      val user = userId.next
      val operator = userId.next
      val comment = readableString.next
      val till = timeService.getNow.plusHours(1)
      supportedMediaTypes.foreach { mediaType =>
        Put(s"$root$user/ban?comment=$comment")
          .withTill(till)
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          resetTime(till.plusHours(1))
          service.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe false
        }
      }
    }

    "ban user temporary - this one should be banned" in {
      val user = userId.next
      val operator = userId.next
      val comment = readableString.next
      val till = timeService.getNow.plusHours(1)
      supportedMediaTypes.foreach { mediaType =>
        Put(s"$root$user/ban?comment=$comment")
          .withTill(till)
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          service.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe true
        }
      }
    }

    "reject ban without operator id" in {
      val user = userId.next
      val comment = readableString.next
      supportedMediaTypes.foreach { mediaType =>
        Put(s"$root$user/ban?comment=$comment").withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.BadRequest)
        }
      }
    }

    "support scope" in {
      val user = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      supportedMediaTypes.foreach { mediaType =>
        Put(s"$root$user/ban?scope=${scope.asString}")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          service.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, scope) shouldBe true
        }
      }
    }
  }

  s"GET $root<id>/ban" should {
    "return false if the user is not banned" in {
      val user = userId.next
      val operator = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root$user/ban")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(false)
        }
      }
    }

    "return true if the user is banned" in {
      val user = userId.next
      val operator = userId.next
      service.ban(user, BanScope.AllUserChats, operator, None, None)
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root$user/ban")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(true)
        }
      }
    }

    "support scope" in {
      val user = userId.next
      val operator = userId.next
      val scope = banScopeKnown.next
      service.ban(user, scope, operator, None, None)
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root$user/ban?scope=${scope.asString}")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          responseAs[Boolean] should be(
            service.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, scope)
          )
        }
      }
    }
  }

  s"DELETE $root<id>/ban" should {
    "clear ban from user" in {
      val user = userId.next
      val operator = userId.next
      service.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      service.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      supportedMediaTypes.foreach { mediaType =>
        Delete(s"$root$user/ban")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val banStatus = service.getBanStatus(user).futureValue
          banStatus.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe false
          banStatus.isBannedAt(timeService.getNow, BanScope.SupportChat) shouldBe true
        }
      }
    }

    "support scope" in {
      val user = userId.next
      val operator = userId.next
      service.ban(user, BanScope.AllUserChats, operator, None, None).futureValue
      service.ban(user, BanScope.SupportChat, operator, None, None).futureValue
      supportedMediaTypes.foreach { mediaType =>
        Delete(s"$root$user/ban?scope=SUPPORT_CHAT")
          .withUser(operator)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val banStatus = service.getBanStatus(user).futureValue
          banStatus.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe true
          banStatus.isBannedAt(timeService.getNow, BanScope.SupportChat) shouldBe false
        }
      }
    }
  }

  s"PUT ${root}block" should {
    "blocks link between users" in {
      val parameters = blockUserParameters.next
      val blocked = parameters.user
      val user = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Put(s"${root}block", parameters)
          .withUser(user)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          service.isLinkBlocked(user, blocked).futureValue shouldBe true
        }
      }
    }
  }

  s"DELETE $root<id>/block" should {
    "remove block for user pair" in {
      val parameters = blockUserParameters.next
      val blocked = parameters.user
      val user = userId.next
      withUserContext(user) { rc =>
        service.block(parameters)(rc).futureValue
      }
      supportedMediaTypes.foreach { mediaType =>
        Delete(s"${root}block?user_id=$blocked")
          .withUser(user)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          service.isLinkBlocked(user, blocked).futureValue shouldBe false
        }
      }
    }
  }

  s"GET $root<id>/stats" should {
    "stats users" in {
      val user = userId.next
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root$user/stats")
          .withUser(user)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val userRoomsAndMessagesStats = Json.parse(responseAs[String]).as[UserRoomsAndMessagesStats]
          userRoomsAndMessagesStats shouldBe UserRoomsAndMessagesStats(0, 0)
        }
      }
    }
  }

  s"GET ${root}blocked" should {
    "list of all currently blocked users" in {
      val blocked1 = blockUserParameters.next
      val blocked2 = blockUserParameters.next
      val blocked3 = blockUserParameters.next
      val user = userId.next
      withUserContext(user) { rc =>
        service.block(blocked1)(rc).futureValue
        service.block(blocked2)(rc).futureValue
        service.block(blocked3)(rc).futureValue
        service.unblock(blocked1.user)(rc).futureValue
      }
      supportedMediaTypes.foreach { mediaType =>
        Get(s"${root}blocked")
          .withUser(user)
          .withSomePassportUser
          .accepting(mediaType) ~> route ~> check {
          status should be(StatusCodes.OK)
          val blocked = responseAs[Seq[BlockedUser]]
          blocked should have size 2
          checkBlocked(blocked2, blocked)
          checkBlocked(blocked3, blocked)
        }
      }
    }

    def checkBlocked(parameters: BlockUserParameters, blocked: Iterable[BlockedUser]): Unit = {
      val maybeUser = blocked.find(_.user == parameters.user)
      maybeUser shouldBe defined
      maybeUser.map(_.context) shouldBe Some(parameters.context)
    }
  }

  s"GET $root<id>/is_blocked" should {
    "retrieve current block status" in {
      val blocked = userId.next
      val user = userId.next
      withUserContext(user) { rc =>
        service.block(blockUserParameters(blocked).next)(rc).futureValue
      }
      checkIsBlocked(user, blocked, blocked = true)
      withUserContext(user) { rc =>
        service.unblock(blocked)(rc).futureValue
      }
      checkIsBlocked(user, blocked, blocked = false)
    }
  }

  def checkIsBlocked(by: UserId, who: UserId, blocked: Boolean): Unit = {
    supportedMediaTypes.foreach { mediaType =>
      Get(s"$root$who/is_blocked")
        .withUser(by)
        .withSomePassportUser
        .accepting(mediaType) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[Boolean] shouldBe blocked
      }
    }
  }
}

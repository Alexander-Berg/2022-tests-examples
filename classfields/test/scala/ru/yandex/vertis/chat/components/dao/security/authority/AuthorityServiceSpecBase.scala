package ru.yandex.vertis.chat.components.dao.security.authority

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.dao.authority.AuthorityService
import ru.yandex.vertis.chat.components.dao.security.BlockUserParameters
import ru.yandex.vertis.chat.components.time.TimeService
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.{UserId, Window}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.components.dao.authority.AuthorityGenerators.banScopeKnown
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceTestKit}
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.chat.components.dao.authority.BanScope

trait AuthorityServiceSpecBase extends SpecBase with ChatServiceTestKit with RequestContextAware with ProducerProvider {

  def authorityService: AuthorityService

  val timeService: TimeService

  def service: ChatService

  "Authority service" should {

    "ban user and clear ban from her" in {
      val banned = userId.next
      val scope = banScopeKnown.next
      authorityService.ban(banned, scope, userId.next, None, None).futureValue
      authorityService
        .getBanStatus(banned)
        .futureValue
        .isBannedAt(timeService.getNow, scope) should be(true)
      authorityService.clearBan(banned, scope, userId.next, None).futureValue
      authorityService
        .getBanStatus(banned)
        .futureValue
        .isBannedAt(timeService.getNow, scope) should be(false)
      val bandata = authorityService.getBansHistory(Some(banned), None, 1, 5).futureValue
      bandata.bans.size should be(2)
      bandata.pageCount should be(1)
      bandata.pageSize should be(5)
      bandata.page should be(1)
      bandata.bans.size should be(2)
      bandata.bans.foreach(_.scope should be(scope))
    }

    "block link in both directions" in {
      val from = userId.next
      val parameters = blockUserParameters.next
      withUserContext(from) { rc =>
        authorityService.block(parameters)(rc).futureValue
        val blocked = authorityService
          .listBlocked(Window[UserId](None, 10, asc = true))(rc)
          .futureValue
          .find(_.user == parameters.user)
        blocked shouldBe defined
        blocked.map(_.context) shouldBe Some(parameters.context)
      }
      checkMutuallyBlocked(from, parameters.user, blocked = true)
    }

    "unblock link in both directions" in {
      val from = userId.next
      val parameters = blockUserParameters.next
      withUserContext(from) { rc =>
        authorityService.block(parameters)(rc).futureValue
        authorityService.unblock(parameters.user)(rc).futureValue
      }
      checkMutuallyBlocked(from, parameters.user, blocked = false)
    }

    "properly lists blocked users" in {
      val from = userId.next
      val toIds = userId.next(10).toVector.sortBy(_.toLowerCase)
      val ctx = userBlockingContext.next
      withUserContext(from) { rc =>
        toIds.foreach(to => {
          authorityService.block(BlockUserParameters(to, ctx))(rc).futureValue
        })
      }

      val cases: Map[Window[UserId], Seq[UserId]] = Map(
        Window(None, 1, asc = true) -> toIds.take(1),
        Window(None, 1, asc = false) -> toIds.takeRight(1),
        Window(None, 2, asc = true) -> toIds.take(2),
        Window(None, 2, asc = false) -> toIds.takeRight(2),
        Window(Some(toIds.head), 1, asc = true) -> toIds.take(1),
        Window(Some(toIds.head), 1, asc = false) -> toIds.take(1),
        Window(Some(toIds.head), 10, asc = true) -> toIds,
        Window(Some(toIds(9)), 10, asc = false) -> toIds,
        Window(Some(toIds(1)), 1, asc = true) -> toIds.slice(1, 2),
        Window(Some(toIds(1)), 1, asc = false) -> toIds.slice(1, 2)
      )

      withUserContext(from) { rc =>
        cases.foreach {
          case (window, users) =>
            val result = authorityService
              .listBlocked(window)(rc)
              .futureValue
              .map(_.user)
            result shouldBe users
        }
      }

      toIds.foreach(to => {
        withUserContext(to) { rc =>
          authorityService
            .listBlocked(Window[UserId](None, 10, asc = true))(rc)
            .futureValue
            .map(_.user) shouldNot contain(to)
        }
      })
    }

    def checkBlocked(userA: UserId, userB: UserId, blocked: Boolean): Unit = {
      authorityService.isLinkBlocked(userA, userB).futureValue should be(
        blocked
      )
    }

    def checkMutuallyBlocked(userA: UserId, userB: UserId, blocked: Boolean): Unit = {
      checkBlocked(userA, userB, blocked)
      checkBlocked(userB, userA, blocked)
    }

    "properly handle mutual block" in {
      val from = userId.next
      val parameters = blockUserParameters.next
      val to = parameters.user
      val ctx = parameters.context
      val window = Window[UserId](None, 10, asc = true)
      withUserContext(from) { rc =>
        authorityService.block(parameters)(rc).futureValue
        authorityService
          .listBlocked(window)(rc)
          .futureValue
          .map(_.user) should contain(to)
      }
      withUserContext(to) { rc =>
        authorityService.block(BlockUserParameters(from, ctx))(rc).futureValue
        authorityService
          .listBlocked(window)(rc)
          .futureValue
          .map(_.user) should contain(from)
      }

      checkMutuallyBlocked(from, to, blocked = true)

      withUserContext(from) { rc =>
        authorityService.unblock(to)(rc).futureValue
        authorityService
          .listBlocked(window)(rc)
          .futureValue
          .map(_.user) shouldNot contain(to)
      }

      checkMutuallyBlocked(from, to, blocked = true)

      withUserContext(to) { rc =>
        authorityService
          .listBlocked(window)(rc)
          .futureValue
          .map(_.user) should contain(from)
        authorityService.unblock(from)(rc).futureValue
        authorityService
          .listBlocked(window)(rc)
          .futureValue
          .map(_.user) shouldNot contain(from)
      }

      checkMutuallyBlocked(from, to, blocked = false)
    }
  }
}

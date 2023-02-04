package ru.yandex.vertis.passport.service.user

import org.joda.time.DateTime
import org.scalatest.WordSpec
import org.mockito.Mockito.verify
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.FullUserDao
import ru.yandex.vertis.passport.model.{FullUser, UserPhone}
import ru.yandex.vertis.passport.service.ban.DummyUserModerationStatusProvider
import ru.yandex.vertis.passport.service.session.UserSessionService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

/**
  * @author neron
  */
class UserBackendServiceSpec extends WordSpec with SpecBase with MockitoSupport with PropertyChecks {

  import scala.concurrent.ExecutionContext.Implicits.global

  class Context {
    val userDao = mock[FullUserDao]
    val userSessionService = mock[UserSessionService]

    val service = new UserBackendService(userDao, DummyUserModerationStatusProvider, userSessionService)
  }

  def context(f: Context => Unit): Unit = {
    f(new Context)
  }

  "UserBackendService" should {
    "drop user sessions" in context { ctx =>
      val user = ModelGenerators.fullUser.next

      when(ctx.userSessionService.deleteAllUserSessions(eq(user.id))(?)).thenReturn(Future.unit)
      when(ctx.userSessionService.invalidateCached(eq(user.id))(?)).thenReturn(Future.unit)

      ctx.service.dropUserSessions(user.id).futureValue

      verify(ctx.userSessionService).deleteAllUserSessions(eq(user.id))(?)
      verify(ctx.userSessionService).invalidateCached(eq(user.id))(?)
    }

    val phones = Seq(
      ModelGenerators.userPhone.next.copy(added = None),
      ModelGenerators.userPhone.next.copy(added = Some(DateTime.now().minusYears(3))),
      ModelGenerators.userPhone.next.copy(added = Some(DateTime.now().minusMonths(2))),
      ModelGenerators.userPhone.next.copy(added = Some(DateTime.now().minusDays(15))),
      ModelGenerators.userPhone.next.copy(added = Some(DateTime.now().minusDays(1)))
    )

    def makeUser(regDate: DateTime = DateTime.now().minusMonths(10), phones: Seq[UserPhone] = phones): FullUser =
      ModelGenerators.fullUser.next.copy(registrationDate = regDate, phones = phones)

    "clean all except first phones for last month created hacked user" in {
      val freshUser = makeUser(DateTime.now().minusMonths(1).plusDays(1), phones.drop(3))
      val survivedPhones = UserBackendService.cleanHackedUserPhones(freshUser)
      survivedPhones shouldBe freshUser.phones.take(1)
    }

    "remove last month phones for hacked user" in {
      val survivedPhones = UserBackendService.cleanHackedUserPhones(makeUser())
      survivedPhones should not contain allElementsOf(phones.takeRight(2))
    }

    "remove phones older than 2 years for hacked user, when email exist" in {
      val user = makeUser().copy(emails = Seq(ModelGenerators.userEmail.next))
      val survivedPhones = UserBackendService.cleanHackedUserPhones(user)
      survivedPhones should not contain allElementsOf(phones.take(2))
    }

    "not remove phones older than 2 years for hacked user, when no emails" in {
      val user = makeUser().copy(emails = Seq())
      val survivedPhones = UserBackendService.cleanHackedUserPhones(user)
      survivedPhones should contain allElementsOf phones.take(2)
    }

    "not remove all phones when no emails" in {
      val user = makeUser(phones = phones.take(2)).copy(emails = Seq())
      val survivedPhones = UserBackendService.cleanHackedUserPhones(user)
      survivedPhones should contain allElementsOf user.phones.take(1)
    }
  }

}

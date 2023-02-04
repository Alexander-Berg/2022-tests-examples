package ru.yandex.vertis.passport.service.user

import org.joda.time.DateTime
import org.scalatest.fixture
import ru.yandex.passport.model.api.ApiModel.PasswordValidationError
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.{FullUserDao, IdentityTokenCache}
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.model.api.ChangePasswordParameters
import ru.yandex.vertis.passport.service.acl.{AclService, DummyGrantsService}
import ru.yandex.vertis.passport.service.antifraud.AntifraudService
import ru.yandex.vertis.passport.service.ban.{BanService, DummyUserModerationStatusProvider}
import ru.yandex.vertis.passport.service.confirmation.ConfirmationService2
import ru.yandex.vertis.passport.service.session.UserSessionService
import ru.yandex.vertis.passport.service.tvm.UserTvmService
import ru.yandex.vertis.passport.service.user.pwd.PasswordService
import ru.yandex.vertis.passport.service.user.social.SocialUserService
import ru.yandex.vertis.passport.service.vox.VoxEncryptors
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.crypt.BlowfishEncryptor
import ru.yandex.vertis.passport.util.{LastIdentityRemoveException, PasswordIsTooWeakException, PhoneIsBannedException}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class UserServiceMockedSpec extends fixture.WordSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  class FixtureParam {
    val userDao = mock[FullUserDao]
    val sessionService = mock[UserSessionService]
    val confirmationService = mock[ConfirmationService2]
    val socialService = mock[SocialUserService]
    val aclService = mock[AclService]
    val banService = mock[BanService]
    val antifraudService = mock[AntifraudService]
    val passwordService = mock[PasswordService]
    val userTvmService = mock[UserTvmService]
    val identityTokenCache = mock[IdentityTokenCache]

    val voxEncryptors =
      VoxEncryptors(new BlowfishEncryptor("vox-username-test"), new BlowfishEncryptor("vox-password-test"))

    val userService = new UserServiceImpl(
      userDao,
      DummyUserModerationStatusProvider,
      sessionService,
      confirmationService,
      banService,
      passwordService,
      DummyGrantsService,
      userTvmService = userTvmService,
      voxEncryptors = voxEncryptors,
      identityTokenCache = identityTokenCache
    )
  }

  override def withFixture(test: OneArgTest) = {
    withFixture(test.toNoArgTest(new FixtureParam))
  }

  "UserService.addPhone" should {
    "forbid banned phones" in { ctx =>
      val userId = ModelGenerators.userId.next
      val phone = ModelGenerators.phoneNumber.next
      val params = AddPhoneParameters(phone, steal = true)

      when(ctx.banService.checkPhoneBanned(eq(phone))(?)).thenReturn(Future.successful(true))

      ctx.userService.addPhone(userId, params).failed.futureValue shouldBe an[PhoneIsBannedException]
    }
  }

  "UserService.addPhone" should {
    "forbid to add too many phones" in { ctx =>
      val curDate = DateTime.now()
      val userId = ModelGenerators.userId.next
      val phone = ModelGenerators.phoneNumber.next
      val params = AddPhoneParameters(phone, steal = true, confirmed = true)
      val user = ModelGenerators.fullUser.next.copy(phones = (0 until 1200).map { i =>
        val phoneNum = ModelGenerators.phoneNumber.next
        UserPhone(phoneNum, Some(curDate.minusSeconds(i)))
      })
      when(ctx.userDao.get(eq(userId))(?)).thenReturn(Future.successful(user))
      when(ctx.userDao.findId(?)(?)).thenReturn(Future.successful(None))
      when(ctx.banService.checkPhoneBanned(eq(phone))(?)).thenReturn(Future.successful(false))
      when(ctx.sessionService.invalidateCached(eq(userId))(?)).thenReturn(Future.unit)
      @volatile var insertedUser: Option[FullUser] = None
      stub(
        ctx.userDao.updateWithPayload[Any](
          _: UserId
        )(_: Function[FullUser, (FullUser, Any)])(_: Traced)
      ) {
        case (_, f, _) =>
          insertedUser = Some(f(user)._1)
          Future.successful(f(user))
      }
      ctx.userService.addPhone(userId, params).futureValue
      insertedUser.get.phones.size shouldBe 1000
      insertedUser.get.phones.map(_.phone) should contain(phone)
    }
  }

  "UserService.changePassword" should {
    "fail on policy violation" in { ctx =>
      val currentPassword = ModelGenerators.readableString.next
      val md5hash = PasswordUtils.hashLegacy(currentPassword)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser.next
        .copy(
          pwdHash = Some(ArgonHasher.hash(md5hash, now))
        )
      val newPassword = ModelGenerators.readableString.next

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.passwordService.checkPasswordStrongEnough(eq(user), eq(newPassword))(?))
        .thenReturn(Future.successful(Seq(PasswordValidationError.TOO_SHORT)))

      ctx.userService
        .changePassword(user.id, ChangePasswordParameters(currentPassword, newPassword))
        .failed
        .futureValue shouldBe a[PasswordIsTooWeakException]
    }

  }

  "UserService.removePhone" should {
    "not allow to remove last user identity (except social)" in { ctx =>
      val user = ModelGenerators.legacyUser
        .suchThat(u => u.phones.size == 1 && u.emails.isEmpty)
        .next
      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))

      stub(
        ctx.userDao.updateWithPayload[Any](
          _: UserId
        )(_: Function[FullUser, (FullUser, Any)])(_: Traced)
      ) {
        case (_, f, _) => Future.successful(f(user))
      }

      intercept[LastIdentityRemoveException] {
        val p = user.phones.head.phone
        ctx.userService
          .removeIdentity(user.id, Identity.Phone(p), removeLast = false)
          .futureValue
      }

    }
  }

}

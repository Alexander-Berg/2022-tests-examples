package ru.yandex.vertis.passport.service.user.pwd

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel.PasswordValidationError._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.UserPreviousPasswordsDao
import ru.yandex.vertis.passport.model.{EnrichedUserModerationStatus, Identity}
import ru.yandex.vertis.passport.service.acl.AclService
import ru.yandex.vertis.passport.service.ban.UserModerationStatusProvider
import ru.yandex.vertis.passport.service.user.pwd.PasswordService.CharClasses._
import ru.yandex.vertis.passport.service.user.{ArgonHasher, PasswordUtils, UserService}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class PasswordServiceSpec extends WordSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  class Context(policies: PasswordPolicies) {
    val acl = mock[AclService]
    val prevPasswordsDao = mock[UserPreviousPasswordsDao]
    val moderationService = mock[UserModerationStatusProvider]
    val passwordService = new PasswordService(policies, acl, prevPasswordsDao, moderationService)
  }

  def context(policies: PasswordPolicies)(f: Context => Unit): Unit = {
    f(new Context(policies))
  }

  def context(policy: PasswordPolicy)(f: Context => Unit): Unit = {
    context(PasswordPolicies(policy, policy, policy)) { ctx =>
      when(ctx.acl.isOurUser(?)(?)).thenReturn(Future.successful(false))
      f(ctx)
    }
  }

  "PasswordService.charClasses" should {
    "work for empty strings" in {
      PasswordService.charClasses("") shouldBe Set()
    }

    "identify letters" in {
      PasswordService.charClasses("йцук") shouldBe Set(LowerCase)
      PasswordService.charClasses("йцукЩГЩ") shouldBe Set(LowerCase, UpperCase)
      PasswordService.charClasses("DHFD") shouldBe Set(UpperCase)
    }

    "identify digits" in {
      PasswordService.charClasses("123") shouldBe Set(Digit)
    }

    "identify everything else as special" in {
      PasswordService.charClasses("`") shouldBe Set(Special)
      PasswordService.charClasses(":,") shouldBe Set(Special)
    }
  }

  "PasswordService.applySimpleChecks" should {
    "check length" in {
      val policy = PasswordPolicy(minLength = Some(8))
      PasswordService.applySimpleChecks(policy, "") shouldBe Seq(TOO_SHORT)
      PasswordService.applySimpleChecks(policy, "123") shouldBe Seq(TOO_SHORT)
      PasswordService.applySimpleChecks(policy, "12345678") shouldBe Nil
      PasswordService.applySimpleChecks(policy, "123456789") shouldBe Nil
    }

    "check character classes" in {
      val policy = PasswordPolicy(minCharClasses = Some(3))
      PasswordService.applySimpleChecks(policy, "") shouldBe Seq(TOO_FEW_CHAR_CLASSES)
      PasswordService.applySimpleChecks(policy, "432") shouldBe Seq(TOO_FEW_CHAR_CLASSES)
      PasswordService.applySimpleChecks(policy, "123ab") shouldBe Seq(TOO_FEW_CHAR_CLASSES)
      PasswordService.applySimpleChecks(policy, "1234abC") shouldBe Nil
      PasswordService.applySimpleChecks(policy, "1234ab`") shouldBe Nil
    }
  }

  val userGen = ModelGenerators.legacyUser.suchThat(_.profile.passwordLogin.isEmpty)

  val emptyUserModerationStatus = EnrichedUserModerationStatus.Empty

  "PasswordService.applyPolicy" should {

    "check previous passwords" in context(PasswordPolicy(uniqueWithinLast = Some(5))) { ctx =>
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser.next.copy(
        registrationDate = now
      )
      val prevPasswords = ModelGenerators.readableString.next(5)
      val prevHashes = prevPasswords.map(PasswordUtils.hashLegacy)
      val prevArgonHashes = prevHashes.map(h => { ArgonHasher.hash(h, user.registrationDate) })
      val otherPassword = ModelGenerators.readableString.next
      when(ctx.moderationService.getUserModerationStatus(?)(?))
        .thenReturn(Future.successful(emptyUserModerationStatus))
      when(ctx.prevPasswordsDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(prevArgonHashes))

      ctx.passwordService.checkPasswordStrongEnough(user, otherPassword).futureValue shouldBe empty
      prevPasswords.foreach { pwd =>
        ctx.passwordService.checkPasswordStrongEnough(user, pwd).futureValue shouldBe Seq(NOT_UNIQUE)
      }
    }

    "don't allow reset current password" in context(PasswordPolicy(uniqueWithinLast = Some(5))) { ctx =>
      val password = ModelGenerators.readableString.next
      val md5Hash = PasswordUtils.hashLegacy(password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser.next
        .copy(
          pwdHash = Some(ArgonHasher.hash(md5Hash, now)),
          registrationDate = now
        )

      when(ctx.moderationService.getUserModerationStatus(?)(?))
        .thenReturn(Future.successful(emptyUserModerationStatus))
      when(ctx.prevPasswordsDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(Nil))

      ctx.passwordService.checkPasswordStrongEnough(user, password).futureValue shouldBe Seq(NOT_UNIQUE)
    }

    "choose policy depending on user type" in
      context(
        PasswordPolicies.Empty.copy(
          moderators = PasswordPolicy(minLength = Some(6)),
          special = PasswordPolicy(minLength = Some(6))
        )
      ) { ctx =>
        val user1 = userGen.next
        val user2 = userGen.next
        when(ctx.moderationService.getUserModerationStatus(?)(?))
          .thenReturn(Future.successful(emptyUserModerationStatus))
        when(ctx.acl.isOurUser(eq(user1.id))(?)).thenReturn(Future.successful(false))
        when(ctx.acl.isOurUser(eq(user2.id))(?)).thenReturn(Future.successful(true))

        ctx.passwordService.checkPasswordStrongEnough(user1, "1").futureValue shouldBe empty
        ctx.passwordService.checkPasswordStrongEnough(user2, "1").futureValue shouldBe Seq(TOO_SHORT)
      }

    "choose special policy on user's setting" in {
      context(PasswordPolicies.Empty.copy(special = PasswordPolicy(minLength = Some(6)))) { ctx =>
        val user = {
          val u = ModelGenerators.legacyUser.next
          u.copy(profile = u.profile.autoru.copy(passwordLogin = Some(true)))
        }
        ctx.passwordService.checkPasswordStrongEnough(user, "4").futureValue shouldBe Seq(TOO_SHORT)
      }

    }
  }

  "PasswordService.allowCodeLogin" should {
    "force password login depends on experiment" in context(
      PasswordPolicies.Empty.copy(passwordLoginPercent = 80)
    ) { ctx =>
      val user1 = userGen.next.copy(id = "11234")
      val user2 = userGen.next.copy(id = "54385")
      val identity = Identity.Email("test@test.com")

      when(ctx.moderationService.getUserModerationStatus(?)(?))
        .thenReturn(Future.successful(emptyUserModerationStatus))
      when(ctx.acl.isOurUser(?)(?)).thenReturn(Future.successful(false))

      ctx.passwordService.allowCodeLogin(user1, identity.identityType).futureValue shouldBe false
      ctx.passwordService.allowCodeLogin(user2, identity.identityType).futureValue shouldBe true
    }
  }

  "use passwordLogin instead of experiment if defined" in context(
    PasswordPolicies.Empty.copy(
      passwordLoginPercent = 80,
      special = PasswordPolicy(allowEmailCodeLogin = false)
    )
  ) { ctx =>
    val user1 = ModelGenerators.legacyUser.suchThat(_.profile.passwordLogin.contains(true)).next.copy(id = "23423599")
    val user2 = ModelGenerators.legacyUser.suchThat(_.profile.passwordLogin.contains(false)).next.copy(id = "123")
    val identity = Identity.Email("test@test.com")

    when(ctx.moderationService.getUserModerationStatus(?)(?))
      .thenReturn(Future.successful(emptyUserModerationStatus))
    when(ctx.acl.isOurUser(?)(?)).thenReturn(Future.successful(false))

    ctx.passwordService.allowCodeLogin(user1, identity.identityType).futureValue shouldBe false
    ctx.passwordService.allowCodeLogin(user2, identity.identityType).futureValue shouldBe true
  }

  "use code login for user without password" in context(
    PasswordPolicies.Empty.copy(
      passwordLoginPercent = 100,
      special = PasswordPolicy(allowEmailCodeLogin = false)
    )
  ) { ctx =>
    val user1 = userGen.next.copy(pwdHash = Some(UserService.PASSWORD_NOT_DEFINED_MARK))
    val user2 = userGen.next
    val identity = Identity.Email("test@test.com")

    when(ctx.moderationService.getUserModerationStatus(?)(?))
      .thenReturn(Future.successful(emptyUserModerationStatus))
    when(ctx.acl.isOurUser(?)(?)).thenReturn(Future.successful(false))

    ctx.passwordService.allowCodeLogin(user1, identity.identityType).futureValue shouldBe true
    ctx.passwordService.allowCodeLogin(user2, identity.identityType).futureValue shouldBe false
  }

  "disable password login for regular users if it's not enabled explicitly" in context(
    PasswordPolicies.Empty.copy(
      passwordLoginPercent = 100,
      normal = PasswordPolicy(allowPasswordLogin = false),
      special = PasswordPolicy()
    )
  ) { ctx =>
    val user1 = userGen.next
    val user2 = {
      val u = userGen.next
      u.copy(profile = u.profile.autoru.copy(passwordLogin = Some(true)))
    }

    when(ctx.moderationService.getUserModerationStatus(?)(?))
      .thenReturn(Future.successful(emptyUserModerationStatus))
    when(ctx.acl.isOurUser(?)(?)).thenReturn(Future.successful(false))

    ctx.passwordService.allowPasswordLogin(user1).futureValue shouldBe false
    ctx.passwordService.allowPasswordLogin(user2).futureValue shouldBe true
  }
}

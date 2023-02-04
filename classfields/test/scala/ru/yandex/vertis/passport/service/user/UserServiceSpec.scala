package ru.yandex.vertis.passport.service.user

import org.scalatest.{CancelAfterFailure, FreeSpec}
import ru.yandex.vertis.passport.dao.FullUserDao
import ru.yandex.vertis.passport.integration.CachedUserProvider
import ru.yandex.vertis.passport.model.SocializedUser._
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.model.api.{ChangeEmailConfirmation, ChangeEmailParameters, ChangePasswordParameters, RequestEmailChangeParameters, RequestPasswordResetParameters}
import ru.yandex.vertis.passport.service.session.UserSessionService
import ru.yandex.vertis.passport.service.user.auth.AuthenticationService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.{AliasIsNotUniqueException, AuthenticationException, IdentityAlreadyTakenException, InvalidCredentialsException, SessionNotFoundException}
import ru.yandex.passport.model.api.ApiModel.LoadUserHint
import ru.yandex.vertis.passport.model.IdentityTypes.Email

/**
  * Basic tests for [[UserService]]
  *
  * @author zvez
  */
//scalastyle:off multiple.string.literals
trait UserServiceSpec extends FreeSpec with SpecBase with CancelAfterFailure {
  val userProvider: CachedUserProvider
  val sessionService: UserSessionService
  val userService: UserService
  val authService: AuthenticationService
  val userDao: FullUserDao

  implicit val ctx: RequestContext = RequestContext("test")

  var email: String = ModelGenerators.emailAddress.next
  var password: String = ModelGenerators.readableString.next
  var phone: Phone = ModelGenerators.phoneNumber.next
  var clientId: IdentityToken = ModelGenerators.clientId.next

  var emailWithSubAddress: String =
    email.takeWhile(c => c != '@') + "+" + ModelGenerators.readableString.next + email.dropWhile(c => c != '@')

  var userId: UserId = _

  "UserService" - {

    "identity confirmation ignore email subaddress" in {
      var confirmCode: String = null
      val source = UserSource(
        profile = ModelGenerators.userProfile.filter(_.fullName.isDefined).next,
        email = Some(emailWithSubAddress),
        phone = None,
        password
      )

      val result = userService.create(source).futureValue
      result.confirmCode shouldBe defined
      confirmCode = result.confirmCode.get
      result.user.active shouldBe false

      sessionService.get(result.session.id).futureValue

      userId = result.user.id

      val confirmationResult =
        userService.confirmIdentity(ConfirmationCode(Identity.Email(emailWithSubAddress), confirmCode)).futureValue
      confirmationResult.identity shouldEqual Some(Identity.Email(email = email))
    }

    "registration" - {

      var confirmCode: String = null
      val source = UserSource(
        profile = ModelGenerators.userProfile.filter(_.fullName.isDefined).next,
        email = Some(email),
        phone = None,
        password
      )

      "create user" in {
        val result = userService.create(source).futureValue
        result.confirmCode shouldBe defined
        confirmCode = result.confirmCode.get
        result.user.active shouldBe false

        sessionService.get(result.session.id).futureValue

        userId = result.user.id
      }

      "can't login before confirmation" in {
        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), password)))
          .failed
          .futureValue shouldBe an[AuthenticationException]
      }

      "confirm registration" in {
        userService.confirmIdentity(ConfirmationCode(Identity.Email(email), confirmCode)).futureValue
        val user = userService.get(userId).futureValue.user
        user.active shouldBe true
        user.emails.exists(_.email == email) shouldBe true
      }

      "don't allow to register user with same email" in {
        val anotherSource = UserSource(
          profile = ModelGenerators.userProfile.filter(_.fullName.isDefined).next,
          email = Some(email),
          phone = None,
          password
        )
        userService.create(anotherSource).failed.futureValue shouldBe an[IdentityAlreadyTakenException]
      }
    }

    "login" in {
      val loginResult =
        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), password)))
          .futureValue
      loginResult.user.id shouldBe userId
      val sessionByService = sessionService.get(loginResult.session.id).futureValue
      val sessionByServiceWithoutJwt = sessionByService.copy(userTicket = None, grants = None)

      sessionByServiceWithoutJwt shouldBe loginResult.asSessionResult.copy(userTicket = None)
      sessionByService.grants shouldBe Some(UserGrantsSet(Nil))
      loginResult.asSessionResult.userTicket shouldNot be(sessionByService.userTicket)

    }

    "change profile" in {
      val expected = userService
        .get(userId)
        .futureValue
        .user
        .profile
        .autoru
        .copy(
          alias = Some("other-name"),
          fullName = None,
          userpic = Some("foo-bar"),
          geoId = Some(1L)
        )
      val patch = AutoruUserProfilePatch(
        alias = FieldPatch.set(Some("other-name")),
        userpic = FieldPatch.set(Some("foo-bar")),
        fullName = FieldPatch.set(None),
        geoId = FieldPatch.set(Some(1))
      )

      userService.updateProfile(userId, patch).futureValue
      val user = userService.get(userId).futureValue.user
      user.profile shouldBe expected
    }

    "fail to set non-unique alias" in {
      userService
        .create( // create user with alias "foo"
          UserSource(
            AutoruUserProfile(alias = Some("foo")),
            None,
            None,
            UserService.PASSWORD_NOT_DEFINED_MARK
          )
        )
        .futureValue
      userService
        .updateProfile(
          userId,
          AutoruUserProfilePatch(
            alias = FieldPatch.set(Some("foo"))
          )
        )
        .failed
        .futureValue shouldBe an[AliasIsNotUniqueException]
    }

    "add phone" - {
      var confirmCode: String = null

      /*"should fail on invalid credentials" in {
        val credentials = UserCredentials(Identity.Email("some@test.com"), "1234")
        val res = userService.addPhone(userId, phone, credentials)
        res.failed.futureValue shouldBe an[InvalidCredentialsException]
      }*/

      "actually add" in {
//        val credentials = UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), password)
        confirmCode = userService.addPhone(userId, AddPhoneParameters(phone, steal = false)).futureValue.code.get

        val user = userService.get(userId).futureValue.user
        user.phones shouldBe empty
      }

      "confirm phone" in {
        userService.confirmIdentity(ConfirmationCode(Identity.Phone(phone), confirmCode)).futureValue
        val user = userService.get(userId).futureValue.user
        user.phones.length shouldBe 1
        user.phones.head.phone shouldBe phone
      }

      "login with new phone" in {
        val loginResult =
          authService
            .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Phone(phone)), password)))
            .futureValue
        loginResult.user.id shouldBe userId
      }
    }

    "change email" - {

      "when user has linked phone or email" - {
        val anotherEmail = ModelGenerators.emailAddress.next
        var confirmCode: String = ""
        val emailIdty = Identity.Email(email)

        "request change confirmation" in {
          val params = RequestEmailChangeParameters(emailIdty)
          confirmCode = userService.askForEmailChange(userId, params).futureValue
        }

        "fail to change if invalid confirmation code is sent" in {
          val badConfirmCode = s"bad-$confirmCode"
          val confirmation = ChangeEmailConfirmation.ByCode(emailIdty, badConfirmCode)
          val params = ChangeEmailParameters(anotherEmail, confirmation)
          userService.changeEmail(userId, params).failed.futureValue
        }

        "accept change" in {
          val confirmation = ChangeEmailConfirmation.ByCode(emailIdty, confirmCode)
          val params = ChangeEmailParameters(anotherEmail, confirmation)
          confirmCode = userService.changeEmail(userId, params).futureValue.code.getOrElse("")

          val user = userService.get(userId).futureValue.user
          user.emails should matchPattern { case Seq(UserEmail(eml, true, _)) if eml == email => }
        }

        "confirm email" in {
          val cc = ConfirmationCode(Identity.Email(anotherEmail), confirmCode)
          userService.confirmIdentity(cc).futureValue
          val user = userService.get(userId).futureValue.user
          user.emails should matchPattern { case Seq(UserEmail(`anotherEmail`, true, _)) => }

          email = anotherEmail
        }
      }
      "when user doesn't have linked phone or email" - {
        val anotherEmail = ModelGenerators.emailAddress.next
        var confirmCode: String = ""
        val params = ChangeEmailParameters(anotherEmail, ChangeEmailConfirmation.SkipConfirmation)

        "remove identities" in {
          userDao
            .update(userId) { user =>
              user.copy(emails = List.empty, phones = List.empty)
            }
            .futureValue
          userService.get(userId).futureValue.user.confirmedIdentities shouldBe empty
        }

        "accept change without confirmation" in {
          confirmCode = userService.changeEmail(userId, params).futureValue.code.value
        }

        "confirm email" in {
          val confirmation = ConfirmationCode(Identity.Email(anotherEmail), confirmCode)
          userService.confirmIdentity(confirmation).futureValue
          email = anotherEmail
        }

        "set phone back" in {
          userService.addPhone(userId, AddPhoneParameters(phone, steal = false, confirmed = true))
        }
      }
    }

    "change password" - {
      val newPassword = ModelGenerators.readableString.next
      var currentSessionId: SessionId = null
      var newSessionId: SessionId = null

      "should fail on invalid password" in {
        val res = userService.changePassword(userId, ChangePasswordParameters("1234", newPassword))
        res.failed.futureValue shouldBe an[InvalidCredentialsException]
      }

      "really change" in {
        currentSessionId = sessionService.getLastUserSessions(userId).futureValue.head.id

        //legacy db has up-to-second precision of passwordDate, so have to wait here
        // to make sure new session get created at leas 1 second after previous one
        //todo remove it when move to new storage
        Thread.sleep(1000L)
        val result = userService
          .changePassword(userId, ChangePasswordParameters(password, newPassword))
          .futureValue
        result.user.id shouldBe userId
        newSessionId = result.session.id
        //check new session is valid
        sessionService.get(result.session.id).futureValue.session shouldBe result.session
        sessionService.getLastUserSessions(userId).futureValue.map(_.id) shouldBe Seq(newSessionId)

        val user = userDao.get(userId).futureValue
        user.passwordDate shouldBe defined
      }

      "password was actually changed" in {
        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), password)))
          .failed
          .futureValue shouldBe an[AuthenticationException]
        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), newPassword)))
          .futureValue
      }

      "check old session get eventually deleted and new one is still alive" in {
        sessionService.get(currentSessionId).failed.futureValue shouldBe a[NoSuchElementException]
        sessionService.get(newSessionId).futureValue
      }
    }

    "reset password" - {
      val newPassword = ModelGenerators.readableString.next

      var confirmationCode: String = null

      "ask for password reset" in {
        val params = RequestPasswordResetParameters(Identity.Email(email))
        confirmationCode = userService.askForPasswordReset(params).futureValue
      }

      "actually reset" in {
        val res = userService
          .resetPassword(ConfirmationCode(Identity.Email(email), confirmationCode), newPassword)
          .futureValue
        sessionService.get(res.session.id).futureValue
        sessionService.getLastUserSessions(userId).futureValue.map(_.id) shouldBe Seq(res.session.id)
      }

      "can login with new password" in {
        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(Identity.Email(email)), newPassword)))
          .futureValue
        password = newPassword
      }
    }

    "social login" - {
      "trusted social provider" - {
        val provider = SocialProviders.Hsd

        val loginParams =
          SocialLoginParameters(provider = provider, authOrUser = Right(ModelGenerators.socialUserSource.next))
        var createdUserId: UserId = null

        "create new user" in {
          authService.loginSocial(loginParams).futureValue.socializedUser match {
            case Created(user) =>
              createdUserId = user.id
              user.socialProfiles.length shouldBe 1
              val profile = user.socialProfiles.head
              profile.socialUser.id shouldBe loginParams.authOrUser.right.get.id
              profile.provider shouldBe provider
            case other => fail("Unexpected result: " + other)
          }
        }

        "login with existing user" in {
          authService.loginSocial(loginParams).futureValue match {
            case SocialLoginResult(Found(_), session, None, _) =>
              session.userId shouldBe Some(createdUserId)
            case other => fail("Unexpected result: " + other)
          }
        }

        "link to existing user" in {
          val someEmail = "social.user@test.com"
          val user = userDao.create {
            ModelGenerators.legacyUser.next.copy(
              phones = Nil,
              socialProfiles = Nil,
              emails = Seq(UserEmail(someEmail, confirmed = true)),
              active = true
            )
          }.futureValue

          val loginParams = SocialLoginParameters(
            provider = provider,
            authOrUser = Right(ModelGenerators.socialUserSource.next.copy(emails = Seq(someEmail)))
          )

          authService.loginSocial(loginParams).futureValue.socializedUser match {
            case Linked(linkedUser, linkedBy) =>
              linkedUser.id shouldBe user.id
              linkedUser.emails shouldBe user.emails
              linkedUser.socialProfiles.length shouldBe 1
              val profile = linkedUser.socialProfiles.head
              profile.socialUser.id shouldBe loginParams.authOrUser.right.get.id
              profile.provider shouldBe provider
              linkedBy shouldBe Identity.Email(someEmail)

            case other => fail("Unexpected result: " + other)
          }

        }
      }
    }

    "loginOrCreate" - {
      val phoneNumber = ModelGenerators.phoneNumber.next

      var userId: UserId = null

      "when phone doesn't belong to anyone" - {
        var code: String = null

        "should send confirmation" in {
          code = authService
            .loginOrRegister(LoginOrRegisterParameters(IdentityOrToken.RealIdentity(Identity.Phone(phoneNumber))))
            .futureValue
            .confirmationCode
        }

        "should create user after phone confirmation" in {
          val cc = ConfirmationCode(Identity.Phone(phoneNumber), code)
          val result = userService.confirmIdentity(cc, createSession = true).futureValue
          result.session shouldBe defined
          result.user.phones.map(_.phone) shouldBe Seq(phoneNumber)
          result.user.active shouldBe true
          userId = result.user.id
        }
      }

      "when phone is attached to user" - {
        var code: String = null

        "should send confirmation" in {
          code = authService
            .loginOrRegister(LoginOrRegisterParameters(IdentityOrToken.RealIdentity(Identity.Phone(phoneNumber))))
            .futureValue
            .confirmationCode
        }

        "should get existing user after phone confirmation" in {
          val cc = ConfirmationCode(Identity.Phone(phoneNumber), code)
          val result = userService.confirmIdentity(cc, createSession = true).futureValue
          result.session shouldBe defined
          result.user.phones.map(_.phone) shouldBe Seq(phoneNumber)
          result.user.active shouldBe true
          result.user.id shouldBe userId
        }
      }
    }

    "add phone should fail if phone belongs to someone else" in {
      val phone = ModelGenerators.phoneNumber.next
      userDao
        .create(
          ModelGenerators.legacyUser.next.copy(phones = Seq(UserPhone(phone)))
        )
        .futureValue
      val user2 = userDao.create(ModelGenerators.legacyUser.next).futureValue

      userService
        .addPhone(user2.id, AddPhoneParameters(phone, steal = false))
        .failed
        .futureValue shouldBe an[IdentityAlreadyTakenException]
    }

    "confirmIdentity should unlink phone" in {
      val phone = ModelGenerators.phoneNumber.next
      val user1 = userDao
        .create(
          ModelGenerators.legacyUser.next.copy(phones = Seq(UserPhone(phone)))
        )
        .futureValue
      val user2 = userDao.create(ModelGenerators.legacyUser.next).futureValue

      val code =
        userService.addPhone(user2.id, AddPhoneParameters(phone, steal = true)).futureValue.code.get

      val res = userService.confirmIdentity(ConfirmationCode(Identity.Phone(phone), code)).futureValue
      res.user.id shouldBe user2.id
      res.user.phones.exists(_.phone == phone) shouldBe true

      val user1Updated = userDao.get(user1.id).futureValue
      user1Updated.phones.exists(_.phone == phone) shouldBe false
    }

    "forgetUser" - {
      "deactivate and delete sessions" in {

        val user = ModelGenerators.fullUser.next
        val userId = userDao.create(user).futureValue.id
        sessionService.create(UserSessionSource(userId, None)).futureValue
        sessionService.getLastUserSessions(userId).futureValue should not be empty

        userService.forgetUser(userId).futureValue
        val forgottenUser = userService.get(userId, hints = LoadUserHint.values.toSet).futureValue.user
        val sessions = sessionService.getLastUserSessions(userId).futureValue

        forgottenUser.active shouldBe false
        sessions shouldBe empty
      }

      "erase personal data" in {
        val user = ModelGenerators.fullUser.next
        val createdUser = userDao.create(user).futureValue
        userService.forgetUser(createdUser.id).futureValue

        def fields(u: FullUser) = Seq[Traversable[_]](
          u.emails,
          u.phones,
          u.socialProfiles,
          u.yandexStaffLogin,
          u.profile.about,
          u.profile.alias,
          u.profile.fullName,
          u.profile.userpic,
          u.profile.autoru.about,
          u.profile.autoru.alias,
          u.profile.autoru.birthday,
          u.profile.autoru.cityId,
          u.profile.autoru.clientId,
          u.profile.autoru.clientGroup,
          u.profile.autoru.countryId,
          u.profile.autoru.drivingYear,
          u.profile.autoru.fullName,
          u.profile.autoru.geoId,
          u.profile.autoru.regionId,
          u.profile.autoru.userpic
        )

        // Make sure this test actually checks something. This technically makes it flaky, but this assertion should
        // almost never fail.
        atLeast(4, fields(createdUser)) should not be empty

        val result = userService.get(createdUser.id, hints = LoadUserHint.values.toSet).futureValue
        val forgottenUser = result.user

        all(fields(forgottenUser)) shouldBe empty
      }

      "prevent forgotten user from being accessible via session" in {
        val user = ModelGenerators.fullUser.next
        val userId = userDao.create(user).futureValue.id
        val sessionId = sessionService.create(UserSessionSource(userId, None)).futureValue.session.id

        userService.forgetUser(userId).futureValue
        sessionService.get(sessionId).failed.futureValue shouldBe a[SessionNotFoundException]
      }

      "should force loginOrRegister to create a new user next time" in {
        val userPhone = ModelGenerators.userPhone.next
        val userEmail = ModelGenerators.userEmail.next
        val userBase = ModelGenerators.fullUser.next
        val user = userBase.copy(
          phones = userPhone +: userBase.phones,
          emails = userEmail +: userBase.emails
        )
        val userId = userDao.create(user).futureValue.id

        userService.forgetUser(userId).futureValue

        val resultByPhone = authService
          .loginOrRegister(
            LoginOrRegisterParameters(
              IdentityOrToken.RealIdentity(
                Identity.Phone(userPhone.phone)
              )
            )
          )
          .futureValue

        resultByPhone.userId shouldBe empty

        val resultByEmail = authService
          .loginOrRegister(
            LoginOrRegisterParameters(
              IdentityOrToken.RealIdentity(
                Identity.Email(userEmail.email)
              )
            )
          )
          .futureValue

        resultByEmail.userId shouldBe empty
      }
    }
  }
}

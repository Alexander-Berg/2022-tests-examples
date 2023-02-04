package ru.yandex.vertis.passport.service.user.social

import org.scalatest.{FreeSpec, Inspectors, OptionValues}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.integration.features.FeatureManager
import ru.yandex.vertis.passport.model.SocializedUser._
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.service.ban.InMemoryBanService
import ru.yandex.vertis.passport.service.user.social.clients.SimpleOAuth2Client.OAuthAuthorizationException
import ru.yandex.vertis.passport.service.user.{DaoProvider, SpiedSocialProviders}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.test.MockFeatures.{featureOff, featureOn}
import ru.yandex.vertis.passport.util.crypt.Signer
import ru.yandex.vertis.passport.util.{LastIdentityRemoveException, SocialLoginNotAllowedException}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.util.Random
import ru.yandex.vertis.passport.service.log.EventLog
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.mockito.ArgumentMatchers.argThat

/**
  *
  * @author zvez
  */
//scalastyle:off multiple.string.literals
trait SocialUserServiceSpec
  extends FreeSpec
  with SpecBase
  with MockitoSupport
  with OptionValues
  with Inspectors
  with SpiedSocialProviders {
  this: DaoProvider =>

  val banService = new InMemoryBanService
  val linkDecider: SocialLinkDecider = mock[SocialLinkDecider]
  val featureManager: FeatureManager = mock[FeatureManager]
  when(featureManager.OneSocialProfileForProvider).thenReturn(featureOff)
  val eventLog = mock[EventLog]
  resetEventLog()

  private def resetEventLog(): Unit = {
    reset(eventLog)
    when(eventLog.logEvent(?)(?)).thenReturn(Future.successful(()))
  }

  /**
    * This spec relies on that this particular signer is used.
    */
  def signer: Signer

  def socialUserService: SocialUserService

  val sessionId = ModelGenerators.richSessionId.next

  implicit override val requestContext: RequestContext =
    RequestContext(ApiPayload("1", sessionId = Some(sessionId)), Traced.empty)

  val preparedSocialUserGen =
    ModelGenerators.socialUserSource.map(_.copy(phones = Nil, emails = Nil))

  "SocialUserService" - {
    "getSocializedUser (trusted)" - {
      val socialProvider = SocialProviders.Hsd

      commonTests(socialProvider) { socialUser =>
        socialUserService.getSocializedUser(socialProvider, socialUser)
      }

      "should fail if provider is not trusted" in {
        socialUserService
          .getSocializedUser(SocialProviders.VK, preparedSocialUserGen.next)
          .failed
          .futureValue shouldBe an[IllegalArgumentException]
      }
    }

    "getSocializedUser (oauth-ish)" - {
      val socialProvider = SocialProviders.VK

      commonTests(socialProvider) { socialUser =>
        val code = ModelGenerators.readableString.next
        val oauthState = signer.signAsUuid(sessionId.asString)
        when(socialProviderVk.getUser(code)).thenReturn(Future.successful(socialUser))
        socialUserService.getSocializedUser(socialProvider, SocialAuthorization.Code(code, oauthState))
      }
    }

    "should fail with IllegalArgumentException if provider is not supported" in {
      socialUserService
        .getSocializedUser(SocialProviders.OK, SocialAuthorization.Code("123", "state"))
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "should fail with OauthAuthenticationException if session id is invalid" in {
      socialUserService
        .getSocializedUser(SocialProviders.OK, SocialAuthorization.Code("123", "state"))
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "should fail with OAuthAuthorizationException if state is invalid" in {
      val code = ModelGenerators.readableString.next
      socialUserService
        .getSocializedUser(SocialProviders.VK, SocialAuthorization.Code(code, "state"))
        .failed
        .futureValue shouldBe an[OAuthAuthorizationException]
    }

    "add social profile to user" - {
      val socialProvider = SocialProviders.VK
      val oauthState = signer.signAsUuid(sessionId.asString)

      "should actually add it" in {
        val socialUser = preparedSocialUserGen.next
        val code = ModelGenerators.readableString.next
        when(socialProviderVk.getUser(code)).thenReturn(Future.successful(socialUser))

        val user = userDao
          .create(
            ModelGenerators.legacyUser.next
          )
          .futureValue

        resetEventLog()
        val res = socialUserService
          .addSocialProfile(
            user.id,
            AddSocialProfileParameters(socialProvider, SocialAuthorization.Code(code, oauthState))
          )
          .futureValue
        res.added shouldBe true

        val updatedUser = userDao.get(user.id).futureValue
        hasSocialProfile(updatedUser, socialProvider, socialUser) shouldBe true

        verify(eventLog).logEvent(
          argThat[Event] {
            case e: SocialUserLinked =>
              e.userId shouldBe user.id
              e.provider shouldBe socialProvider
              e.source shouldBe socialUser
              e.linkedBy shouldBe None
              e.candidates shouldBe empty
              true
            case _ => false
          }
        )(any())
      }

      "should work when user already has this social profile" in {
        val socialUser = preparedSocialUserGen.next
        val code = ModelGenerators.readableString.next
        when(socialProviderVk.getUser(code)).thenReturn(Future.successful(socialUser))

        val user = userDao
          .create(
            ModelGenerators.legacyUser.next.copy(
              socialProfiles = Seq(
                UserSocialProfile(
                  provider = socialProvider,
                  socialUser = SocialUser(socialUser.id)
                )
              )
            )
          )
          .futureValue

        resetEventLog()
        val res = socialUserService
          .addSocialProfile(
            user.id,
            AddSocialProfileParameters(socialProvider, SocialAuthorization.Code(code, oauthState))
          )
          .futureValue
        res.added shouldBe false

        val updatedUser = userDao.get(user.id).futureValue
        hasSocialProfile(updatedUser, socialProvider, socialUser) shouldBe true

        verifyNoMoreInteractions(eventLog)
      }

      "with feature OneSocialProfileForProvider" - {

        val socialUser1 = preparedSocialUserGen.next
        val socialUser2 = preparedSocialUserGen.next
        val socialUser3 = preparedSocialUserGen.next
        val socialUser4 = preparedSocialUserGen.next
        val code1 = ModelGenerators.readableString.next
        val code2 = ModelGenerators.readableString.next
        val code3 = ModelGenerators.readableString.next
        val code4 = ModelGenerators.readableString.next
        when(socialProviderVk.getUser(code1)).thenReturn(Future.successful(socialUser1))
        when(socialProviderVk.getUser(code2)).thenReturn(Future.successful(socialUser2))
        when(socialProviderHsd.getUser(code3)).thenReturn(Future.successful(socialUser3))
        when(socialProviderVk.getUser(code4)).thenReturn(Future.successful(socialUser4))

        val socialProfiles = Seq(
          UserSocialProfile(
            provider = SocialProviders.VK,
            socialUser = SocialUser(socialUser1.id)
          ),
          UserSocialProfile(
            provider = SocialProviders.VK,
            socialUser = SocialUser(socialUser2.id)
          ),
          UserSocialProfile(
            provider = SocialProviders.Hsd,
            socialUser = SocialUser(socialUser3.id)
          )
        )

        "enabled should interchange multiple social profiles of the same provider when a new one added" in {
          when(featureManager.OneSocialProfileForProvider).thenReturn(featureOn)
          val user = userDao.create(ModelGenerators.legacyUser.next.copy(socialProfiles = socialProfiles)).futureValue

          val res = socialUserService
            .addSocialProfile(
              user.id,
              AddSocialProfileParameters(SocialProviders.VK, SocialAuthorization.Code(code4, oauthState))
            )
            .futureValue
          res.added shouldBe true

          val updatedUser = userDao.get(user.id).futureValue
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser1) shouldBe false
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser2) shouldBe false
          hasSocialProfile(updatedUser, SocialProviders.Hsd, socialUser3) shouldBe true
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser4) shouldBe true
        }

        "disabled should keep multiple social profiles of the same provider when a new one added" in {
          when(featureManager.OneSocialProfileForProvider).thenReturn(featureOff)
          val user = userDao.create(ModelGenerators.legacyUser.next.copy(socialProfiles = socialProfiles)).futureValue

          val res = socialUserService
            .addSocialProfile(
              user.id,
              AddSocialProfileParameters(SocialProviders.VK, SocialAuthorization.Code(code4, oauthState))
            )
            .futureValue
          res.added shouldBe true

          val updatedUser = userDao.get(user.id).futureValue
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser1) shouldBe true
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser2) shouldBe true
          hasSocialProfile(updatedUser, SocialProviders.Hsd, socialUser3) shouldBe true
          hasSocialProfile(updatedUser, SocialProviders.VK, socialUser4) shouldBe true
        }

      }

      "should steal if social profile is linked to other user" in {
        val socialUser = preparedSocialUserGen.next
        val code = ModelGenerators.readableString.next
        when(socialProviderVk.getUser(code)).thenReturn(Future.successful(socialUser))

        val user1 = userDao
          .create(
            ModelGenerators.legacyUser.next
          )
          .futureValue

        val user2 = userDao
          .create(
            ModelGenerators.legacyUser.next.copy(
              socialProfiles = Seq(
                UserSocialProfile(
                  provider = socialProvider,
                  socialUser = SocialUser(socialUser.id)
                )
              )
            )
          )
          .futureValue

        val res = socialUserService.addSocialProfile(
          user1.id,
          AddSocialProfileParameters(socialProvider, SocialAuthorization.Code(code, oauthState))
        )
        res.futureValue.added shouldBe true

        userDao.get(user1.id).futureValue.socialProfiles.exists(_.id == socialUser.id) shouldBe true
        userDao.get(user2.id).futureValue.socialProfiles.exists(_.id == socialUser.id) shouldBe false
      }
    }

    "removeSocialProfile" - {
      "should remove it" in {
        val user = userDao
          .create(
            ModelGenerators.legacyUser
              .suchThat(u =>
                u.socialProfiles.nonEmpty
                  && (u.hasEmailOrPhone || u.socialProfiles.size > 1)
              )
              .next
          )
          .futureValue
        val sampleProfile = Random.shuffle(user.socialProfiles).head

        socialUserService
          .removeSocialProfile(user.id, RemoveSocialProfileParameters(sampleProfile.provider, sampleProfile.id))
          .futureValue

        val updatedUser = userDao.get(user.id).futureValue
        updatedUser.socialProfiles.size shouldBe (user.socialProfiles.size - 1)

        updatedUser.socialProfiles.contains(sampleProfile) shouldBe false
      }

      "should not allow to remove if there is no other auth method" in {
        val user = userDao
          .create(
            ModelGenerators.legacyUser
              .suchThat(u => u.socialProfiles.size == 1 && !u.hasEmailOrPhone)
              .next
          )
          .futureValue

        val profile = user.socialProfiles.head

        socialUserService
          .removeSocialProfile(user.id, RemoveSocialProfileParameters(profile.provider, profile.id))
          .failed
          .futureValue shouldBe an[LastIdentityRemoveException]
      }

      "remove if there is no other auth method, but removeLastIdentity flag is set" in {
        val user = userDao
          .create(
            ModelGenerators.legacyUser
              .suchThat(u => u.socialProfiles.size == 1 && !u.hasEmailOrPhone)
              .next
          )
          .futureValue

        val profile = user.socialProfiles.head

        socialUserService
          .removeSocialProfile(
            user.id,
            RemoveSocialProfileParameters(profile.provider, profile.id),
            removeLastIdentity = true
          )
          .futureValue
      }
    }

  }

  //scalastyle:off
  def commonTests(provider: SocialProvider)(getSocializedUser: SocialUserSource => Future[SocializedUser]): Unit = {

    "created" - {
      "create new user when email and phone are empty" in {
        val socialUser = preparedSocialUserGen.next
        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            user.socialProfiles should have size 1
            val profile = user.socialProfiles.head
            profile.provider shouldBe provider
            profile.socialUser.id shouldBe socialUser.id
          case other => fail("Unexpected result: " + other)
        }
      }

      "do not add fullName from social user info to user" in {
        val nickname, firstName, lastName = ModelGenerators.stringWithSurrogates.next
        val socialUser =
          preparedSocialUserGen.next.copy(
            nickname = Some(nickname),
            firstName = Some(firstName),
            lastName = Some(lastName)
          )
        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            val profile = user.profile.autoru
            profile.fullName shouldBe empty
          case other => fail("Unexpected result: " + other)
        }
      }

      "replace 4-byte unicode symbols with \ufffd in user info" in {
        val nickname, firstName, lastName = ModelGenerators.stringWithSurrogates.next
        val socialUser =
          preparedSocialUserGen.next.copy(
            nickname = Some(nickname),
            firstName = Some(firstName),
            lastName = Some(lastName)
          )
        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            val profile = user.profile.autoru
            forAll(Seq(profile.alias.value)) { name =>
              forAll(name) { c =>
                Character.isSurrogate(c) shouldBe false
              }
            }
          case other => fail("Unexpected result: " + other)
        }
      }

      "attach email and phones to new user" in {
        val phones = ModelGenerators.seq(ModelGenerators.phoneNumber).next
        val email = ModelGenerators.emailAddress.next
        val socialUser =
          preparedSocialUserGen.next
            .copy(phones = phones.map(SocialUserPhone(_)), emails = Seq(email))

        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            user.socialProfiles should have size 1
            hasSocialProfile(user, provider, socialUser) shouldBe true

            user.phones should have size phones.size
            user.phones.map(_.phone) should contain theSameElementsAs phones

            user.emails should have size 1
            user.emails.head.email shouldBe email
          case other => fail("Unexpected result: " + other)
        }
      }

      "shouldn't link or attach email if it is already taken by admin" in {
        when(linkDecider.allowLink(?, eq(false), eq(false))(?)).thenReturn(Future.successful(false))

        val email = ModelGenerators.emailAddress.next
        val socialUser = preparedSocialUserGen.next.copy(emails = Seq(email))

        val otherUser = ModelGenerators.legacyUser.next
          .copy(emails = Seq(UserEmail(email, confirmed = true)))
        userDao.create(otherUser).futureValue

        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            user.socialProfiles should have size 1
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.phones shouldBe empty
            user.emails shouldBe empty

          case other => fail("Unexpected result: " + other)
        }
      }

      "shouldn't link or attach phone if it is already taken by admin" in {
        when(linkDecider.allowLink(?, eq(false), eq(false))(?)).thenReturn(Future.successful(false))

        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next.copy(phones = Seq(SocialUserPhone(phone)))

        val otherUser = ModelGenerators.legacyUser.next
          .copy(phones = Seq(UserPhone(phone, added = None)))
        userDao.create(otherUser).futureValue

        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            user.socialProfiles should have size 1
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.phones shouldBe empty
            user.emails shouldBe empty

          case other => fail("Unexpected result: " + other)
        }
      }

      "shouldn't attach banned phone" in {
        val bannedPhone = ModelGenerators.phoneNumber.next

        banService.banPhone(bannedPhone, "test").futureValue

        val socialUser = preparedSocialUserGen.next.copy(phones = Seq(SocialUserPhone(bannedPhone)))

        getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            user.socialProfiles should have size 1
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.phones shouldBe empty
            user.emails shouldBe empty

          case other => fail("Unexpected result: " + other)
        }
      }
    }

    "linked" - {

      "prepare" in {
        when(linkDecider.allowLink(?, eq(false), eq(false))(?)).thenReturn(Future.successful(true))
      }

      "link to existing user by email" in {
        val email = ModelGenerators.emailAddress.next
        val socialUser = preparedSocialUserGen.next.copy(emails = Seq(email))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(emails = Seq(UserEmail(email, confirmed = true)))
          )
          .futureValue

        resetEventLog()
        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones shouldBe otherUser.phones
            user.emails shouldBe otherUser.emails
            linkedBy shouldBe Identity.Email(email)

          case other => fail("Unexpected result: " + other)
        }

        verify(eventLog).logEvent(
          argThat[Event] {
            case e: SocialUserLinked =>
              e.userId shouldBe otherUser.id
              e.provider shouldBe provider
              e.source shouldBe socialUser
              e.linkedBy shouldBe Some(Identity.Email(email))
              e.candidates should not be empty
              true
            case _ => false
          }
        )(any())
      }

      "link to existing user by phone" in {
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next.copy(phones = Seq(SocialUserPhone(phone)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next.copy(phones = Seq(UserPhone(phone, None)))
          )
          .futureValue

        resetEventLog()
        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones shouldBe otherUser.phones
            user.emails shouldBe otherUser.emails
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }

        verify(eventLog).logEvent(
          argThat[Event] {
            case e: SocialUserLinked =>
              e.userId shouldBe otherUser.id
              e.provider shouldBe provider
              e.source shouldBe socialUser
              e.linkedBy shouldBe Some(Identity.Phone(phone))
              e.candidates should not be empty
              true
            case _ => false
          }
        )(any())
      }

      "link by phone and attach email" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(phones = Seq(UserPhone(phone, added = None)), emails = Nil)
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones shouldBe otherUser.phones
            user.emails.exists(_.email == email) shouldBe true
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }
      }

      "link by email and attach phone" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(emails = Seq(UserEmail(email, confirmed = true)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.emails shouldBe otherUser.emails
            user.phones.exists(_.phone == phone) shouldBe true
            linkedBy shouldBe Identity.Email(email)

          case other => fail("Unexpected result: " + other)
        }
      }

      "link by phone and add more phones" in {
        val phone = ModelGenerators.phoneNumber.next
        val otherPhone = ModelGenerators.phoneNumber.next

        val socialUser = preparedSocialUserGen.next
          .copy(phones = Seq(phone, otherPhone).map(SocialUserPhone(_)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(phones = Seq(UserPhone(phone, added = None)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones.map(_.phone) should contain theSameElementsAs Seq(phone, otherPhone)
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }
      }

      "shouldn't try to attach second email to user (because it is not really supported)" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser
              .filter(_.emails.nonEmpty)
              .next
              .copy(phones = Seq(UserPhone(phone, added = None)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones shouldBe otherUser.phones
            user.emails shouldBe otherUser.emails
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }
      }

      "link by phone in case email and phone belong to different users, " +
        "but the one with email is admin" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val otherUser1 = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(emails = Seq(UserEmail(email, confirmed = true)))
          )
          .futureValue

        val otherUser2 = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(phones = Seq(UserPhone(phone, added = None)))
          )
          .futureValue

        when(linkDecider.allowLink(eq(otherUser1.id), eq(false), eq(false))(?)).thenReturn(Future.successful(false))

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser2.id
            user.phones shouldBe otherUser2.phones
            user.emails shouldBe otherUser2.emails
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }

        userDao.get(otherUser1.id).futureValue shouldBe otherUser1
      }

      "prefer email in case phone and email belong to different users" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val user1 = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(emails = Seq(UserEmail(email, confirmed = true)))
          )
          .futureValue

        val user2 = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(phones = Seq(UserPhone(phone, added = None)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe user1.id
            user.phones shouldBe user1.phones
            user.emails shouldBe user1.emails
            linkedBy shouldBe Identity.Email(email)

          case other => fail("Unexpected result: " + other)
        }

        userDao.get(user2.id).futureValue shouldBe user2
      }

      "ensure correct email and phones copied if linking to existing users" in {
        val email = ModelGenerators.emailAddress.next
        val phone = ModelGenerators.phoneNumber.next
        val socialUser = preparedSocialUserGen.next
          .copy(emails = Seq(email), phones = Seq(SocialUserPhone(phone)))

        val user1 = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(emails = Seq(UserEmail(email, confirmed = true)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe user1.id
            socialUser.emails.toSet.subsetOf(user.emails.map(_.email).toSet) shouldBe true
            socialUser.phones
              .map(_.phone)
              .toSet
              .subsetOf(user.phones.map(_.phone).toSet) shouldBe true

            linkedBy shouldBe Identity.Email(email)

          case other => fail("Unexpected result: " + other)
        }
      }

      "shouldn't attach banned phone" in {
        val phone = ModelGenerators.phoneNumber.next
        val bannedPhone = ModelGenerators.phoneNumber.next

        banService.banPhone(bannedPhone, "test").futureValue

        val socialUser = preparedSocialUserGen.next
          .copy(phones = Seq(phone, bannedPhone).map(SocialUserPhone(_)))

        val otherUser = userDao
          .create(
            ModelGenerators.legacyUser.next
              .copy(phones = Seq(UserPhone(phone, added = None)))
          )
          .futureValue

        getSocializedUser(socialUser).futureValue match {
          case Linked(user, linkedBy) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user.id shouldBe otherUser.id
            user.phones.map(_.phone) should contain theSameElementsAs Seq(phone)
            linkedBy shouldBe Identity.Phone(phone)

          case other => fail("Unexpected result: " + other)
        }
      }
    }

    "found" - {

      "prepare" in {
        when(linkDecider.allowLogin(?, ?, eq(false), eq(false))(?)).thenReturn(Future.successful(true))
      }

      "return existing user" in {
        val socialUser = preparedSocialUserGen.next
        val createdUser = getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user
          case other => fail("Unexpected result: " + other)
        }

        getSocializedUser(socialUser).futureValue match {
          case Found(user) =>
            user shouldBe createdUser
          case other => fail("Unexpected result: " + other)
        }
      }

      "don't allow to login if user is special" in {
        val socialUser = preparedSocialUserGen.next
        val createdUser = getSocializedUser(socialUser).futureValue match {
          case Created(user) =>
            hasSocialProfile(user, provider, socialUser) shouldBe true
            user
          case other => fail("Unexpected result: " + other)
        }

        when(linkDecider.allowLogin(eq(createdUser), ?, eq(false), eq(false))(?)).thenReturn(Future.successful(false))

        getSocializedUser(socialUser).failed.futureValue shouldBe an[SocialLoginNotAllowedException]
      }
    }

  }

  private def hasSocialProfile(user: FullUser, provider: SocialProvider, socialUser: SocialUserSource) =
    user.socialProfiles.exists { profile =>
      profile.provider == provider && profile.socialUser.id == socialUser.id
    }

}

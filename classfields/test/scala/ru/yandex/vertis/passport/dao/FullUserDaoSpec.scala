package ru.yandex.vertis.passport.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{CancelAfterFailure, WordSpec}
import ru.yandex.vertis.passport.dao.FullUserDao.{DuplicateKeyException, FindBy}
import ru.yandex.vertis.passport.model.{FullUser, Identity, SocialProviders, UserEmail, UserPhone, UserSocialProfile}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  * Tests for [[FullUserDao]]
  *
  * @author zvez
  */
trait FullUserDaoSpec extends WordSpec with SpecBase with CancelAfterFailure {

  val userDao: FullUserDao
  private val SocialProviderGen = Gen.oneOf(SocialProviders.values.toSeq)

  val userGenerator: Gen[FullUser] = ModelGenerators.fullUser.map(_.copy(id = ""))
  private val SomeEmail = "some@test.com"
  private val SomePhone = "77777777123"
  private val SomeSocialProvider = SocialProviderGen.next
  private val SomeSocialUser = ModelGenerators.socialProviderUser.next

  "FullUserDao" should {

    var createdUser: FullUser = null

    def cleanExtraEmails(in: FullUser): FullUser = {
      in.copy(socialProfiles =
        in.socialProfiles.map(sp =>
          sp.copy(socialUser = sp.socialUser.copy(emails = Seq(sp.socialUser.emails.headOption).flatten))
        )
      )
    }

    "create user" in {
      val user = userGenerator.next
      val singleEmailUser = cleanExtraEmails(user)
      createdUser = userDao.create(singleEmailUser).futureValue
      createdUser.id shouldNot be("")
      createdUser shouldBe singleEmailUser.copy(id = createdUser.id)
    }

    "get user by id" in {
      val foundUser = userDao.get(createdUser.id).futureValue
      foundUser shouldBe createdUser
    }

    "get essentials" in {
      val foundUser = userDao.getUserEssentials(createdUser.id).futureValue
      foundUser shouldBe createdUser.asEssentials
    }

    "update user" in {
      userDao
        .update(createdUser.id) { user =>
          {
            user.copy(
              pwdHash = Some("changed")
            )
          }
        }
        .futureValue

      userDao.get(createdUser.id).futureValue.pwdHash shouldBe Some("changed")
    }

    "set and remove clientId" in {
      val clientId = ModelGenerators.clientId.next
      userDao.setClientId(createdUser.id, clientId, clientGroup = None).futureValue
      val afterSet = userDao.get(createdUser.id).futureValue
      afterSet.profile.clientId shouldBe Some(clientId)
      afterSet.profile.autoru.clientGroup shouldBe None
      userDao.removeClientId(createdUser.id).futureValue
      val afterDelete = userDao.get(createdUser.id).futureValue
      afterDelete.profile.clientId shouldBe None
      afterDelete.profile.autoru.clientGroup shouldBe None
    }

    "set and remove clientId with user clientGroup" in {
      val clientId = ModelGenerators.clientId.next
      userDao.setClientId(createdUser.id, clientId, clientGroup = Some("custom-group")).futureValue
      val afterSet = userDao.get(createdUser.id).futureValue
      afterSet.profile.clientId shouldBe Some(clientId)
      afterSet.profile.autoru.clientGroup shouldBe Some("custom-group")
      userDao.removeClientId(createdUser.id).futureValue
      val afterDelete = userDao.get(createdUser.id).futureValue
      afterDelete.profile.clientId shouldBe None
      afterDelete.profile.autoru.clientGroup shouldBe None
    }

    "overwrites non-active phone" in {
      val nonActivePhone = ModelGenerators.userPhone.next.copy(active = false)
      val user1 = userDao.create {
        userGenerator.next.copy(phones = Seq(nonActivePhone))
      }.futureValue

      userDao.get(user1.id).futureValue.phones shouldBe Seq(nonActivePhone)

      val activePhone = nonActivePhone.copy(active = true)
      val user2 = userDao.create {
        userGenerator.next.copy(phones = Seq(activePhone))
      }.futureValue

//      userDao.get(user1.id).futureValue.phones shouldBe Nil
      userDao.get(user2.id).futureValue.phones shouldBe Seq(activePhone)
    }

    "find by email" in {
      val user = userDao.create {
        cleanExtraEmails(
          userGenerator.next.copy(emails = Seq(UserEmail(SomeEmail, confirmed = true)))
        )
      }.futureValue

      userDao.find(FindBy.Email(SomeEmail)).futureValue shouldBe Some(user)
      userDao.find(FindBy.Email("other@email.com")).futureValue shouldBe None

      userDao.findId(FindBy.Email(SomeEmail)).futureValue shouldBe Some(user.id)
      userDao.findId(FindBy.Email("other@email.com")).futureValue shouldBe None
    }

    "not allow to create user with non-unique email" in {
      userDao
        .create {
          userGenerator.next.copy(emails = Seq(UserEmail(SomeEmail, confirmed = true)))
        }
        .failed
        .futureValue shouldBe an[DuplicateKeyException]
    }

    "find by phone" in {
      val user = userDao.create {
        val phone = UserPhone(SomePhone, Some(DateTime.now().withMillisOfSecond(0)))
        cleanExtraEmails(userGenerator.next.copy(phones = Seq(phone)))
      }.futureValue

      userDao.find(FindBy.Phone(SomePhone)).futureValue shouldBe Some(user)
      userDao.find(FindBy.Phone("123456")).futureValue shouldBe None

      userDao.findId(FindBy.Phone(SomePhone)).futureValue shouldBe Some(user.id)
      userDao.findId(FindBy.Phone("123456")).futureValue shouldBe None
    }

    "find by phone should not see non-active phones" in {
      val nonActivePhone = ModelGenerators.userPhone.next.copy(active = false)
      val activePhone = ModelGenerators.userPhone.next.copy(active = true)
      val user = userDao.create {
        cleanExtraEmails(userGenerator.next.copy(phones = Seq(nonActivePhone, activePhone)))
      }.futureValue

      userDao.find(FindBy.Phone(activePhone.phone)).futureValue shouldBe Some(user)
      userDao.find(FindBy.Phone(nonActivePhone.phone)).futureValue shouldBe None
    }

    "find phones' owners" in {
      val user1 = userDao.create(userGenerator.next).futureValue
      val user2 = userDao.create(userGenerator.next).futureValue

      val allPhones = (user1.phones ++ user2.phones).map(_.phone)
      val res = userDao.findPhonesOwners(allPhones).futureValue
      res.keys should contain theSameElementsAs allPhones
    }

    "not allow to create user with non-unique phone" in {
      userDao
        .create {
          userGenerator.next.copy(phones = Seq(UserPhone(SomePhone)))
        }
        .failed
        .futureValue shouldBe an[DuplicateKeyException]
    }

    "find by social" in {
      val user = userDao.create {
        val socialProfile = UserSocialProfile(
          SomeSocialProvider,
          SomeSocialUser,
          DateTime.now().withMillisOfSecond(0),
          activated = true,
          None
        )
        cleanExtraEmails(userGenerator.next.copy(socialProfiles = Seq(socialProfile)))
      }.futureValue

      userDao.find(FindBy.SocialProfile(SomeSocialProvider, SomeSocialUser.id)).futureValue shouldBe Some(user)
      userDao.find(FindBy.SocialProfile(SocialProviderGen.next, "123")).futureValue shouldBe None

      userDao.findId(FindBy.SocialProfile(SomeSocialProvider, SomeSocialUser.id)).futureValue shouldBe Some(user.id)
      userDao.findId(FindBy.SocialProfile(SocialProviderGen.next, "123")).futureValue shouldBe None
    }

    "allow messing with identities" in {
      val user = userDao.create(userGenerator.next).futureValue
      userGenerator.next(5).foldLeft(user) { (current, upd) =>
        val updatedUser = userDao
          .update(current.id) { user =>
            user.copy(emails = upd.emails, phones = upd.phones, socialProfiles = upd.socialProfiles)
          }
          .futureValue

        current.emails.foreach { email =>
          userDao.find(FindBy.Email(email.email)).futureValue shouldBe None
        }
        current.phones.foreach { phone =>
          userDao.find(FindBy.Phone(phone.phone)).futureValue shouldBe None
        }
        current.socialProfiles.foreach { sp =>
          userDao.find(FindBy.SocialProfile(sp.provider, sp.id)).futureValue shouldBe None
        }

        updatedUser.emails.foreach { email =>
          userDao.find(FindBy.Email(email.email)).futureValue shouldBe Some(updatedUser)
        }
        updatedUser.phones.foreach { phone =>
          userDao.find(FindBy.Phone(phone.phone)).futureValue shouldBe Some(updatedUser)
        }
        updatedUser.socialProfiles.foreach { sp =>
          userDao.find(FindBy.SocialProfile(sp.provider, sp.id)).futureValue shouldBe Some(updatedUser)
        }

        updatedUser
      }
    }

    "replace user profile" in {
      ModelGenerators.userProfile.next(5).foreach { profile =>
        userDao
          .update(createdUser.id) { user =>
            {
              user.copy(
                profile = profile
              )
            }
          }
          .futureValue

        userDao.get(createdUser.id).futureValue.profile shouldBe profile
      }
    }

    "create/update allowOffersShow" in {
      val user = userGenerator.next
      // create
      val userId =
        userDao.create(user.copy(profile = user.profile.autoru.copy(allowOffersShow = Some(true)))).futureValue.id
      userDao.get(userId).futureValue.profile.autoru.allowOffersShow shouldBe Some(true)
      // update field
      userDao.update(userId)(u => u.copy(profile = u.profile.autoru.copy(allowOffersShow = Some(false)))).futureValue
      userDao.get(userId).futureValue.profile.autoru.allowOffersShow shouldBe Some(false)
      // delete field
      userDao.update(userId)(u => u.copy(profile = u.profile.autoru.copy(allowOffersShow = None))).futureValue
      userDao.get(userId).futureValue.profile.autoru.allowOffersShow shouldBe None
    }

    "read allowOffersShow in UserEssentials" in {
      val user = userGenerator.next
      // create
      val userId =
        userDao.create(user.copy(profile = user.profile.autoru.copy(allowOffersShow = Some(true)))).futureValue.id
      userDao.getUserEssentials(userId).futureValue.allowOffersShow shouldBe Some(true)
      // update field
      userDao.update(userId)(u => u.copy(profile = u.profile.autoru.copy(allowOffersShow = Some(false)))).futureValue
      userDao.getUserEssentials(userId).futureValue.allowOffersShow shouldBe Some(false)
      // delete field
      userDao.update(userId)(u => u.copy(profile = u.profile.autoru.copy(allowOffersShow = None))).futureValue
      userDao.getUserEssentials(userId).futureValue.allowOffersShow shouldBe None
    }

    "delete user" in {
      val user = userGenerator.next
      val createdUser = userDao.create(user).futureValue
      userDao.delete(createdUser.id).futureValue

      userDao.get(createdUser.id).failed.futureValue shouldBe a[NoSuchElementException]
    }
  }

}

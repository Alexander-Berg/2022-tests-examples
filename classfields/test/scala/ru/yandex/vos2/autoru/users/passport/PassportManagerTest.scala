package ru.yandex.vos2.autoru.users.passport

import com.google.protobuf.util.Timestamps
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.passport.model.api.ApiModel.{User, UserEmail, UserIdentity, UserPhone}
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.model.proto.{Event, EventPayload, IdentityAdded}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.services.passport.PassportClient
import ru.yandex.vos2.util.Protobuf._
import ru.yandex.vos2.{getNow, UserModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by sievmi on 13.09.18
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PassportManagerTest extends AnyFunSuite with MockitoSupport with InitTestDbs {

  initDbs()
  implicit private val t: Traced = Traced.empty

  test("update user") {
    val passportUser: User =
      createPassportUser("new_email@yandex.ru", "+79999999999", allowOffersShow = Some(true), alias = Some("alias"))
    val moderation = createPassportModeration(true)
    val identityAddedEvent = createIdentity("a_123", Some("new_email@yandex.ru"), None)
    val passportClient = mock[PassportClient]
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))
    when(passportClient.getModeration(?)(?)).thenReturn(Some(moderation))

    val vosUser = createVosUser("a_123", Some("old_email@yandex.ru"), None)
    components.userDao.update(vosUser)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
      assert(!dbUser.getProfile.getAllowOffersShow)
      assert(dbUser.getProfile.getAlias.isEmpty)
    }

    val passportManager =
      new PassportManager(components.userDao, components.getOfferDao, passportClient, components.autoruSalonsDao)

    val events = Seq(identityAddedEvent)

    passportManager.scheduleUpdateUserWithOffers(UserRef.from("a_123"), events)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "new_email@yandex.ru")
      assert(dbUser.getUserContacts.getPhones(0).getNumber === "79999999999")
      assert(dbUser.getProfile.getAllowOffersShow)
      assert(dbUser.getProfile.getAlias == "alias")
    }
  }

  test("don't update user if events is empty") {

    val passportUser: User = createPassportUser("new_email@yandex.ru", "+79999999999")
    val moderation = createPassportModeration(true)
    val passportClient = mock[PassportClient]
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))
    when(passportClient.getModeration(?)(?)).thenReturn(Some(moderation))

    val vosUser = createVosUser("a_123", Some("old_email@yandex.ru"), None)
    components.userDao.update(vosUser)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
    }

    val passportManager =
      PassportManager(components.userDao, components.getOfferDao, passportClient, components.autoruSalonsDao)

    val events = Seq()

    passportManager.scheduleUpdateUserWithOffers(UserRef.from("a_123"), events)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
      assert(dbUser.getUserContacts.getPhonesList.isEmpty === true)
    }
  }

  test("don't update user if passport retunrs exception") {

    val passportClient = mock[PassportClient]
    when(passportClient.getUser(?)(?)).thenReturn(Failure(new IllegalArgumentException("test")))
    when(passportClient.getModeration(?)(?)).thenReturn(None)

    val vosUser = createVosUser("a_123", Some("old_email@yandex.ru"), None)
    components.userDao.update(vosUser)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
    }

    val passportManager =
      PassportManager(components.userDao, components.getOfferDao, passportClient, components.autoruSalonsDao)

    val events = Seq()

    passportManager.scheduleUpdateUserWithOffers(UserRef.from("a_123"), events)

    components.userDao.useIfExist(UserRef.from("a_123")) { dbUser =>
      assert(dbUser.getUserRef === "a_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
      assert(dbUser.getUserContacts.getPhonesList.isEmpty === true)
    }
  }

  private def createVosUser(userRef: String, emailOpt: Option[String], phoneOpt: Option[String]) = {
    val builder = UserModel.User.newBuilder()
    builder.setUserRef(userRef)
    emailOpt.foreach(builder.getUserContactsBuilder.setEmail)
    phoneOpt.foreach(phone =>
      builder.getUserContactsBuilder.addPhones(UserModel.UserPhone.newBuilder().setNumber(phone))
    )

    builder.build()
  }

  private def createPassportUser(email: String,
                                 phone: String,
                                 allowOffersShow: Option[Boolean] = None,
                                 alias: Option[String] = None): User = {
    val builder = User.newBuilder()
    builder.addEmails(
      UserEmail
        .newBuilder()
        .setConfirmed(true)
        .setEmail(email)
    )
    builder.addPhones(
      UserPhone
        .newBuilder()
        .setPhone(phone)
        .setAdded(Timestamps.fromMillis(getNow))
    )

    allowOffersShow.foreach(allowOffersShow =>
      builder.getProfileBuilder.getAutoruBuilder.setAllowOffersShow(allowOffersShow)
    )
    alias.foreach(alias => builder.getProfileBuilder.getAutoruBuilder.setAlias(alias))

    builder.build()
  }

  private def createPassportModeration(reseller: Boolean): UserModerationStatus = {
    UserModerationStatus.newBuilder().setReseller(reseller).build()
  }

  private def createIdentity(id: String, email: Option[String], phone: Option[String]) = {
    val builder = Event.newBuilder()
    builder.setDomain("AUTO")
    val payloadBuilder = EventPayload.newBuilder()

    val identityBuilder: UserIdentity = {
      if (email.isDefined) UserIdentity.newBuilder().setEmail(email.get)
      else UserIdentity.newBuilder().setPhone(phone.get)
    }.build()

    payloadBuilder.setIdentityAdded(
      IdentityAdded
        .newBuilder()
        .setWithoutConfirmation(false)
        .setIdentity(identityBuilder)
    )
    payloadBuilder.setUserId(id)
    builder.setPayload(payloadBuilder)
    builder.build()
  }
}

package ru.yandex.realty.managers.chat

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.managers.chat.push.message.ChatNewMessage
import ru.yandex.realty.chat.model.ChatUtil.TechSupportUserId
import ru.yandex.realty.pushnoy.PushnoyClient
import ru.yandex.realty.proto.api.v2.chat.{Subject, User}
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.UserProfileLight
import ru.yandex.vertis.chat.model.events.EventsModel
import ru.yandex.realty.proto.unified.offer.address.{Address, LocationUnified}
import ru.yandex.realty.clients.BnbSearcherClient
import ru.yandex.realty.clients.blackbox.BlackboxClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.errors.ProtoErrorResponseException
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.RegionType
import akka.http.scaladsl.model.StatusCode
import ru.yandex.realty.api.ProtoResponse.ErrorResponse
import ru.yandex.realty.clients.rent.chat.RentChatServiceClient
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.storage.DeveloperWithChatStorageImpl
import ru.yandex.realty.tracing.Traced

import scala.util.{Failure, Success, Try}

sealed trait UserProfile
case class FullName(fullName: String) extends UserProfile
case class Alias(alias: String) extends UserProfile

@RunWith(classOf[JUnitRunner])
class DefaultChatPushManagerSpec extends AsyncSpecBase {
  private val pushnoyClient = mock[PushnoyClient]
  private val blackboxClient = mock[BlackboxClient]
  private val vosClient = mock[VosClientNG]
  private val searcherClient = mock[SearcherClient]
  private val bnbSearcherClient = mock[BnbSearcherClient]
  private val rentChatClient = mock[RentChatServiceClient]
  private val chatEnricher = new ChatEnricher(blackboxClient, vosClient, searcherClient, bnbSearcherClient)
  private val mdsUrlBuilder = new MdsUrlBuilder("//avatars.mds.yandex.net")
  private val aliases = Seq.empty
  private val imageEnricher = new ImageEnricher(mdsUrlBuilder, aliases)
  private val responseBuilder = new ChatResponseBuilder(
    imageEnricher,
    chatEnricher,
    new ProviderAdapter(new DeveloperWithChatStorageImpl(Set.empty)),
    rentChatClient
  )
  private val chatPushManager = new DefaultChatPushManager(pushnoyClient, responseBuilder)
  private val offerOwnerId = "1234567890"
  private val customerId = "1234567891"
  implicit private val t: Traced = Traced.empty

  "DefaultChatPushManagerSpec in getPushTitle" should {
    "return default tech support title if a room is a tech support room" in {
      val extUsers = buildExtUsers(Map(TechSupportUserId -> UserProfileLight.getDefaultInstance))
      val extEvent = buildExtEvent(Some(TechSupportUserId))
      val subject = buildSubject()
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe ChatNewMessage.DefaultTechSupportTitle
    }

    "return the default title if a room is not tech support and a subject is failure" in {
      val extUsers = buildExtUsers(Map(customerId -> UserProfileLight.getDefaultInstance))
      val extEvent = buildExtEvent(Some(customerId))
      val subject =
        Failure(new ProtoErrorResponseException(StatusCode.int2StatusCode(500), ErrorResponse.getDefaultInstance))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe ChatNewMessage.DefaultTitle
    }
  }

  "DefaultChatPushManagerSpec in getPushTitle and an author is a customer" should {
    "return a name if the author has only the name" in {
      val offerOwnerName = "Homer"
      val customerName = "Peter"
      val users =
        Map(
          offerOwnerId -> buildUserProfile(FullName(offerOwnerName)),
          customerId -> buildUserProfile(FullName(customerName))
        )
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(customerId))
      val subject = buildSubject(Some(offerOwnerId))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe customerName
    }

    "return a name and first letter of last name if an author has both the name and last name" in {
      val offerOwnerName = "Homer Simpson"
      val customerName = "Peter Griffin"
      val users =
        Map(
          offerOwnerId -> buildUserProfile(FullName(offerOwnerName)),
          customerId -> buildUserProfile(FullName(customerName))
        )
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(customerId))
      val subject = buildSubject(Some(offerOwnerId))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe "Peter G"
    }

    "return an alias if an author hasn't the name but has the alias" in {
      val offerOwnerAlias = "homer2021"
      val customerAlias = "peter2020"
      val users =
        Map(
          offerOwnerId -> buildUserProfile(Alias(offerOwnerAlias)),
          customerId -> buildUserProfile(Alias(customerAlias))
        )
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(customerId))
      val subject = buildSubject(Some(offerOwnerId))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe customerAlias
    }

    "return default title if an author hasn't neither full name nor alias" in {
      val users =
        Map(
          offerOwnerId -> UserProfileLight.getDefaultInstance,
          customerId -> UserProfileLight.getDefaultInstance
        )
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(customerId))
      val subject = buildSubject(Some(offerOwnerId))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe ChatNewMessage.DefaultTitle
    }
  }

  "DefaultChatPushManagerSpec in getPushTitle and an author is seller" should {
    "return a street name and house number if it exist" in {
      val users = {
        Map(
          offerOwnerId -> UserProfileLight.getDefaultInstance,
          customerId -> UserProfileLight.getDefaultInstance
        )
      }
      val streetName = "street"
      val houseNumber = "123"
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(offerOwnerId))
      val subject = buildSubject(Some(offerOwnerId), Some(streetName), Some(houseNumber))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe s"$streetName, $houseNumber"
    }

    "return a street name if an offer has only street name" in {
      val users = {
        Map(
          offerOwnerId -> UserProfileLight.getDefaultInstance,
          customerId -> UserProfileLight.getDefaultInstance
        )
      }
      val streetName = "street"
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(offerOwnerId))
      val subject = buildSubject(Some(offerOwnerId), Some(streetName))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe streetName
    }

    "return default title if an offer hasn't a street and house number" in {
      val users =
        Map(
          offerOwnerId -> UserProfileLight.getDefaultInstance,
          customerId -> UserProfileLight.getDefaultInstance
        )
      val extUsers = buildExtUsers(users)
      val extEvent = buildExtEvent(Some(offerOwnerId))
      val subject = buildSubject(Some(offerOwnerId))
      chatPushManager.getPushTitle(extEvent, extUsers, subject) shouldBe ChatNewMessage.DefaultTitle
    }
  }

  private def buildExtUsers(users: Map[String, UserProfileLight]): Seq[UserExtended[User]] = {
    val extUsers = for {
      (id, profile) <- users
    } yield {
      val user = User.newBuilder().setId(id).setProfile(profile).build()
      UserExtended(user, isBanned = false, isBanTemporary = false, isAgency = false)
    }
    extUsers.toSeq
  }

  private def buildUserProfile(profile: UserProfile): UserProfileLight = {
    val b = UserProfileLight.newBuilder()
    profile match {
      case FullName(fullName) => b.setFullName(fullName)
      case Alias(alias) => b.setAlias(alias)
    }
    b.build()
  }

  private def buildSubject(
    offerOwnerIdOpt: Option[String] = None,
    streetNameOpt: Option[String] = None,
    houseNumberOpt: Option[String] = None
  ): Try[Option[Subject]] = {
    val unifiedOfferBuilder = UnifiedOffer.newBuilder()
    offerOwnerIdOpt.foreach(unifiedOfferBuilder.setUserRef)
    unifiedOfferBuilder.setLocation(buildLocation(streetNameOpt, houseNumberOpt))

    val subject = Subject
      .newBuilder()
      .setOffer(
        unifiedOfferBuilder
      )
      .build()
    Success(Some(subject))
  }

  private def buildLocation(streetNameOpt: Option[String], houseNumberOpt: Option[String]): LocationUnified = {
    val locationBuilder = LocationUnified.newBuilder()
    val addressBuilder = Address.newBuilder
    val streetNameComponentBuilder = Address.Component.newBuilder()
    val houseNumberComponentBuilder = Address.Component.newBuilder()
    streetNameOpt.foreach(streetName => {
      streetNameComponentBuilder.setRegionType(RegionType.STREET)
      streetNameComponentBuilder.setValue(streetName)
    })
    houseNumberOpt.foreach(houseNumber => {
      houseNumberComponentBuilder.setRegionType(RegionType.HOUSE)
      houseNumberComponentBuilder.setValue(houseNumber)
    })
    addressBuilder.addComponent(streetNameComponentBuilder.build())
    addressBuilder.addComponent(houseNumberComponentBuilder.build())
    locationBuilder.setUnifiedAddress(addressBuilder).build()
  }

  private def buildExtEvent(authorOpt: Option[String] = None): EventsModel.MessageSent = {
    val b = EventsModel.MessageSent.newBuilder()
    authorOpt.foreach(
      author => {
        b.setMessage(ApiModel.Message.newBuilder().setAuthor(author).build())
        b.setRoom(ApiModel.Room.newBuilder().addUserIds(author).build())
      }
    )
    b.build()
  }
}

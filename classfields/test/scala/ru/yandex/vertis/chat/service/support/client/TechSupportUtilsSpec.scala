package ru.yandex.vertis.chat.service.support.client

import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.passport.model.api.ApiModel.{User => PassportModelUser, _}
import ru.yandex.vertis.chat._
import ru.yandex.vertis.chat.common.techsupport.TechSupport.{ChatUser, UserMessage}
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils.RichSendMessageResult
import ru.yandex.vertis.chat.common.techsupport.{TechSupport, TechSupportUtils}
import ru.yandex.vertis.chat.components.clients.pushnoy.DeviceInfo
import ru.yandex.vertis.chat.model.api.ApiModel.Attachment
import ru.yandex.vertis.chat.model.{Message, MessagePayload, ModelGenerators, Participants, User}
import ru.yandex.vertis.chat.service.{CreateMessageParameters, CreateRoomParameters, RoomLocator, SendMessageResult}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.image.Image
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.{Identity, MimeType, RawMdsIdentity}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
class TechSupportUtilsSpec extends WordSpec with Matchers {
  "TechSupportUtils.RichSendMessageResult.asRequestFromChatUser" should {
    val aliasSmall = "320x320"
    val aliasBig = "1200x1200"
    val imageCommonPart = "//avatars.mds.yandex.net/get-vertis-chat/814462/7fc67fe5e9f775ef9487fd06e44a96c4/"
    "render request to tech support with image correctly" in {
      val image1 = imageCommonPart + aliasBig
      val image2 = imageCommonPart + "460x460"
      val image3 = imageCommonPart + aliasSmall
      val userIdLong = "10245464"
      val userId = "user:" + userIdLong
      val message = SendMessageResult(
        message = Message(
          id = "11e87b88e32ac5c0892f215e367edb20",
          providedId = None,
          roomId = "4ab7f54c586b98882023a6f9aeeed672",
          author = userId,
          created = new DateTime(2018, 6, 29, 10, 40, 49, 180),
          payload = model.MessagePayload(MimeType.TEXT_PLAIN, ""),
          attachments = Seq(
            Attachment
              .newBuilder()
              .setImage(
                Image
                  .newBuilder()
                  .putAllSizes(
                    Map(
                      aliasBig -> image1,
                      "460x460" -> image2,
                      aliasSmall -> image3
                    ).asJava
                  )
              )
              .build()
          ),
          isSilent = false,
          isSpam = false
        ),
        previousMessage = None
      )
      val email = "example@example.com"
      val phone = "79291112233"
      val userPic = "//avatars.mds.yandex.net/get-vertis-chat/814462/310903b35bbb9520310c/" + aliasSmall
      val fullName = "Андрей"
      implicit val userResult: UserResult = UserResult
        .newBuilder()
        .setUser(
          PassportModelUser
            .newBuilder()
            .setId(userIdLong)
            .addPhones(UserPhone.newBuilder().setPhone(phone))
            .addEmails(UserEmail.newBuilder().setEmail(email).setConfirmed(true))
            .setProfile(
              UserProfile
                .newBuilder()
                .setAutoru(
                  AutoruUserProfile
                    .newBuilder()
                    .setFullName(fullName)
                    .setAlias("alias")
                    .setUserpic(
                      ImageUrl
                        .newBuilder()
                        .putSizes(aliasSmall, userPic)
                    )
                )
            )
        )
        .build()
      implicit val rc: UserRequestContext = UserRequestContext(
        id = "",
        requester = Client(),
        user = userId,
        passportId = userIdLong.toLong,
        cacheControl = CacheControl.Default,
        isInternal = false,
        trace = Traced.empty,
        idempotencyKey = None
      )
      implicit val mdsReadUrlPrefix: String = "//avatars.mds.yandex.net"
      implicit val deviceInfo: Option[DeviceInfo] = Some(DeviceInfo(Some("android"), Some("5.2.1")))
      val request = message.asRequestFromChatUser
      val protocolPrefix = "http:"
      request shouldBe TechSupport.Request(
        sender = Some(
          ChatUser(
            id = Some(userId),
            name = Some(fullName + " (user:" + userIdLong + ")"),
            phone = Some(phone),
            email = Some(email),
            photo = Some(protocolPrefix + userPic),
            url = None,
            crm_link = Some("https://moderation.vertis.yandex-team.ru/autoru?user_id=" + userIdLong),
            invite = None
          )
        ),
        message = Some(
          UserMessage(
            `type` = "photo",
            text = Some(""),
            file = Some(protocolPrefix + image1),
            thumb = Some(protocolPrefix + image3)
          )
        ),
        recipient = None
      )
    }

    "render request to tech support with image as raw mds data" in {
      val image1 = imageCommonPart + aliasBig
      val image3 = imageCommonPart + aliasSmall
      val userIdLong = "10245464"
      val userId = "user:" + userIdLong
      val message = SendMessageResult(
        message = Message(
          id = "11e87b88e32ac5c0892f215e367edb20",
          providedId = None,
          roomId = "4ab7f54c586b98882023a6f9aeeed672",
          author = userId,
          created = new DateTime(2018, 6, 29, 10, 40, 49, 180),
          payload = model.MessagePayload(MimeType.TEXT_PLAIN, ""),
          attachments = Seq(
            Attachment
              .newBuilder()
              .setImage(
                Image
                  .newBuilder()
                  .setId(
                    Identity
                      .newBuilder()
                      .setRawMds(
                        RawMdsIdentity
                          .newBuilder()
                          .setGroupId(814462)
                          .setName("7fc67fe5e9f775ef9487fd06e44a96c4")
                          .setNamespace("vertis-chat")
                      )
                  )
              )
              .build()
          ),
          isSilent = false,
          isSpam = false
        ),
        previousMessage = None
      )
      val email = "example@example.com"
      val phone = "79291112233"
      val userPic = "//avatars.mds.yandex.net/get-vertis-chat/814462/310903b35bbb9520310c/" + aliasSmall
      val fullName = "Андрей"
      implicit val userResult: UserResult = UserResult
        .newBuilder()
        .setUser(
          PassportModelUser
            .newBuilder()
            .setId(userIdLong)
            .addPhones(UserPhone.newBuilder().setPhone(phone))
            .addEmails(UserEmail.newBuilder().setEmail(email).setConfirmed(true))
            .setProfile(
              UserProfile
                .newBuilder()
                .setAutoru(
                  AutoruUserProfile
                    .newBuilder()
                    .setFullName(fullName)
                    .setAlias("alias")
                    .setUserpic(
                      ImageUrl
                        .newBuilder()
                        .putSizes(aliasSmall, userPic)
                    )
                )
            )
        )
        .build()
      implicit val rc: UserRequestContext = UserRequestContext(
        id = "",
        requester = Client(),
        user = userId,
        passportId = userIdLong.toLong,
        cacheControl = CacheControl.Default,
        isInternal = false,
        trace = Traced.empty,
        idempotencyKey = None
      )
      implicit val mdsReadUrlPrefix: String = "//avatars.mds.yandex.net"
      implicit val deviceInfo: Option[DeviceInfo] = Some(DeviceInfo(Some("ios"), Some("8.5.0")))
      val request = message.asRequestFromChatUser
      val protocolPrefix = "http:"
      request shouldBe TechSupport.Request(
        sender = Some(
          ChatUser(
            id = Some(userId),
            name = Some(fullName + " (user:" + userIdLong + ")"),
            phone = Some(phone),
            email = Some(email),
            photo = Some(protocolPrefix + userPic),
            url = None,
            crm_link = Some("https://moderation.vertis.yandex-team.ru/autoru?user_id=" + userIdLong),
            invite = None
          )
        ),
        message = Some(
          UserMessage(
            `type` = "photo",
            text = Some(""),
            file = Some(protocolPrefix + image1),
            thumb = Some(protocolPrefix + image3)
          )
        ),
        recipient = None
      )
    }
    "render request to tech support without device info" in {
      // TODO
    }
    "render request to tech support without device and platform info" in {
      // TODO
    }

    val techSupport = "techSupport"

    "render correct overloadMessage" in {
      val overloadMessage = TechSupportUtils.privateOverloadMessage("user:100500", "text")
      fixProvidedId(overloadMessage) shouldBe service.CreateMessageParameters(
        "e8dc42ddc373ad22b94de7a13fa87c28",
        techSupport,
        MessagePayload(
          MimeType.TEXT_PLAIN,
          "text"
        ),
        attachments = Seq.empty,
        providedId = Some(TechSupportUtils.TechSupportOverloadMessageId),
        isSilent = true,
        roomLocator = Some(
          RoomLocator.Source(
            CreateRoomParameters(
              Some("e8dc42ddc373ad22b94de7a13fa87c28"),
              Participants(Set(User("user:100500"), User(techSupport))),
              Map()
            )
          )
        )
      )
    }

    "check overload message already sent" in {
      val time = DateTime.now().withMillisOfDay(0).withHourOfDay(4)
      TechSupportUtils.alreadySentOverloadMessageInPeriod(Seq.empty, time, 1.day) shouldBe false

      val m = ModelGenerators.message.next
      val message1 = m.copy(
        author = techSupport,
        created = time.minusHours(5),
        providedId = Some(TechSupportUtils.TechSupportOverloadMessageId)
      )
      TechSupportUtils.alreadySentOverloadMessageInPeriod(Seq(message1), time, 1.day) shouldBe true

      val message2 = message1.copy(created = time.minusHours(24))
      TechSupportUtils.alreadySentOverloadMessageInPeriod(Seq(message2), time, 1.day) shouldBe false
    }
  }

  private def fixProvidedId(overloadMessage: CreateMessageParameters) = {
    overloadMessage.copy(providedId = overloadMessage.providedId.map(_.split("_").head))
  }
}

package ru.auto.api.managers.chat

import brave.propagation.TraceContext
import org.mockito.Mockito.verify
import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.RequestModel.SendMessageRequest
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.app2app.App2AppHandleCrypto
import ru.auto.api.managers.chat.OfferRoomSource.Properties
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.ModelGenerators.{offerGen, OfferIDGen, PrivateUserRefGen, StrictCategoryGen}
import ru.auto.api.model.chat.RoomId
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.gen.ChatApiGenerators._
import ru.auto.api.model.{CategorySelector, OfferID, RequestParams}
import ru.auto.api.services.chat.ChatClient
import ru.auto.api.services.chatbot.ChatBotClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.sender.SenderClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util.RequestImpl
import ru.auto.api.{BaseSpec, RequestModel}
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.model.api.ApiModel.CallInfoMessage.CallSource
import ru.yandex.vertis.chat.model.api.ApiModel._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.tracing.Traced
import zipkin.Endpoint

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class ChatManagerNewSpec extends BaseSpec with MockitoSupport with OptionValues {

  private class Fixture(antirobotDegradationHeader: Boolean = false, antirobotDegradationFeature: Boolean = false) {
    val chatClient: ChatClient = mock[ChatClient]
    val chatBotClient: ChatBotClient = mock[ChatBotClient]
    val offerLoader: EnrichedOfferLoader = mock[EnrichedOfferLoader]
    val passportClient: PassportClient = mock[PassportClient]
    val pushnoyClient: PushnoyClient = mock[PushnoyClient]
    val senderClient: SenderClient = mock[SenderClient]
    val uploaderClient: UploaderClient = mock[UploaderClient]
    val optImageTtl = None
    val chatBotInfo = TestData.chatBotInfo
    val appsFeaturesManager = mock[AppsFeaturesManager]
    val featureManager = mock[FeatureManager]
    val app2AppHandleCrypto = mock[App2AppHandleCrypto]

    val chatManager = new ChatManager(
      chatClient,
      chatBotClient,
      offerLoader,
      passportClient,
      pushnoyClient,
      senderClient,
      uploaderClient,
      chatBotInfo,
      "",
      "",
      optImageTtl,
      appsFeaturesManager,
      featureManager,
      app2AppHandleCrypto
    )

    when(featureManager.chatMessageDegradation).thenReturn {
      new Feature[Boolean] {
        override def name: String = "chat_message_degradation"

        override def value: Boolean = antirobotDegradationFeature
      }
    }

    implicit val request: RequestImpl = newRequest(antirobotDegradation = antirobotDegradationHeader)
    when(appsFeaturesManager.getFeaturesEnum(?)).thenReturnF(Set())
  }

  "ChatManager" should {
    "don't mark spam message if feature is false" in new Fixture(
      antirobotDegradationHeader = true,
      antirobotDegradationFeature = false
    ) {
      val user = ChatUserRef.from(request.user.userRef)
      val roomId = readableString.next
      val sendMessageRequest = createSendMessageRequest(optRoomId = Some(roomId), optRoomLocator = None)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(Message.getDefaultInstance)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(ApiModel.Room.getDefaultInstance)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      chatManager.sendMessage(user, sendMessageRequest).futureValue
      verify(chatClient).sendMessage(?, ?, ?, ?, ?, ?, ?, eeq(false))(?, ?)
    }

    "don't mark spam message if feature is true but header is false" in new Fixture(
      antirobotDegradationHeader = false,
      antirobotDegradationFeature = true
    ) {
      val user = ChatUserRef.from(request.user.userRef)
      val roomId = readableString.next
      val sendMessageRequest = createSendMessageRequest(optRoomId = Some(roomId), optRoomLocator = None)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(Message.getDefaultInstance)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(ApiModel.Room.getDefaultInstance)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      chatManager.sendMessage(user, sendMessageRequest).futureValue
      verify(chatClient).sendMessage(?, ?, ?, ?, ?, ?, ?, eeq(false))(?, ?)
    }

    "mark spam message if feature is true and header is true" in new Fixture(
      antirobotDegradationHeader = true,
      antirobotDegradationFeature = true
    ) {
      val user = ChatUserRef.from(request.user.userRef)
      val roomId = readableString.next
      val sendMessageRequest = createSendMessageRequest(optRoomId = Some(roomId), optRoomLocator = None)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(Message.getDefaultInstance)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(ApiModel.Room.getDefaultInstance)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      chatManager.sendMessage(user, sendMessageRequest).futureValue
      verify(chatClient).sendMessage(?, ?, ?, ?, ?, ?, ?, eeq(true))(?, ?)
    }

    "enrich with app2app data" in new Fixture {
      val roomId = readableString.map(RoomId.parse).next

      val user = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val category: CategorySelector.StrictCategory = StrictCategoryGen.next
      val offerId: OfferID = OfferIDGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUserIds(user.toPlain)
        .addUserIds(otherUser.toPlain)
        .addUsers(makeChatUser(user.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .putProperties(Properties.OfferCategory, category.toString)
        .putProperties(Properties.OfferId, offerId.toPlain)
        .build()

      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)

      val slice = window(messageId).next
      val messages = List {
        val builder = Message.newBuilder()
        builder.getPropertiesBuilder.getCallInfoBuilder.setCallSource(CallSource.APP)
        builder.build()
      }
      when(chatClient.getMessages(eeq(roomId.value), eeq(slice))(eeq(request), ?))
        .thenReturn(Future.successful(messages))

      val offer = offerGen(category, offerId.toPlain, Gen.const(otherUser)).next
      when(offerLoader.getOffer(eeq(category), eeq(offerId), ?, ?, ?, ?)(eeq(request)))
        .thenReturn(Future.successful(offer))

      val app2AppHandle: String = readableString.next
      when(app2AppHandleCrypto.encrypt(eeq(otherUser), eeq(category), eeq(offerId))).thenReturn(app2AppHandle)

      val result = chatManager.getMessages(roomId, ChatUserRef.from(user), slice).futureValue
      result.getMessages(0).getProperties.getCallInfo.getApp2AppHandle shouldBe app2AppHandle
      verify(app2AppHandleCrypto).encrypt(eeq(otherUser), eeq(category), eeq(offerId))
    }
  }

  private def newRequest(experiments: Set[String] = Set.empty, antirobotDegradation: Boolean = false): RequestImpl = {
    val trace = new Traced {
      override def named(name: String): Unit = ()

      override def annotate(tag: String): Unit = ()

      override def annotate(tag: String, value: String): Unit = ()

      override def context: Option[TraceContext] = None

      override def requestId: String = BasicGenerators.readableString(32, 32).next

      override def remoteEndpoint(endpoint: Endpoint): Unit = ()

      override def subTraced(name: String): Traced = this

      override def subTraced(name: String, endpoint: Endpoint): Traced = this

      override def finish(): Unit = ()
    }
    val r = new RequestImpl
    r.setRequestParams(
      RequestParams.construct(
        "1.1.1.1",
        deviceUid = Option("testUid"),
        experiments = experiments,
        antirobotDegradation = antirobotDegradation
      )
    )
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.web)
    r.setToken(TokenServiceImpl.web)
    r.setTrace(trace)
    r
  }

  private def makeChatUser(id: String, muted: Boolean = false, blockedRoom: Boolean = false): User.Builder = {
    ApiModel.User.newBuilder().setId(id).setMutedNotifications(muted).setBlockedRoom(blockedRoom)
  }

  private def createSendMessageRequest(optRoomId: Option[String] = Gen.option(readableString).next,
                                       payload: MessagePayload = messagePayload.next,
                                       payloadMimeType: Option[MimeType] = Some(MimeType.TEXT_PLAIN),
                                       messageAttachments: Seq[Attachment] = attachments.next,
                                       providedId: Option[String] = Gen.option(readableString).next,
                                       optRoomLocator: Option[RequestModel.RoomLocator] =
                                         Gen.option(requestRoomLocator).next,
                                       properties: Option[MessageProperties] = None,
                                       isSilent: Boolean = false,
                                       isSpam: Boolean = false): SendMessageRequest = {
    val payload2 = payloadMimeType
      .map(mimeType => payload.toBuilder.setContentType(mimeType).build())
      .getOrElse(payload)
    val sendMessageRequest = SendMessageRequest
      .newBuilder()
      .setPayload(payload2)
      .addAllAttachments(messageAttachments.asJava)
    optRoomId.foreach(sendMessageRequest.setRoomId)
    providedId.foreach(sendMessageRequest.setProvidedId)
    optRoomLocator.foreach(sendMessageRequest.setRoomLocator)
    properties.foreach(sendMessageRequest.setProperties)
    sendMessageRequest.setIsSilent(isSilent)
    sendMessageRequest.build()
  }
}

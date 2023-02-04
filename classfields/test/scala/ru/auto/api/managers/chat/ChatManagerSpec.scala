package ru.auto.api.managers.chat

import akka.http.scaladsl.model.StatusCodes
import io.prometheus.client.Counter
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.OfferOrBuilder
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.RequestModel.{CreateRoomRequest, RoomLocator, SendMessageRequest}
import ru.auto.api.ResponseModel.PushCounterResponse
import ru.auto.api.auth.Application
import ru.auto.api.chat.ChatModel.{ChatEvent, ChatUser, MessageTypingEvent, RoomType, Room => ApiRoom}
import ru.auto.api.exceptions.{ChatSecurityException, CustomerAccessForbidden}
import ru.auto.api.experiments.PhoneNumberWithChat
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.app2app.App2AppHandleCrypto
import ru.auto.api.managers.chat.ChatManager.PushSubscriptionName
import ru.auto.api.managers.chat.OfferRoomSource.Properties
import ru.auto.api.managers.decay.DecayOptions
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.chat.RoomId
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.gen.ChatApiGenerators._
import ru.auto.api.model.pushnoy.PushInfo
import ru.auto.api.model.{CategorySelector, OfferID, RequestParams, UserRef, _}
import ru.auto.api.services.chat.ChatClient
import ru.auto.api.services.chatbot.ChatBotClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.pushnoy.{PushnoyClient, Targets}
import ru.auto.api.services.sender.SenderClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util.FutureMatchers._
import ru.auto.api.util.{Protobuf, Request, RequestImpl}
import ru.auto.api.{ApiOfferModel, BaseSpec, ResponseModel}
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.auto.chatbot.ApiModel.{ChatBotStateEssentials, ChatBotStateEssentialsResponse}
import ru.yandex.passport.model.api.ApiModel.{UserEssentials, UserResult}
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.model.api.ApiModel.{RoomLocator => ChatRoomLocator, _}
import ru.yandex.vertis.chat.model.events.EventsModel
import ru.yandex.vertis.chat.model.events.EventsModel.UnreadRooms
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import ru.auto.api.model.gen.BasicGenerators

class ChatManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with OptionValues {
  private val chatClient: ChatClient = mock[ChatClient]
  private val chatBotClient: ChatBotClient = mock[ChatBotClient]
  private val offerLoader: EnrichedOfferLoader = mock[EnrichedOfferLoader]
  private val passportClient: PassportClient = mock[PassportClient]
  private val pushnoyClient: PushnoyClient = mock[PushnoyClient]
  private val senderClient: SenderClient = mock[SenderClient]
  private val uploaderClient: UploaderClient = mock[UploaderClient]
  private val optImageTtl = None
  private val chatBotInfo = TestData.chatBotInfo
  private val appsFeaturesManager = mock[AppsFeaturesManager]
  private val featureManager = mock[FeatureManager]
  private val app2AppHandleCrypto = mock[App2AppHandleCrypto]

  private val realChatManager = new ChatManager(
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
  private val chatManager = Mockito.spy(realChatManager)

  private val me: UserRef = PrivateUserRefGen.next
  private val participants: Set[UserRef] = List(me, PrivateUserRefGen.next).toSet

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = newRequest()

  setFeatures()

  private def setFeatures(voxCheck: Boolean = true, chatsDontTextMe: Boolean = true) = {
    reset(featureManager)
    when(featureManager.voxCheck).thenReturn {
      new Feature[Boolean] {
        override def name: String = "vox_check"

        override def value: Boolean = voxCheck
      }
    }
    when(featureManager.chatsDontTextMe).thenReturn {
      new Feature[Boolean] {
        override def name: String = "chats_dont_text_me"
        override def value: Boolean = chatsDontTextMe
      }
    }
    when(featureManager.chatMessageDegradation).thenReturn {
      new Feature[Boolean] {
        override def name: String = "chat_message_degradation"
        override def value: Boolean = false
      }
    }
  }

  private def newRequest(experiments: Set[String] = Set.empty): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid"), experiments = experiments))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.web)
    r.setToken(TokenServiceImpl.web)
    r.setTrace(trace)
    r
  }

  implicit private val ctx: ResolvingContextImpl = mock[ResolvingContextImpl]

  before {
    reset(passportClient)
    reset(chatClient)
    reset(chatBotClient)
    reset(appsFeaturesManager)
    when(appsFeaturesManager.getFeaturesEnum(?)).thenReturnF(Set())
  }

  after {
    verifyNoMoreInteractions(passportClient)
    verifyNoMoreInteractions(chatClient)
    verifyNoMoreInteractions(chatBotClient)
  }

  trait request extends MockitoSupport {

    implicit val request: RequestImpl = {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
      r.setUser(PrivateUserRefGen.next)
      r.setApplication(Application.iosApp)
      r.setToken(TokenServiceImpl.iosApp)
      r.setTrace(trace)
      r
    }

    implicit val ctx: ResolvingContextImpl = mock[ResolvingContextImpl]
  }

  private val techSupportChatRoom = {
    val roomId = readableString.map(RoomId.parse).next
    val user = ChatUserRef.from(PrivateUserRefGen.next)
    val otherUser = TechSupportRoomSource.TechSupportUserId

    ApiModel.Room
      .newBuilder()
      .setId(roomId.value)
      .addUsers(makeChatUser(user.toPlain))
      .addUserIds(user.toPlain)
      .addUsers(makeChatUser(otherUser))
      .addUserIds(otherUser)
      .build()
  }

  private val chatBotRoom = {
    val roomId = readableString.map(RoomId.parse).next
    val user = ChatUserRef.from(PrivateUserRefGen.next)
    val otherUserId = "chatbot:vibiralshik"

    ApiModel.Room
      .newBuilder()
      .setId(roomId.value)
      .addUsers(makeChatUser(user.toPlain))
      .addUserIds(user.toPlain)
      .addUsers(makeChatUser(otherUserId))
      .addUserIds(otherUserId)
      .build()
  }

  private val offerChatRoom = {
    val roomId = readableString.map(RoomId.parse).next
    val user = ChatUserRef.from(PrivateUserRefGen.next)
    val otherUser = ChatUserRef.from(PrivateUserRefGen.next)

    ApiModel.Room
      .newBuilder()
      .setId(roomId.value)
      .addUsers(makeChatUser(user.toPlain))
      .addUserIds(user.toPlain)
      .addUsers(makeChatUser(otherUser.toPlain))
      .addUserIds(otherUser.toPlain)
      .build()
  }

  "ChatManager" should {
    val categoryStr = "cars"
    val strictCategory = CategorySelector.parseStrict(categoryStr)
    val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
    val properties = Map(
      OfferRoomSource.Properties.OfferCategory -> categoryStr,
      OfferRoomSource.Properties.OfferId -> offerId
    )
    val room = roomGenByUserRefs(Gen.const(participants), Gen.const(properties)).next
    val event = textMessageSent(Gen.const(room)).next
    val roomId = event.getMessage.getRoomId
    val messageText = event.getMessage.getPayload.getValue
    val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
    val all = participants.map(p => ChatUser.newBuilder().setId(p.toString).build)
    val chatUserMe = ChatUserMe.fromUserRef(me)
    val chatUsers = CleanChatUsers(all, Some(chatUserMe))
    val offerRoom = OfferRoom(roomId, chatUsers, OfferPointer(categoryStr, offerId), Try(offer))

    when(ctx.getOffer(?)).thenReturnF(offer)

    "return rooms with chat bot in" in {
      val user = ChatUserRef.from(request.user.userRef)
      when(appsFeaturesManager.getFeaturesEnum(?)).thenReturnF(Set(ClientFeature.CHAT_BOT))
      when(chatClient.getRooms(?)(?, ?)).thenReturnF(Nil)
      chatManager.getRooms(user).futureValue
      verify(appsFeaturesManager).getFeaturesEnum(eq(request))
      verify(chatClient).getRooms(eq(user.toPlain))(any(), eq(Set(ClientFeature.CHAT_BOT)))
    }

    "return rooms without chat bot in" in {
      val user = ChatUserRef.from(request.user.userRef)
      when(appsFeaturesManager.getFeaturesEnum(?)).thenReturnF(Set.empty[ClientFeature])
      when(chatClient.getRooms(any())(any(), any())).thenReturnF(Nil)
      chatManager.getRooms(user).futureValue
      verify(appsFeaturesManager).getFeaturesEnum(eq(request))
      verify(chatClient).getRooms(eq(user.toPlain))(any(), eq(Set()))
    }

    "recognize buyer" in {
      val messageBuilder = event.getMessage.toBuilder
      messageBuilder.setAuthor(offer.getUserRef)
      val updatedMessage = messageBuilder.build()

      val eventBuilder = event.toBuilder
      val updatedEvent = eventBuilder.setMessage(updatedMessage).build()

      when(chatManager.asOfferRoom(?)(eq(ctx))).thenReturnF(Some(offerRoom))

      whenReady(chatManager.asMessageSentPush(updatedEvent, _ => updatedMessage)) { value =>
        val title = s"${offer.getCarInfo.getMark} ${offer.getCarInfo.getModel}"
        value("") shouldBe MessageSentPush(
          title,
          "Новое сообщение от продавца: «%s»".format(messageText),
          roomId,
          MessageSentPush.Name
        )
      }
    }

    "recognize seller" in {
      whenReady(chatManager.asMessageSentPush(event, _ => event.getMessage)) { value =>
        val title = s"${offer.getCarInfo.getMark} ${offer.getCarInfo.getModel}"
        value("") shouldBe MessageSentPush(
          title,
          "Новое сообщение от покупателя: «%s»".format(messageText),
          roomId,
          MessageSentPush.Name
        )
      }
    }

    "handle tech support message sent event" in {
      val users = Set(me.toPlain, TechSupportRoomSource.TechSupportUserId)
      val all = users.map(p => ChatUser.newBuilder().setId(p.toString).build)
      val chatUserMe = ChatUserMe.fromUserRef(me)
      val chatUsers = CleanChatUsers(all, Some(chatUserMe))
      val room = roomGen(Gen.const(users), Gen.const(Map.empty)).next
      val event = textMessageSent(Gen.const(room)).next
      val roomId = event.getMessage.getRoomId
      val messageText = event.getMessage.getPayload.getValue
      val techSupportRoom = TechSupportRoom(roomId, chatUsers)

      when(chatManager.asOfferRoom(?)(eq(ctx))).thenReturnF(None)
      when(chatManager.asTechSupportRoom(?)(eq(ctx))).thenReturnF(Some(techSupportRoom))

      whenReady(chatManager.asMessageSentPush(event, _ => event.getMessage)) { value =>
        value("") shouldBe MessageSentPush("Авто.ру", messageText, roomId, TechSupportRoomSource.TechSupportPushName)
      }
    }

    "handle chat bot message sent event" in new request {
      val me: UserRef = PrivateUserRefGen.next

      val users = Set(me.toPlain, "chatbot:vibiralshik")
      val all = users.map(p => ChatUser.newBuilder().setId(p).build)
      val chatUserMe = ChatUserMe.fromUserRef(me)
      val chatUsers = CleanChatUsers(all, Some(chatUserMe))
      val room = roomGen(Gen.const(users), Gen.const(Map.empty)).next
      val event = htmlMessageSent(Gen.const(room)).next
      val roomId = event.getMessage.getRoomId
      val messageText = event.getMessage.getPayload.getValue
      val chatBotRoom = ChatBotRoom(roomId, chatUsers)

      when(chatManager.asOfferRoom(?)(eq(ctx))).thenReturnF(None)
      when(chatManager.asChatBotRoom(?)(eq(ctx))).thenReturnF(Some(chatBotRoom))

      whenReady(chatManager.asMessageSentPush(event, _ => event.getMessage)) { value =>
        value("") shouldBe MessageSentPush(
          "Помощник осмотра Авто.ру",
          messageText.replaceAll("\\<.*?>", ""),
          roomId,
          "Помощник осмотра Авто.ру"
        )
      }
    }

    "make fallback message" in {
      when(chatManager.asOfferRoom(?)(eq(ctx))).thenReturnF(None)
      when(chatManager.asTechSupportRoom(?)(eq(ctx))).thenReturnF(None)

      whenReady(chatManager.asMessageSentPush(event, _ => event.getMessage)) { value =>
        value("") shouldBe MessageSentPush("Новое сообщение", messageText, roomId, MessageSentPush.Name)
      }
    }

    "mute room for user" in {
      when(chatClient.setRoomMuted(any(), any(), any())(any(), any())).thenReturn(Future.unit)
      val roomId = readableString.map(RoomId.parse).next
      val registeredMe = ChatUserRef.from(me.asRegistered)
      chatManager.setRoomMuted(roomId, registeredMe, value = true).futureValue
      verify(chatClient).setRoomMuted(eq(roomId.value), eq(registeredMe.toPlain), eq(true))(any(), any())
    }

    "unmute room for user" in {
      when(chatClient.setRoomMuted(any(), any(), any())(any(), any())).thenReturn(Future.unit)
      val roomId = readableString.map(RoomId.parse).next
      val registeredMe = ChatUserRef.from(me.asRegistered)
      chatManager.setRoomMuted(roomId, registeredMe, value = false).futureValue
      verify(chatClient).setRoomMuted(eq(roomId.value), eq(registeredMe.toPlain), eq(false))(any(), any())
    }

    "block room by user" in {
      when(chatClient.setRoomBlocked(any(), any(), any())(any(), any())).thenReturn(Future.unit)
      val roomId = readableString.map(RoomId.parse).next
      val registeredMe = ChatUserRef.from(me.asRegistered)
      chatManager.setRoomBlocked(roomId, registeredMe, value = true).futureValue
      verify(chatClient).setRoomBlocked(eq(roomId.value), eq(registeredMe.toPlain), eq(true))(any(), any())
    }

    "unblock room by user" in {
      when(chatClient.setRoomBlocked(any(), any(), any())(any(), any())).thenReturn(Future.unit)
      val roomId = readableString.map(RoomId.parse).next
      val registeredMe = ChatUserRef.from(me)
      chatManager.setRoomBlocked(roomId, registeredMe, value = false).futureValue
      verify(chatClient).setRoomBlocked(eq(roomId.value), eq(registeredMe.toPlain), eq(false))(any(), any())
    }

    "do not send push on sent message for muted user" in {
      val mutedUser = PrivateUserRefGen.next
      val unmutedUser = PrivateUserRefGen.next

      val messageSentBuilder = textMessageSent().next.toBuilder
      messageSentBuilder.getRoomBuilder.clearUserIds().clearUsers()
      val userIds = Seq(mutedUser, unmutedUser).map(_.toPlain)
      val users = Seq(
        User.newBuilder().setId(mutedUser.toPlain).setMutedNotifications(true).build(),
        User.newBuilder().setId(unmutedUser.toPlain).setMutedNotifications(false).build()
      )
      messageSentBuilder.getRoomBuilder
        .addAllUserIds(userIds.asJava)
        .addAllUsers(users.asJava)

      val event = EventsModel.Event
        .newBuilder()
        .setMessageSent(messageSentBuilder)
        .build()

      val pushes = ArrayBuffer[(String, Targets.Value)]()
      stub(pushnoyClient.pushToUser(_: String, _: PushInfo, _: Targets.Value)(_: Traced)) {
        case (userId, _, target, _) =>
          pushes += ((userId, target))
          Future.successful(PushCounterResponse.getDefaultInstance)
      }

      chatManager.handleChatEvent(event).futureValue
      (pushes should have).length(3)

      val pushesToMutedUser = pushes.filter(_._1 == mutedUser.toPlain)
      (pushesToMutedUser should have).length(1)

      val pushTargetsForMutedUser = pushesToMutedUser.map(_._2)
      pushTargetsForMutedUser should contain theSameElementsAs Seq(
        Targets.Websocket
      )

      val pushTargetsForUnmutedUser = pushes.filter(_._1 == unmutedUser.toPlain).map(_._2)
      pushTargetsForUnmutedUser should contain theSameElementsAs Seq(
        Targets.Websocket,
        Targets.Devices
      )
    }

    "handle send message event when no user provided" in {
      when(pushnoyClient.pushToUser(?, ?, ?)(?)).thenReturn(Future.successful(PushCounterResponse.getDefaultInstance))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val properties = offerProperties.next
      val strictCategory = CategorySelector.parseStrict(properties(OfferRoomSource.Properties.OfferCategory))
      val offerId = properties(OfferRoomSource.Properties.OfferId)
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      val messageSentBuilder = textMessageSent().next.toBuilder
      messageSentBuilder.getRoomBuilder.clearProperties().putAllProperties(properties.asJava)
      val event = EventsModel.Event
        .newBuilder()
        .setMessageSent(messageSentBuilder)
        .build()
      val r: Request = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1"))
        r.setUser(UserRef.empty)
        r.setApplication(Application.chatBackend)
        r.setToken(TokenServiceImpl.chatBackend)
        r.setTrace(trace)
        r
      }
      chatManager.handleChatEvent(event)(r).futureValue

      val privateUsersCount = messageSentBuilder.getRoom.getUserIdsList.asScala.toSeq
        .count(id => UserRef.unapply(id).exists(_.isPrivate))

      val ctx: ResolvingContextImpl =
        new ResolvingContextImpl(
          offerLoader,
          passportClient,
          chatBotInfo
        )(r, directExecutor)
      val offerRoom = chatManager.asOfferRoom(event.getMessageSent.getRoom)(ctx).futureValue.value
      assert(offerRoom.users.me.isEmpty)

      verify(offerLoader, times(2)).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, r) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
      verify(passportClient, times(privateUsersCount * 2)).getUserEssentials(?, ?)(?)
    }

    "return blockedRoom and mutedNotifications info for simple room" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue

      result.getRoom.getMutedNotifications shouldBe true
      val blurredOtherUser = result.getRoom.getUsers(1)
      blurredOtherUser.getBlockedRoom shouldBe true

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "return lastSeen for room users" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()

      val userEssentials = UserEssentials.newBuilder()
      val moment = DateTime.now()
      userEssentials.getLastSeenBuilder.setSeconds(moment.getMillis / 1000)

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials.build())

      val result = chatManager.getRoom(roomId).futureValue
      result.getRoom.getUsersList.asScala.toSeq.foreach(user => {
        user.getLastSeen.getSeconds shouldBe moment.getMillis / 1000
      })

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "not set lastSeen if none returned from passport" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue
      result.getRoom.getUsersList.asScala.toSeq.foreach(user => {
        user.hasLastSeen shouldBe false
      })

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "return blockedRoom and mutedNotifications info for offer room" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      val otherUser = UserRef.parse(offer.getUserRef).asPrivate
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .putProperties(OfferRoomSource.Properties.OfferCategory, strictCategory.code)
        .putProperties(OfferRoomSource.Properties.OfferId, offerId)
        .build()
      val banned = BasicGenerators.bool.next

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(banned)
      when(offerLoader.getOffer(?, eq(OfferID.parse(offer.getId)), ?, ?, ?, ?)(?))
        .thenReturn(Future.successful(offer))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue

      result.getRoom.getMutedNotifications shouldBe true
      val blurredOtherUser = result.getRoom.getUsers(1)
      blurredOtherUser.getBlockedRoom shouldBe true
      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(offerLoader).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "return rooms without service users" in {
      val roomId = readableString.map(RoomId.parse).next
      val user = PrivateUserRefGen.next
      val chatUser = ChatUserRef.from(user)
      val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        r
      }
      val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      val otherUser = UserRef.parse(offer.getUserRef).asPrivate
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .setCreator(user.toPlain)
        .addUserIds(user.toPlain)
        .addUserIds(otherUser.toPlain)
        .addUserIds("user:28199386")
        .addUsers(makeChatUser(user.toPlain))
        .addUsers(makeChatUser(otherUser.toPlain))
        .addUsers(makeChatUser("user:28199386"))
        .putProperties(OfferRoomSource.Properties.OfferCategory, strictCategory.code)
        .putProperties(OfferRoomSource.Properties.OfferId, offerId)
        .build()

      when(chatClient.getRooms(?)(?, ?)).thenReturnF(List(room))
      when(offerLoader.getOffer(?, eq(OfferID.parse(offerId)), ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val actual = chatManager
        .getRooms(chatUser)(request)
        .futureValue
        .getRoomsList
        .asScala
        .toSeq
        .flatMap(_.getUsersList.asScala.toSeq.map(_.getId))

      val expected = List(blurUserId(user.toPlain, roomId.value), blurUserId(otherUser.toPlain, roomId.value))

      actual should contain theSameElementsAs expected

      verify(chatClient).getRooms(eq(chatUser.toPlain))(?, ?)
      verify(passportClient).getUserEssentials(eq(user), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(offerLoader).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
      verifyNoMoreInteractions(offerLoader)
    }

    "return rooms enriched from passport and vos" in {
      val roomId = readableString.map(RoomId.parse).next
      val user = PrivateUserRefGen.next
      val chatUser = ChatUserRef.from(user)
      val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        r
      }
      val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      val otherUser = UserRef.parse(offer.getUserRef).asPrivate
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .setCreator(user.toPlain)
        .addUsers(makeChatUser(user.toPlain))
        .addUsers(makeChatUser(otherUser.toPlain))
        .putProperties(OfferRoomSource.Properties.OfferCategory, strictCategory.code)
        .putProperties(OfferRoomSource.Properties.OfferId, offerId)
        .build()

      when(chatClient.getRooms(?)(?, ?)).thenReturnF(List(room))
      when(offerLoader.getOffer(?, eq(OfferID.parse(offerId)), ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.getRooms(chatUser)(request).futureValue

      verify(chatClient).getRooms(eq(chatUser.toPlain))(?, ?)
      verify(passportClient).getUserEssentials(eq(user), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(offerLoader).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
      verifyNoMoreInteractions(offerLoader)
    }

    "return rooms enriched from passport and vos by id" in {
      val roomId = readableString.map(RoomId.parse).next
      val user = PrivateUserRefGen.next
      val chatUser = ChatUserRef.from(user)
      val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        r
      }
      val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      val otherUser = UserRef.parse(offer.getUserRef).asPrivate
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .setCreator(user.toPlain)
        .addUsers(makeChatUser(user.toPlain))
        .addUsers(makeChatUser(otherUser.toPlain))
        .putProperties(OfferRoomSource.Properties.OfferCategory, strictCategory.code)
        .putProperties(OfferRoomSource.Properties.OfferId, offerId)
        .build()

      when(chatClient.getRoomsByIds(?)(?, ?, ?)).thenReturnF(List(room))
      when(offerLoader.getOffer(?, eq(OfferID.parse(offerId)), ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.getRoomsByIds(chatUser, Iterable(roomId))(request).futureValue

      verify(chatClient).getRoomsByIds(eq(Iterable(roomId.value)))(?, ?, ?)
      verify(passportClient).getUserEssentials(eq(user), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(offerLoader).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
      verifyNoMoreInteractions(offerLoader)
    }

    "return light rooms not enriched from passport and vos" in {
      val roomId = readableString.map(RoomId.parse).next
      val user = PrivateUserRefGen.next
      val chatUser = ChatUserRef.from(user)
      val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        r
      }
      val offerId = ModelGenerators.HashedOfferIDGen.map(_.toPlain).next
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      val otherUser = UserRef.parse(offer.getUserRef).asPrivate
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .setCreator(user.toPlain)
        .addUsers(makeChatUser(user.toPlain))
        .addUsers(makeChatUser(otherUser.toPlain))
        .putProperties(OfferRoomSource.Properties.OfferCategory, strictCategory.code)
        .putProperties(OfferRoomSource.Properties.OfferId, offerId)
        .build()

      when(chatClient.getRooms(?)(?, ?)).thenReturnF(List(room))

      chatManager.getLightRooms(chatUser)(request).futureValue

      verify(chatClient).getRooms(eq(chatUser.toPlain))(?, ?)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(offerLoader)
    }

    "set correct 'me' for dealer in offer room" in {
      val roomId = readableString.map(RoomId.parse).next
      val access = Map(ResourceAlias.CHATS -> AccessLevel.READ_ONLY)
      val dealerRequest = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(PrivateUserRefGen.next)
        r.setDealer(DealerUserRefGen.next)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setSession(dealerSessionResultWithAccessGen(access).next)
        r.setTrace(trace)
        r
      }
      val user0 = dealerRequest.user.ref
      val user1 = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain))
        .addUsers(makeChatUser(user1.toPlain))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId)(dealerRequest).futureValue

      verify(passportClient).getUserEssentials(eq(user1.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(dealerRequest.user.userRef.asPrivate), eq(true))(?)
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)

      result.getRoom.getMe shouldBe blurUserId(user0.toPlain, roomId.value)
    }

    "remove anonymous users from room, keep private users and dealers" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val user1 = DealerUserRefGen.next
      val user2 = AnonymousUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain))
        .addUsers(makeChatUser(user1.toPlain))
        .addUsers(makeChatUser(user2.toPlain))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue

      result.getRoom.getUsersCount shouldBe 2
      result.getRoom.getUsers(0).getId shouldBe blurUserId(user0.toPlain, roomId.value)
      result.getRoom.getUsers(1).getId shouldBe blurUserId(user1.toPlain, roomId.value)

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(chatClient).getRoom(eq(roomId.value))(?, ?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "create chat room between private users if chatsEnabled=true" in {
      val user0 = request.user.userRef
      val offerB = offerGen(PrivateUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(true)
      val offer = offerB.build()
      val user1 = UserRef.parse(offer.getUserRef)
      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(user1.toPlain)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      val properties = Map(
        OfferRoomSource.Properties.OfferCategory -> offer.getCategory.name().toLowerCase(),
        OfferRoomSource.Properties.OfferId -> offer.getId
      )
      val roomId = buildRoomId(Set(user0.toPlain, user1.toPlain), properties)

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId)
        .addUsers(makeChatUser(user0.toPlain))
        .addUsers(makeChatUser(user1.toPlain))
        .putProperties(OfferRoomSource.Properties.OfferCategory, offer.getCategory.name())
        .putProperties(OfferRoomSource.Properties.OfferId, offer.getId)
        .build()

      when(chatClient.createRoom(?, ?, ?)(?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest).futureValue

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(user1.asPrivate), eq(true))(?)
      verify(chatClient).createRoom(eq(roomId), eq(Set(user0.toPlain, user1.toPlain)), eq(properties))(?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "don't create chat room between private users if chatsEnabled=false" in {
      val user0 = request.user.userRef
      val offerB = offerGen(PrivateUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(false)
      val offer = offerB.build()

      val user1 = UserRef.parse(offer.getUserRef)

      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(user1.toPlain)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest) should failWith[ChatSecurityException]

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(user1.asPrivate), eq(true))(?)
      verifyNoMoreInteractions(chatClient)
    }

    "create chat room by dealer with user" in {
      val dealer = DealerUserRefGen.next
      val userFromDealer = PrivateUserRefGen.next
      val offerB = offerGen(PrivateUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(true)
      val offer = offerB.build()
      val user = offer.getUserRef
      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()
      val properties = Map(
        OfferRoomSource.Properties.OfferCategory -> offer.getCategory.name().toLowerCase(),
        OfferRoomSource.Properties.OfferId -> offer.getId
      )
      val roomId = buildRoomId(Set(dealer.toPlain, user), properties)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId)
        .addUsers(makeChatUser(dealer.toPlain))
        .addUsers(makeChatUser(user))
        .putProperties(OfferRoomSource.Properties.OfferCategory, offer.getCategory.name())
        .putProperties(OfferRoomSource.Properties.OfferId, offer.getId)
        .build()
      when(chatClient.createRoom(?, ?, ?)(?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      implicit val request: RequestImpl = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(userFromDealer)
        r.setDealer(dealer)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setTrace(trace)
        r
      }

      chatManager.createRoom(createRoomRequest)(request).futureValue

      verify(passportClient).getUserEssentials(eq(UserRef.parse(user).asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(userFromDealer), eq(true))(?)
      verify(chatClient).createRoom(eq(roomId), eq(Set(dealer.toPlain, user)), eq(properties))(?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "create chat room with dealer if chatsEnabled=true" in {
      val user0 = request.user.userRef
      val offerB = offerGen(DealerUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(true)
      val offer = offerB.build()

      val dealer = offer.getUserRef

      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(dealer)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      val properties = Map(
        OfferRoomSource.Properties.OfferCategory -> offer.getCategory.name().toLowerCase(),
        OfferRoomSource.Properties.OfferId -> offer.getId
      )
      val roomId = buildRoomId(Set(user0.toPlain, dealer), properties)

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId)
        .addUsers(makeChatUser(user0.toPlain))
        .addUsers(makeChatUser(dealer))
        .putProperties(OfferRoomSource.Properties.OfferCategory, offer.getCategory.name())
        .putProperties(OfferRoomSource.Properties.OfferId, offer.getId)
        .build()

      when(chatClient.createRoom(?, ?, ?)(?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest).futureValue

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(chatClient).createRoom(eq(roomId), eq(Set(user0.toPlain, dealer)), eq(properties))(?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "fail to create chat room with dealer if chatsEnabled=false" in {
      val user0 = request.user.userRef
      val offerB = offerGen(DealerUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(false)
      val offer = offerB.build()
      val dealer = offer.getUserRef

      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(dealer)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest) should failWith[ChatSecurityException]

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verifyNoMoreInteractions(chatClient)
    }

    "fail to create chat room with dealer if chatsEnabled=false with experiment 6072 and with bad offer" in {

      val request0 = newRequest(Set(PhoneNumberWithChat.desktopExp))
      val user0 = request0.user.userRef
      val offerB = offerGen(DealerUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(false)
      offerB.setCategory(ApiOfferModel.Category.TRUCKS)
      val offer = offerB.build()
      val dealer = offer.getUserRef

      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(dealer)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest)(request0) should failWith[ChatSecurityException]

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verifyNoMoreInteractions(chatClient)
    }

    "create chat room with dealer if chatsEnabled=false with experiment 6072 and with applying offer" in {

      val request0 = newRequest(Set(PhoneNumberWithChat.desktopExp))
      val user0 = request0.user.userRef
      val offerB = offerGen(DealerUserRefGen).next.toBuilder
      offerB.getSellerBuilder.setChatsEnabled(false)
      offerB.setCategory(ApiOfferModel.Category.CARS)
      offerB.setSection(ApiOfferModel.Section.NEW)
      val offer = offerB.build()

      val dealer = offer.getUserRef

      val createRoomRequestB = CreateRoomRequest.newBuilder()
      createRoomRequestB.addUsers(user0.toPlain).addUsers(dealer)
      createRoomRequestB.getSubjectBuilder.getOfferBuilder
        .setCategory(offer.getCategory.name())
        .setId(offer.getId)
      val createRoomRequest = createRoomRequestB.build()

      val properties = Map(
        OfferRoomSource.Properties.OfferCategory -> offer.getCategory.name().toLowerCase(),
        OfferRoomSource.Properties.OfferId -> offer.getId
      )
      val roomId = buildRoomId(Set(user0.toPlain, dealer), properties)

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId)
        .addUsers(makeChatUser(user0.toPlain))
        .addUsers(makeChatUser(dealer))
        .putProperties(OfferRoomSource.Properties.OfferCategory, offer.getCategory.name())
        .putProperties(OfferRoomSource.Properties.OfferId, offer.getId)
        .build()

      when(chatClient.createRoom(?, ?, ?)(?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.createRoom(createRoomRequest)(request0).futureValue

      verify(passportClient).getUserEssentials(eq(user0.asPrivate), eq(true))(?)
      verify(chatClient).createRoom(eq(roomId), eq(Set(user0.toPlain, dealer)), eq(properties))(?, ?)
      verify(chatClient).getBan(?, ?)(?, ?)
    }

    "throw exception on empty roomId and room locator in sendMessage" in {
      val user = ChatUserRef.from(request.user.userRef)
      val sendMessageRequest = createSendMessageRequest(optRoomId = None, optRoomLocator = None)

      chatManager.sendMessage(user, sendMessageRequest) should failWith[IllegalArgumentException]
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(chatClient)
    }

    "throw exception on wrong payload mime type in sendMessage" in {
      val user = ChatUserRef.from(request.user.userRef)
      val sendMessageRequest = createSendMessageRequest(payloadMimeType = Some(MimeType.TEXT_HTML))

      chatManager.sendMessage(user, sendMessageRequest) should failWith[IllegalArgumentException]
    }

    "send message with HTML type payload from vos application" in {
      implicit val request = new RequestImpl
      request.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
      request.setUser(PrivateUserRefGen.next)
      request.setApplication(Application.vos)
      request.setToken(TokenServiceImpl.vos)
      request.setTrace(trace)
      val user = ChatUserRef.from(request.user.userRef)
      val sendMessageRequest = createSendMessageRequest(payloadMimeType = Some(MimeType.TEXT_HTML))

      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = Some(roomId),
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          payloadMimeType = Some(MimeType.TEXT_HTML)
        )

      val roomLocator = r.getRoomLocator
      val providedId = r.getProvidedId
      val chatRoomLocator: ChatRoomLocator = chatRoomLocatorDirectFromRequest(roomLocator)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(techSupportChatRoom)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(Message.getDefaultInstance)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.sendMessage(user, r)(request).futureValue

      verify(chatClient).sendMessage(
        eq(user.toPlain),
        eq(chatRoomLocator),
        eq(r.getPayload),
        eq(r.getAttachmentsList.asScala.toSeq),
        eq(providedId),
        eq(None),
        eq(r.getIsSilent),
        eq(false)
      )(any(), any())

      verify(chatClient).getRoom(?)(?, ?, ?)
      verify(passportClient, times(2)).getUserEssentials(?, ?)(?)
    }

    "send message with roomLocator.direct" in {
      val roomId = readableString.map(RoomId.parse).next

      val user = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)

      val r: SendMessageRequest =
        createSendMessageRequest(optRoomId = None, optRoomLocator = Some(requestRoomLocatorDirect.next))

      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(Message.getDefaultInstance)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.sendMessage(ChatUserRef.from(user), r).futureValue

      verify(chatClient).getRoom(eq(r.getRoomLocator.getRoomId))(?, ?, ?)
      verify(passportClient).getUserEssentials(eq(user.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(chatClient).sendMessage(
        eq(user.toPlain),
        eq(chatRoomLocatorDirectFromRequest(r.getRoomLocator)),
        eq(r.getPayload),
        eq(r.getAttachmentsList.asScala.toSeq),
        eq(r.getProvidedId),
        eq(None),
        eq(r.getIsSilent),
        eq(false)
      )(any(), any())
    }

    "send message with roomLocator.source" in {
      val message = Message.getDefaultInstance
      val user = ChatUserRef.from(request.user.userRef)
      val r: SendMessageRequest =
        createSendMessageRequest(optRoomId = None, optRoomLocator = Some(requestRoomLocatorSource.next))

      val roomLocator = r.getRoomLocator
      val providedId = r.getProvidedId

      val (offer: ApiOfferModel.Offer, chatRoomLocator: ChatRoomLocator) =
        chatRoomLocatorSourceFromRequest(user, roomLocator)

      val strictCategory = CategorySelector.parseStrict(offer.getCategory.name())

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturn(Future.successful(message))

      chatManager.sendMessage(user, r).futureValue

      verify(chatClient).sendMessage(
        eq(user.toPlain),
        eq(chatRoomLocator),
        eq(r.getPayload),
        eq(r.getAttachmentsList.asScala.toSeq),
        eq(providedId),
        eq(None),
        eq(r.getIsSilent),
        eq(false)
      )(any(), any())
      verify(offerLoader).getOffer(
        eq(strictCategory),
        eq(OfferID.parse(offer.getId)),
        argThat[(OfferOrBuilder, Request) => EnrichOptions](f => f(offer, request) == EnrichOptions.ForChat),
        eq(Some(DecayOptions.full.copy(sensitiveData = false))),
        any(),
        any()
      )(any())
    }

    "check if room with given roomLocator.direct exists" in {
      val roomLocator = requestRoomLocatorDirect.next
      val chatRoomLocator: ChatRoomLocator = chatRoomLocatorDirectFromRequest(roomLocator)
      when(chatClient.checkRoomExists(?)(?, ?, ?)).thenReturn(Future.successful(true))

      chatManager.checkRoomExists(roomLocator).futureValue

      verify(chatClient).checkRoomExists(eq(chatRoomLocator))(?, ?, ?)
      verify(chatClient).checkRoomExists(?)(?, ?, ?)
    }

    "check if room with given roomLocator.source exists" in {
      val user = ChatUserRef.from(request.user.userRef)
      val roomLocator = requestRoomLocatorSource.next

      val (offer: ApiOfferModel.Offer, chatRoomLocator: ChatRoomLocator) =
        chatRoomLocatorSourceFromRequest(user, roomLocator)

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      when(chatClient.checkRoomExists(?)(?, ?, ?)).thenReturn(Future.successful(true))

      chatManager.checkRoomExists(roomLocator).futureValue

      verify(chatClient).checkRoomExists(eq(chatRoomLocator))(any(), any(), any())
    }

    "return rooms in sorted order with tech support rooms in the top" in {
      val rooms1: Seq[ApiRoom] = Seq(
        makeApiRoom(RoomType.ROOM_TYPE_TECH_SUPPORT, _.minusHours(1)),
        makeApiRoom(RoomType.ROOM_TYPE_OFFER)
      )
      rooms1.min.getRoomType shouldBe RoomType.ROOM_TYPE_TECH_SUPPORT

      val rooms2: Seq[ApiRoom] = Seq(
        makeApiRoom(RoomType.ROOM_TYPE_OFFER, _.minusHours(1)),
        makeApiRoom(RoomType.ROOM_TYPE_OFFER)
      )
      rooms2.min.getId shouldBe rooms2(1).getId
    }

    "return tech support room" in {
      when(chatClient.getTechSupportRoom(?, ?, ?)).thenReturn(Future.successful(ApiModel.Room.getDefaultInstance))
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.getTechSupportRoom.futureValue

      verify(chatClient).getTechSupportRoom(eq(request), any(), any())
      verify(chatClient).getBan(?, ?)(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
    }

    "start new chatbot checkup: default chat bot state essentials" in {
      val offerLink = "https://auto.ru/cars/used/sale/skoda/octavia/1085376352-0face745/"
      val roomId = readableString.next
      when(chatClient.getChatBotRoom(?, ?, ?))
        .thenReturn(Future.successful(ApiModel.Room.newBuilder().setId(roomId).build()))
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(Message.getDefaultInstance))
      val chatBotStateEssentials = ChatBotStateEssentialsResponse.getDefaultInstance
      when(chatBotClient.getCurrentStateEssentials(?)(?)).thenReturn(Future.successful(chatBotStateEssentials))
      chatManager.startNewChatBotCheckUp(offerLink).futureValue

      verify(chatClient, times(2)).getChatBotRoom(eq(request), any(), any())
      verify(chatClient).sendMessage(
        eq(request.user.userRef.toPlain),
        eq(ChatRoomLocator.newBuilder().setRoomId(roomId).build()),
        eq(
          MessagePayload
            .newBuilder()
            .setContentType(MimeType.TEXT_PLAIN)
            .setValue("Привет! Я собираюсь на осмотр этого автомобиля — " + offerLink)
            .build()
        ),
        eq(Seq()),
        eq(""),
        eq(None),
        eq(false),
        eq(false)
      )(eq(request), ?)
      verify(chatClient).getBan(?, ?)(?, ?)
      verify(chatBotClient).getCurrentStateEssentials(eq(roomId))(?)
      verify(passportClient).getUserEssentials(?, ?)(?)
    }

    "start new chatbot checkup: different offer id from chat bot state essentials" in {
      val offerLink = "https://auto.ru/cars/used/sale/skoda/octavia/1085376352-0face745/"
      val roomId = readableString.next
      when(chatClient.getChatBotRoom(?, ?, ?))
        .thenReturn(Future.successful(ApiModel.Room.newBuilder().setId(roomId).build()))
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(Message.getDefaultInstance))
      val chatBotStateEssentials = ChatBotStateEssentials.newBuilder().setOfferId("100500-hash").build()
      val chatBotStateEssentialsResponse = ChatBotStateEssentialsResponse
        .newBuilder()
        .setChatBotStateEssentials(chatBotStateEssentials)
        .build()
      when(chatBotClient.getCurrentStateEssentials(?)(?))
        .thenReturn(Future.successful(chatBotStateEssentialsResponse))
      chatManager.startNewChatBotCheckUp(offerLink).futureValue

      verify(chatClient, times(2)).getChatBotRoom(eq(request), any(), any())
      verify(chatClient).sendMessage(
        eq(request.user.userRef.toPlain),
        eq(ChatRoomLocator.newBuilder().setRoomId(roomId).build()),
        eq(
          MessagePayload
            .newBuilder()
            .setContentType(MimeType.TEXT_PLAIN)
            .setValue("Привет! Я собираюсь на осмотр этого автомобиля — " + offerLink)
            .build()
        ),
        eq(Seq()),
        eq(""),
        eq(None),
        eq(false),
        eq(false)
      )(eq(request), ?)
      verify(chatBotClient).getCurrentStateEssentials(eq(roomId))(?)
      verify(chatClient).getBan(?, ?)(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
    }

    "continue chatbot checkup" in {
      val offerLink = "https://auto.ru/cars/used/sale/skoda/octavia/1085376352-0face745/"
      val roomId = readableString.next
      when(chatClient.getChatBotRoom(?, ?, ?))
        .thenReturn(Future.successful(ApiModel.Room.newBuilder().setId(roomId).build()))
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(false)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      val chatBotStateEssentials = ChatBotStateEssentials.newBuilder().setOfferId("1085376352-0face745").build()
      val chatBotStateEssentialsResponse = ChatBotStateEssentialsResponse
        .newBuilder()
        .setChatBotStateEssentials(chatBotStateEssentials)
        .build()
      when(chatBotClient.getCurrentStateEssentials(?)(?))
        .thenReturn(Future.successful(chatBotStateEssentialsResponse))
      chatManager.startNewChatBotCheckUp(offerLink).futureValue

      verify(chatClient).getChatBotRoom(eq(request), any(), any())
      verify(chatClient).getBan(?, ?)(?, ?)
      verify(chatBotClient).getCurrentStateEssentials(eq(roomId))(?)
      verify(passportClient).getUserEssentials(?, ?)(?)
    }

    "send different emails on unread messages" in {
      val user1 = PrivateUserRefGen.next
      val user2 = PrivateUserRefGen.next
      val category = "cars"
      val offerId = "100500-hash"
      val email = "email@example.com"

      val unreadRooms = UnreadRooms.newBuilder
      unreadRooms.setUser(user1.toPlain)
      unreadRooms
        .addRoomsBuilder()
        .setId("room1")
        .putAllProperties(
          Map(
            Properties.OfferCategory -> category,
            Properties.OfferId -> offerId
          ).asJava
        )
        .addAllUserIds(Seq(user1.toPlain, user2.toPlain).asJava)
      unreadRooms
        .addRoomsBuilder()
        .setId("room2")
        .addAllUserIds(Seq(user1.toPlain, TechSupportRoomSource.TechSupportUserId).asJava)
      val event = EventsModel.Event
        .newBuilder()
        .setUnreadRooms(unreadRooms)
        .build()

      val userResult = UserResult.newBuilder()
      userResult.getUserBuilder.addEmailsBuilder().setEmail(email).setConfirmed(true)
      when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult.build())

      val strictCategory = CategorySelector.parseStrict(category)
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))

      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      when(senderClient.sendLetter(?)(?)).thenReturn(Future.unit)

      chatManager.handleChatEvent(event).futureValue

      verify(passportClient).getUserWithHints(?, ?)(?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(senderClient, times(2)).sendLetter(?)(?)
      verifyNoMoreInteractions(senderClient)
    }

    "do not send email on unread messages from notify center user" in {
      val notifyCenterUser = AutoruUser(28199386)
      val anotherUser = PrivateUserRefGen.next
      val category = "cars"
      val offerId = "100500-hash"
      val email = "email@example.com"
      val unreadRooms = UnreadRooms.newBuilder
      unreadRooms.setUser(notifyCenterUser.toPlain)
      unreadRooms
        .addRoomsBuilder()
        .setId("room1")
        .putAllProperties(
          Map(
            Properties.OfferCategory -> category,
            Properties.OfferId -> offerId
          ).asJava
        )
        .addAllUserIds(Seq(anotherUser.toPlain, notifyCenterUser.toPlain).asJava)
      val event = EventsModel.Event
        .newBuilder()
        .setUnreadRooms(unreadRooms)
        .build()
      val userResult = UserResult.newBuilder()
      userResult.getUserBuilder.addEmailsBuilder().setEmail(email).setConfirmed(true)
      when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult.build())
      val strictCategory = CategorySelector.parseStrict(category)
      val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(offer))
      chatManager.handleChatEvent(event).futureValue
      verify(passportClient).getUserWithHints(?, ?)(?)
      verifyNoMoreInteractions(senderClient)
    }

    "send just one letter if only tech support room is unread" in {
      clearInvocations(senderClient)
      val user1 = PrivateUserRefGen.next
      val email = "email@example.com"

      val unreadRooms = UnreadRooms.newBuilder
      unreadRooms.setUser(user1.toPlain)
      unreadRooms
        .addRoomsBuilder()
        .setId("room1")
        .addAllUserIds(Seq(user1.toPlain, TechSupportRoomSource.TechSupportUserId).asJava)
      val event = EventsModel.Event
        .newBuilder()
        .setUnreadRooms(unreadRooms)
        .build()

      val userResult = UserResult.newBuilder()
      userResult.getUserBuilder.addEmailsBuilder().setEmail(email).setConfirmed(true)
      when(passportClient.getUserWithHints(?, ?)(?)).thenReturnF(userResult.build())

      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      when(senderClient.sendLetter(?)(?)).thenReturn(Future.unit)

      chatManager.handleChatEvent(event).futureValue

      verify(passportClient).getUserWithHints(?, ?)(?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(senderClient).sendLetter(?)(?)
      verifyNoMoreInteractions(senderClient)
    }

    "send tech support poll" in {
      when(chatClient.techSupportPoll(?, ?, ?)(?, ?)).thenReturn(Future.successful(true))
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val hash = readableString.next
      val result = chatManager.techSupportPoll(user, hash, rating = 1).futureValue
      verify(chatClient).techSupportPoll(eq(user.toPlain), eq(hash), eq(1))(any(), any())
      result.getRatingSaved shouldBe true
      result.getStatus shouldBe ResponseModel.ResponseStatus.SUCCESS
    }

    "remove private message properties" in {
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturn(Future.successful(techSupportChatRoom))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      when(chatClient.getMessages(?, ?)(?, ?)).thenReturn(Future.successful(List {
        val builder = Message.newBuilder()
        builder.getPropertiesBuilder
          .setTechSupportOperatorId("some@yandex-team.ru")
          .setUserAppVersion("ios")
        builder.build()
      }))
      val room = readableString.map(RoomId.parse).next
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val slice = window(messageId).next
      val result = chatManager.getMessages(room, user, slice).futureValue
      result.getMessagesCount shouldBe 1
      val m = result.getMessages(0).getProperties
      m.getTechSupportOperatorId shouldBe ""
      m.getUserAppVersion shouldBe "ios"
      verify(chatClient).getMessages(eq(room.value), eq(slice))(?, ?)
      verify(chatClient).getRoom(eq(room.value))(?, ?, ?)
      verify(passportClient, times(2)).getUserEssentials(?, ?)(?)
    }

    "send message properties" in {
      val roomId = readableString.map(RoomId.parse).next

      val user = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)

      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()
      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = None,
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          properties = Some(props)
        )

      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?))
        .thenReturn(Future.successful(Message.getDefaultInstance))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)
      chatManager.sendMessage(ChatUserRef.from(user), r).futureValue

      verify(chatClient).getRoom(eq(r.getRoomLocator.getRoomId))(?, ?, ?)
      verify(passportClient).getUserEssentials(eq(user.asPrivate), eq(true))(?)
      verify(passportClient).getUserEssentials(eq(otherUser), eq(true))(?)
      verify(chatClient).sendMessage(
        eq(user.toPlain),
        eq(chatRoomLocatorDirectFromRequest(r.getRoomLocator)),
        eq(r.getPayload),
        eq(r.getAttachmentsList.asScala.toSeq),
        eq(r.getProvidedId),
        eq(Some(props)),
        eq(r.getIsSilent),
        eq(false)
      )(any(), any())
    }

    "send message with room id" in {
      val roomId = readableString.next
      val message = Message.getDefaultInstance
      val user = ChatUserRef.from(request.user.userRef)
      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()
      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = Some(roomId),
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          properties = Some(props)
        )

      val roomLocator = r.getRoomLocator
      val providedId = r.getProvidedId
      val chatRoomLocator: ChatRoomLocator = chatRoomLocatorDirectFromRequest(roomLocator)
      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(message)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(techSupportChatRoom)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.sendMessage(user, r).futureValue

      verify(chatClient).sendMessage(
        eq(user.toPlain),
        eq(chatRoomLocator),
        eq(r.getPayload),
        eq(r.getAttachmentsList.asScala.toSeq),
        eq(providedId),
        eq(Some(props)),
        eq(r.getIsSilent),
        eq(false)
      )(any(), any())

      verify(chatClient).getRoom(eq(roomId))(?, ?, ?)
      verify(passportClient, times(2)).getUserEssentials(?, ?)(?)
    }

    "dealer send message" in {
      val roomId = readableString.next
      val access = Map(ResourceAlias.CHATS -> AccessLevel.READ_WRITE)
      val message = Message.getDefaultInstance
      val author = DealerUserRefGen.next
      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()

      val dealerRequest = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(PrivateUserRefGen.next)
        r.setDealer(author)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setSession(dealerSessionResultWithAccessGen(access).next)
        r.setTrace(trace)
        r
      }

      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = Some(roomId),
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          properties = Some(props)
        )

      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(message)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(offerChatRoom)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val user = ChatUserRef.from(author)
      chatManager.sendMessage(user, r)(dealerRequest).futureValue

      verify(chatClient).getRoom(eq(roomId))(?, ?, ?)
      verify(chatClient).sendMessage(eq(user.toPlain), ?, ?, ?, ?, ?, ?, ?)(?, ?)
      verify(passportClient, times(3)).getUserEssentials(?, ?)(?)
    }

    "dealer send message to tech support" in {
      val roomId = readableString.next
      val access = Map(ResourceAlias.CHATS -> AccessLevel.READ_ONLY)
      val message = Message.getDefaultInstance
      val userRef = PrivateUserRefGen.next
      val dealerRequest = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(PrivateUserRefGen.next)
        r.setDealer(DealerUserRefGen.next)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setSession(dealerSessionResultWithAccessGen(access).next)
        r.setTrace(trace)
        r
      }

      val user = ChatUserRef.from(userRef)
      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()

      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = Some(roomId),
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          properties = Some(props)
        )

      when(chatClient.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?, ?)).thenReturnF(message)
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(techSupportChatRoom)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.sendMessage(user, r)(dealerRequest).futureValue

      verify(chatClient).getRoom(eq(roomId))(?, ?, ?)
      verify(chatClient).sendMessage(eq(user.toPlain), ?, ?, ?, ?, ?, ?, ?)(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
    }

    "fail on dealer send message without access" in {
      val roomId = readableString.next
      val access = Map(ResourceAlias.CHATS -> AccessLevel.READ_ONLY)
      val userRef = PrivateUserRefGen.next
      val user = ChatUserRef.from(userRef)
      val props = MessageProperties.newBuilder().setUserAppVersion("ios").build()
      val dealerRequest = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
        r.setUser(PrivateUserRefGen.next)
        r.setDealer(DealerUserRefGen.next)
        r.setApplication(Application.iosApp)
        r.setToken(TokenServiceImpl.iosApp)
        r.setSession(dealerSessionResultWithAccessGen(access).next)
        r.setTrace(trace)
        r
      }

      val r: SendMessageRequest =
        createSendMessageRequest(
          optRoomId = Some(roomId),
          optRoomLocator = Some(requestRoomLocatorDirect.next),
          properties = Some(props)
        )

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(offerChatRoom)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      chatManager.sendMessage(user, r)(dealerRequest).failed.futureValue shouldBe a[CustomerAccessForbidden]

      verify(chatClient).getRoom(eq(roomId))(?, ?, ?)
      verify(passportClient, times(3)).getUserEssentials(?, ?)(?)
    }

    "handle tech support typing event" in {
      val room = readableString.next
      val user = PrivateUserRefGen.next.toPlain
      val eventBuilder = EventsModel.Event.newBuilder()
      eventBuilder.getTechSupportTypingBuilder.setRoomId(room).setUserId(user)
      val event = eventBuilder.build()
      val pushEvent =
        Events.asOuterEvent(
          ChatEvent
            .newBuilder()
            .setMessageTyping(
              MessageTypingEvent
                .newBuilder()
                .setUserId(blurUserId(TechSupportRoomSource.TechSupportUserId, room))
                .setRoomId(room)
            )
            .build()
        )
      val push = PushInfo(
        "message_typing",
        PushSubscriptionName,
        Json.parse(Protobuf.toJson(pushEvent)),
        repack = None,
        ttl = Some(1.minute)
      )
      when(pushnoyClient.pushToUser(?, ?, ?)(?)).thenReturn(Future.successful(PushCounterResponse.getDefaultInstance))
      chatManager.handleChatEvent(event).futureValue
      verify(pushnoyClient).pushToUser(eq(user), eq(push), eq(Targets.Websocket))(any())
    }

    "handle user typing to tech support" in {
      implicit val counter = Counter.build("a", "a").labelNames("name", "status").create()
      val roomId = readableString.map(RoomId.parse).next
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val otherUser = TechSupportRoomSource.TechSupportUserId
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user.toPlain))
        .addUserIds(user.toPlain)
        .addUsers(makeChatUser(otherUser))
        .addUserIds(otherUser)
        .build()
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.userTypingToTechSupport(?)(?, ?)).thenReturn(Future.unit)
      chatManager.typing(user, roomId).futureValue
      request.tasks.start(StatusCodes.OK).foreach(_.await)
      verify(chatClient).getRoom(eq(roomId.value))(any(), any(), any())
      verify(chatClient).userTypingToTechSupport(eq(user.toPlain))(any(), any())
    }

    "handle user typing to some other user" in {
      val roomId = readableString.map(RoomId.parse).next
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user.toPlain))
        .addUserIds(user.toPlain)
        .addUsers(makeChatUser(otherUser.toPlain))
        .addUserIds(otherUser.toPlain)
        .build()
      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      chatManager.typing(user, roomId).futureValue
      verify(chatClient).getRoom(eq(roomId.value))(any(), any(), any())
    }

    "return 'banned' for simple room" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val otherUser = PrivateUserRefGen.next
      val room = ApiModel.Room
        .newBuilder()
        .setId(roomId.value)
        .addUsers(makeChatUser(user0.toPlain, muted = true))
        .addUsers(makeChatUser(otherUser.toPlain, blockedRoom = true))
        .build()
      val banned = BasicGenerators.bool.next

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(room)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(banned)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue
      result.getUserIsBanned() shouldBe banned

      verify(passportClient, times(2)).getUserEssentials(?, ?)(?)
      verify(chatClient).getRoom(?)(?, ?, ?)
      verify(chatClient).getBan(eq(user0.toPlain), eq(BanScope.ALL_USER_CHATS))(?, ?)
    }

    "return 'banned' for tech support room" in {
      val roomId = readableString.map(RoomId.parse).next
      val user0 = request.user.userRef
      val banned = BasicGenerators.bool.next

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(techSupportChatRoom)
      when(chatClient.getBan(?, ?)(?, ?)).thenReturnF(banned)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue
      result.getUserIsBanned() shouldBe banned

      verify(passportClient, times(2)).getUserEssentials(?, ?)(?)
      verify(chatClient).getRoom(?)(?, ?, ?)
      verify(chatClient).getBan(eq(user0.toPlain), eq(BanScope.SUPPORT_CHAT))(?, ?)
    }

    "return 'banned' for chat bot room" in {
      val roomId = RoomId.parse(chatBotRoom.getId)

      when(chatClient.getRoom(?)(?, ?, ?)).thenReturnF(chatBotRoom)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentials.getDefaultInstance)

      val result = chatManager.getRoom(roomId).futureValue
      result.getUserIsBanned() shouldBe false

      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(chatClient).getRoom(?)(?, ?, ?)
      verifyNoMoreInteractions(chatClient)
    }
  }

  private def makeApiRoom(roomType: RoomType,
                          updated: DateTime => DateTime = (d) => d,
                          id: String = readableString.next): ApiRoom = {
    val builder = ApiRoom.newBuilder()
    builder
      .setId(id)
      .setRoomType(RoomType.ROOM_TYPE_TECH_SUPPORT)
      .getUpdatedBuilder
      .setSeconds(updated(DateTime.now()).getMillis / 1000)
    builder.build()
  }

  private def chatRoomLocatorSourceFromRequest(user: ChatUserRef,
                                               roomLocator: RoomLocator): (ApiOfferModel.Offer, ChatRoomLocator) = {
    val category = roomLocator.getSource.getSubject.getOffer.getCategory
    val offerId = roomLocator.getSource.getSubject.getOffer.getId
    val strictCategory = CategorySelector.parseStrict(category)
    val offer = offerGen(strictCategory, offerId, PrivateUserRefGen).next
    val chatUsers: Seq[String] =
      (roomLocator.getSource.getUsersList.asScala.toSeq :+ offer.getUserRef :+ user.toPlain).distinct
    val chatProperties = Map(
      OfferRoomSource.Properties.OfferCategory -> roomLocator.getSource.getSubject.getOffer.getCategory,
      OfferRoomSource.Properties.OfferId -> roomLocator.getSource.getSubject.getOffer.getId
    )
    val chatRoomLocator = {
      val b = ChatRoomLocator.newBuilder()
      b.getSourceBuilder
        .setId(buildRoomId(chatUsers.toSet, chatProperties))
        .putAllProperties(chatProperties.asJava)
        .addAllUserIds(chatUsers.sorted.asJava)
      b.build()
    }
    (offer, chatRoomLocator)
  }

  private def chatRoomLocatorDirectFromRequest(roomLocator: RoomLocator): ChatRoomLocator = {
    ChatRoomLocator.newBuilder().setRoomId(roomLocator.getRoomId).build()
  }

  private def makeChatUser(id: String, muted: Boolean = false, blockedRoom: Boolean = false): User.Builder = {
    ApiModel.User.newBuilder().setId(id).setMutedNotifications(muted).setBlockedRoom(blockedRoom)
  }

  private def createSendMessageRequest(optRoomId: Option[String] = Gen.option(readableString).next,
                                       payload: MessagePayload = messagePayload.next,
                                       payloadMimeType: Option[MimeType] = Some(MimeType.TEXT_PLAIN),
                                       messageAttachments: Seq[Attachment] = attachments.next,
                                       providedId: Option[String] = Gen.option(readableString).next,
                                       optRoomLocator: Option[RoomLocator] = Gen.option(requestRoomLocator).next,
                                       properties: Option[MessageProperties] = None,
                                       isSilent: Boolean = false): SendMessageRequest = {
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

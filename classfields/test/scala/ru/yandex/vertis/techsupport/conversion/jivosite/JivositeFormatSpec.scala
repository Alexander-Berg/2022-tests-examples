package ru.yandex.vertis.vsquality.techsupport.conversion.jivosite

import cats.data.Validated
import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.techsupport.conversion.jivosite.JivositeFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.model.Message.Payload
import ru.yandex.vertis.vsquality.techsupport.model.Request.TechsupportAppeal.{
  AddTags,
  CompleteConversation,
  ProcessMessage
}
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.model.api.RequestMeta
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite.ChatUser
import ru.yandex.vertis.vsquality.techsupport.service.ChatService.ChatMessage
import ru.yandex.vertis.vsquality.techsupport.service.{ChatService, ExternalTechsupportService}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

class JivositeFormatSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private def replaceId(request: jivosite.Request, id: Option[String]): jivosite.Request =
    request.copy(message = request.message.map(_.copy(id = id)))

  private val autoruUserId = 123456L.taggedWith[Tags.AutoruPrivatePersonId]
  private val realtyUserId = 654321L.taggedWith[Tags.RealtyUserId]

  private val autoruUser = UserId.Client.Autoru.PrivatePerson(autoruUserId)
  private val realtyUser = UserId.Client.Realty(realtyUserId)

  private val autoruUserInfo = UserInfo(UserId.Client.Autoru.PrivatePerson(autoruUserId), None, None)

  private val operatorId = "alex321".taggedWith[Tags.JivositeOperatorId]
  private val operator = UserId.Operator.Jivosite(operatorId)

  private val operatorUserInfo = UserInfo(
    UserId.Operator.Jivosite(operatorId),
    Some("andrew".taggedWith[Tags.Name]),
    Some("andrew@techsupport".taggedWith[Tags.Email])
  )

  private val text = "some text"
  private val imageUri1 = "https://github.com/file1".taggedWith[Tags.Url]
  private val imageUri2 = "https://github.com/file1".taggedWith[Tags.Url]

  private val tag = "tag".taggedWith[Tags.AppealTag]
  private val completeConversationTag = CompleteConversationNeedFeedbackTag.taggedWith[Tags.AppealTag]

  implicit private val requestMeta: RequestMeta = generate[RequestMeta]()

  private val timestamp = requestMeta.timestamp
  private val messageId = requestMeta.timestamp.toEpochMilli.toString.taggedWith[Tags.MessageId]
  private val imageMessageId0 = messageId + "0"
  private val imageMessageId1 = messageId + "1"

  private val userRequestContext: ClientRequestContext =
    ClientRequestContext(
      requestMeta.timestamp,
      requestMeta.timestamp,
      requestMeta.requestId,
      autoruUserInfo,
      ClientInfo.empty(autoruUser).copy(deviceId = requestMeta.deviceId)
    )

  private val operatorRequestContext: ClientRequestContext =
    ClientRequestContext(
      requestMeta.timestamp,
      requestMeta.timestamp,
      requestMeta.requestId,
      operatorUserInfo,
      ClientInfo.empty(autoruUser)
    )

  private val chatId = "user:123456".taggedWith[Tags.ChatId]
  private val chatDescriptor = ChatDescriptor(ChatProvider.VertisChats, chatId)
  private val emptyChatDescriptor = ChatDescriptor(ChatProvider.VertisChats, "".taggedWith[Tags.ChatId])
  private val chatDescriptorFromJivosite = ChatDescriptor(ChatProvider.VertisChats, jivosite.ChatId)
  private val autoruUserIdInfo = ClientInfo.empty(autoruUser)
  private val realtyUserIdInfo = ClientInfo.empty(realtyUser)

  val autoUserJivoSender: ChatUser = JivositeFormatInstances.clientInfoChatUser(autoruUserIdInfo)

  val autoUserJivoRecepient: jivosite.ChatUser = emptyChatUser(chatId)

  val operatorJivo: ChatUser = JivositeFormatInstances.emptyChatUser(operatorId)

  val textMessage =
    jivosite.UserMessage(
      `type` = "text",
      id = Some(messageId),
      text = Some(text),
      file = None,
      thumb = None,
      suggests = None
    )

  val imageMessage =
    jivosite.UserMessage(
      `type` = "photo",
      id = Some(messageId),
      text = None,
      file = Some(imageUri1),
      thumb = Some(imageUri1),
      suggests = None
    )

  val image2Message =
    jivosite.UserMessage(
      `type` = "photo",
      id = Some(messageId),
      text = None,
      file = Some(imageUri2),
      thumb = Some(imageUri2),
      suggests = None
    )

  val tagsMessage =
    jivosite.UserMessage(
      `type` = "text",
      id = Some(messageId),
      text = Some(s"###$tag ###$completeConversationTag"),
      file = None,
      thumb = None,
      suggests = None
    )

  val tagMessage =
    jivosite.UserMessage(
      `type` = "text",
      id = Some(messageId),
      text = Some(s"###$tag"),
      file = None,
      thumb = None,
      suggests = None
    )

  val completeConversationTagMessage =
    jivosite.UserMessage(
      `type` = "text",
      id = Some(messageId),
      text = Some(s"###$completeConversationTag"),
      file = None,
      thumb = None,
      suggests = None
    )

  val userTextMessageJivo =
    new jivosite.Request(sender = Some(autoUserJivoSender), message = Some(textMessage), recipient = None)

  val userImageMessageJivo =
    new jivosite.Request(sender = Some(autoUserJivoSender), message = Some(imageMessage), recipient = None)

  val userImage2MessageJivo =
    new jivosite.Request(sender = Some(autoUserJivoSender), message = Some(image2Message), recipient = None)

  val operatorTextMessageJivo =
    new jivosite.Request(
      sender = Some(JivositeFormatInstances.userInfoChatUser(operatorUserInfo)),
      message = Some(textMessage),
      recipient = Some(autoUserJivoRecepient)
    )

  val operatorImageMessageJivo =
    new jivosite.Request(
      sender = Some(JivositeFormatInstances.userInfoChatUser(operatorUserInfo)),
      message = Some(imageMessage),
      recipient = Some(autoUserJivoRecepient)
    )

  val operatorImage2MessageJivo =
    new jivosite.Request(
      sender = Some(operatorJivo),
      message = Some(image2Message),
      recipient = Some(autoUserJivoRecepient)
    )

  val operatorTagsMessageJivo =
    new jivosite.Request(
      sender = Some(JivositeFormatInstances.userInfoChatUser(operatorUserInfo)),
      message = Some(tagsMessage),
      recipient = Some(autoUserJivoRecepient)
    )

  val operatorTagMessageJivo =
    new jivosite.Request(
      Some(JivositeFormatInstances.userInfoChatUser(operatorUserInfo)),
      message = Some(tagMessage),
      recipient = Some(autoUserJivoRecepient)
    )

  val operatorCompleteConversationMessageJivo =
    new jivosite.Request(
      Some(JivositeFormatInstances.userInfoChatUser(operatorUserInfo)),
      message = Some(completeConversationTagMessage),
      recipient = Some(autoUserJivoRecepient)
    )

  val imagePayload = Payload(text, Seq(Image(imageUri1)), Seq.empty, None)

  val image2Payload = Payload(text, Seq(Image(imageUri1), Image(imageUri2)), Seq.empty, None)

  val userImageMessage = ProcessMessage(
    emptyChatDescriptor,
    userRequestContext,
    Message(MessageType.Common, messageId, timestamp, autoruUser, Some(imagePayload))
  )

  val userImageMessageEnvelope = ExternalTechsupportService.Envelope(
    TechsupportRespondent.ExternalProvider(TechsupportProvider.Jivosite),
    Message(MessageType.Common, messageId, timestamp, autoruUser, Some(imagePayload)),
    ClientInfo.empty(autoruUser)
  )

  val userImage2Message = ProcessMessage(
    chatDescriptor,
    userRequestContext,
    Message(MessageType.Common, messageId, timestamp, autoruUser, Some(image2Payload))
  )

  val userImage2MessageEnvelope = ExternalTechsupportService.Envelope(
    TechsupportRespondent.ExternalProvider(TechsupportProvider.Jivosite),
    Message(MessageType.Common, messageId, timestamp, autoruUser, Some(image2Payload)),
    ClientInfo.empty(autoruUser)
  )

  val operatorImageMessage = ProcessMessage(
    chatDescriptorFromJivosite,
    operatorRequestContext,
    Message(MessageType.Common, messageId, timestamp, operator, Some(imagePayload.copy(text = "")))
  )

  val operatorImageMessageEnvelope = ChatService.Envelope(
    ChatDescriptor(ChatProvider.VertisChats, chatId),
    ChatService.ChatMessage(Message(MessageType.Common, messageId, timestamp, operator, Some(imagePayload))),
    autoruUser,
    operatorUserInfo
  )

  val operatorImage2Message = ProcessMessage(
    chatDescriptor,
    operatorRequestContext,
    Message(MessageType.Common, messageId, timestamp, operator, Some(image2Payload))
  )

  val operatorTagsMessage = AddTags(chatDescriptorFromJivosite.provider, operatorRequestContext, Appeal.Tags(Set(tag)))

  val operatorTagsMessageEnvelope =
    ChatService.Envelope(
      ChatDescriptor(ChatProvider.VertisChats, chatId),
      ChatService.ChatTags(Appeal.Tags(Set(tag))),
      autoruUser,
      operatorUserInfo
    )

  val operatorCompleteConversationMessage =
    CompleteConversation(chatDescriptorFromJivosite.provider, operatorRequestContext, needFeedback = true)

  val operatorCompleteConversationMessageEnvelope =
    ChatService.Envelope(
      ChatDescriptor(ChatProvider.VertisChats, chatId),
      ChatService.ChatCompleteConversation(
        Appeal.Key(autoruUser, ChatProvider.VertisChats, timestamp),
        timestamp,
        needFeedback = true
      ),
      autoruUser,
      operatorUserInfo
    )

  "JivositeFormat" should {
    "convert chat-backend messages" in {

      jivositeWrites.serialize(userImageMessageEnvelope) shouldBe
        Validated.Valid(Seq(userImageMessageJivo, replaceId(userTextMessageJivo, Some(imageMessageId0))))

      jivositeWrites.serialize(userImage2MessageEnvelope) shouldBe
        Validated.Valid(
          Seq(
            userImageMessageJivo,
            replaceId(userImage2MessageJivo, Some(imageMessageId0)),
            replaceId(userTextMessageJivo, Some(imageMessageId1))
          )
        )

      vertisChatWrites.serialize(operatorImageMessageEnvelope) shouldBe
        Validated.Valid(Seq(operatorImageMessageJivo, replaceId(operatorTextMessageJivo, Some(imageMessageId0))))
    }

    "convert jivosite messages" in {
      jivositeReads.deserialize(operatorImageMessageJivo, Domain.Autoru, ChatProvider.VertisChats) shouldBe
        Validated.Valid(Seq(operatorImageMessage))

      vertisChatWrites.serialize(operatorImageMessageEnvelope) shouldBe
        Validated.Valid(Seq(operatorImageMessageJivo, replaceId(operatorTextMessageJivo, Some(imageMessageId0))))

      jivositeReads.deserialize(operatorTagsMessageJivo, Domain.Autoru, ChatProvider.VertisChats) shouldBe
        Validated.Valid(Seq(operatorTagsMessage, operatorCompleteConversationMessage))

      vertisChatWrites.serialize(operatorTagsMessageEnvelope) shouldBe
        Validated.Valid(Seq(replaceId(operatorTagMessageJivo, None)))

      vertisChatWrites.serialize(operatorCompleteConversationMessageEnvelope) shouldBe
        Validated.Valid(Seq(replaceId(operatorCompleteConversationMessageJivo, None)))

    }

    "convert ChatMessage with suggested bot commands" in {
      val suggests = generate[Seq[BotCommand]](_.nonEmpty)
      val msgPayload = generate[Message.Payload]().copy(availableBotCommands = suggests)
      val message = generate[Message]().copy(payload = Some(msgPayload))
      val chatMessage: ChatService.Envelope = generate[ChatService.Envelope]().copy(
        payload = ChatMessage(message)
      )
      val serialized = vertisChatWrites.serialize(chatMessage)
      val actualRequests = serialized.toOption.get
      actualRequests should not be empty
      val prefixRequestsSuggests = actualRequests
        .take(actualRequests.size - 1)
        .flatMap(_.message)
        .flatMap(_.suggests.toSeq.flatten)
      prefixRequestsSuggests shouldBe empty
      actualRequests.lastOption.foreach { request =>
        val actualSuggests = request.message.flatMap(_.suggests)
        actualSuggests shouldBe Some(suggests)
      }
    }

    "convert ChatMessage with empty suggested bot commands" in {
      val suggests: Seq[BotCommand] = Seq.empty
      val msgPayload = generate[Message.Payload]().copy(availableBotCommands = suggests)
      val message = generate[Message]().copy(payload = Some(msgPayload))
      val chatMessage: ChatService.Envelope = generate[ChatService.Envelope]().copy(
        payload = ChatMessage(message)
      )
      val serialized = vertisChatWrites.serialize(chatMessage)
      val actualRequests = serialized.toOption.get
      actualRequests should not be empty
      actualRequests.foreach { request =>
        val actualSuggests = request.message.flatMap(_.suggests).map(_.toSet)
        actualSuggests shouldBe None
      }
    }

    "crm url for autoru" in {
      val actualAutoruUserCrmUrl: ChatUser = JivositeFormatInstances.clientInfoChatUser(autoruUserIdInfo)
      actualAutoruUserCrmUrl.crm_link.isDefined shouldBe true
      actualAutoruUserCrmUrl.crm_link.get shouldBe "https://moderation.vertis.yandex-team.ru/offers/autoru?userId=123456"
    }

    "crm url for realty" in {
      val actualRealtyUserCrmUrl: ChatUser = JivositeFormatInstances.clientInfoChatUser(realtyUserIdInfo)
      actualRealtyUserCrmUrl.crm_link.isDefined shouldBe true
      actualRealtyUserCrmUrl.crm_link.get shouldBe "https://moderation.vertis.yandex-team.ru/offers/realty?authorUid=654321"
    }

    "correctly create device info prefix for absent os and version" in {
      val clientInfo = generate[ClientInfo]().copy(os = None, appVersion = None)
      JivositeFormatInstances.deviceInfoPrefix(clientInfo) shouldBe ""
    }

    "correctly create device info prefix for present os and absent version" in {
      val platform = generate[Platform]()
      val clientInfo = generate[ClientInfo]().copy(os = Some(platform), appVersion = None)
      JivositeFormatInstances.deviceInfoPrefix(clientInfo) shouldBe s"[$platform] "
    }

    "correctly create device info prefix for present os and version" in {
      val platform = generate[Platform]()
      val version = generate[Version]()
      val clientInfo = generate[ClientInfo]().copy(os = Some(platform), appVersion = Some(version))
      JivositeFormatInstances.deviceInfoPrefix(clientInfo) shouldBe s"[$platform $version] "
    }
  }

}

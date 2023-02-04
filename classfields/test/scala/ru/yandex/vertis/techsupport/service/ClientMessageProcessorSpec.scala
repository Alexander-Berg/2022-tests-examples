package ru.yandex.vertis.vsquality.techsupport.service

import cats.syntax.applicative._
import ru.yandex.vertis.vsquality.techsupport.model.Conversation.BotMeta
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.ClientMessageProcessor.RefuseConversation
import ru.yandex.vertis.vsquality.techsupport.service.bot.TechsupportBot
import ru.yandex.vertis.vsquality.techsupport.service.impl.{ClientMessageProcessorImpl, ConversationReducerImpl}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, UseOrIgnore}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author potseluev
  */
class ClientMessageProcessorSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  implicit private val rc: ClientRequestContext = mock[ClientRequestContext]

  private val bot: TechsupportBot[F] = mock[TechsupportBot[F]]
  private val fallbackProvider: ExternalTechsupportProvider = generate[ExternalTechsupportProvider]()
  private val conversationReducer: ConversationReducer = mock[ConversationReducer]

  private def processor(
      bot: TechsupportBot[F] = bot,
      fallbackProvider: ExternalTechsupportProvider = fallbackProvider,
      conversationReducer: ConversationReducer = conversationReducer): ClientMessageProcessor[F] =
    new ClientMessageProcessorImpl[F](bot, fallbackProvider, conversationReducer)

  private def generateConversation(lastMessageFromClient: Boolean): Conversation =
    generate[Conversation] {
      _.messages.last.author match {
        case _: UserId.Client => lastMessageFromClient
        case _                => !lastMessageFromClient
      }
    }

  case class BotTestCase(
      description: String,
      botVerdict: TechsupportBot.Verdict,
      expectedResult: ClientMessageProcessor.Verdict,
      conversationMessage: Option[Message] = None,
      botPatch: UseOrIgnore[Metadata[BotMeta]] = Ignore,
      requestContext: ClientRequestContext,
      appealContext: AppealContext)

  private val botsTestCases: Seq[BotTestCase] = Seq(
    BotTestCase(
      description = "do nothing if assigned bot make decision DoNothing",
      botVerdict = TechsupportBot.Verdict.DoNothing,
      expectedResult = ClientMessageProcessor.Verdict.Empty,
      requestContext = generate[ClientRequestContext](),
      appealContext = generate[AppealContext]()
    ), {
      val botReplyMessage = generate[Message]()
      BotTestCase(
        description = "reply to client if bot decided Reply(msg)",
        botVerdict = TechsupportBot.Verdict.Reply(botReplyMessage),
        expectedResult = ClientMessageProcessor.Verdict(
          sendToClient = Some(botReplyMessage),
          sendToOperator = None,
          refuseConversation = None
        ),
        requestContext = generate[ClientRequestContext](),
        appealContext = generate[AppealContext]()
      )
    }, {
      val replyToClientMsg = generate[Option[Message]]()
      val sendToOperatorMessage = generate[Message]()
      val newRespondent = TechsupportRespondent.ExternalProvider(fallbackProvider)
      val rc = generate[ClientRequestContext]()
      BotTestCase(
        description = "reassign to fallbackProvider if bot decided Escalate",
        botVerdict = TechsupportBot.Verdict.Escalate(replyToClientMsg),
        expectedResult = ClientMessageProcessor.Verdict(
          sendToClient = replyToClientMsg,
          sendToOperator =
            Some(ExternalTechsupportService.Envelope(newRespondent, sendToOperatorMessage, rc.clientInfo)),
          refuseConversation = Some(
            RefuseConversation.Reassign(
              newRespondent = newRespondent,
              sendToOperatorMessage = sendToOperatorMessage
            )
          )
        ),
        conversationMessage = Some(sendToOperatorMessage),
        requestContext = rc,
        appealContext = generate[AppealContext]()
      )
    }, {
      val botVerdict = generate[TechsupportBot.Verdict.CompleteConversation]()
      BotTestCase(
        description = "complete conversation if bot decided to complete",
        botVerdict = botVerdict,
        expectedResult = ClientMessageProcessor.Verdict(
          sendToClient = botVerdict.messageToClient,
          sendToOperator = None,
          refuseConversation = Some(
            RefuseConversation.Complete(botVerdict.needFeedback)
          )
        ),
        requestContext = generate[ClientRequestContext](),
        appealContext = generate[AppealContext]()
      )
    }
  )

  "ClientMessageProcessor" should {
    "fail when last message isn't from client" in {
      val conversation = generateConversation(lastMessageFromClient = false)
      val rc = generate[ClientRequestContext]()
      val ac = generate[AppealContext]()
      processor()
        .process(conversation)(rc, ac)
        .shouldFailWith[IllegalArgumentException]
    }

    "send client message to external operator if techsupportRespondent is external techsupport" in {
      val externalTechsupportRespondent = generate[TechsupportRespondent.ExternalProvider]()
      val conversation =
        generateConversation(lastMessageFromClient = true).copy(techsupportRespondent = externalTechsupportRespondent)
      val rc = generate[ClientRequestContext]()
      val ac = generate[AppealContext]()
      val expectedResult = ClientMessageProcessor.Verdict(
        sendToClient = None,
        refuseConversation = None,
        sendToOperator = Some(
          ExternalTechsupportService.Envelope(
            externalTechsupportRespondent,
            conversation.messages.last,
            rc.clientInfo
          )
        )
      )
      val actualResult = processor().process(conversation)(rc, ac).await
      actualResult shouldBe expectedResult
    }

    botsTestCases.foreach {
      case BotTestCase(description, botVerdict, expectedResult, conversationMessage, botPatch, rc, ac) =>
        description in {
          val botRespondent = generate[TechsupportRespondent.Bot.type]()
          val bot = mock[TechsupportBot[F]]
          val conversation =
            generateConversation(lastMessageFromClient = true).copy(techsupportRespondent = botRespondent)
          conversationMessage.foreach { msg =>
            stub(conversationReducer.makeMessage(_: Conversation, _: UserId)(_: ClientRequestContext)) { case _ => msg }
          }
          stub(bot.process(_: Conversation)(_: ClientRequestContext, _: AppealContext)) {
            case (`conversation`, `rc`, `ac`) =>
              TechsupportBot.Result(botVerdict, botPatch).pure[F]
          }
          val actualResult = processor(bot).process(conversation)(rc, ac).await
          actualResult shouldBe expectedResult
        }
    }

    "send escalation message to operator" in {
      val replyToClientMsg = generate[Message]()
      val conversation =
        generateConversation(lastMessageFromClient = true)
          .copy(techsupportRespondent = TechsupportRespondent.Bot)
      val rc = generate[ClientRequestContext]()
      val ac = generate[AppealContext]()

      stub(bot.process(_: Conversation)(_: ClientRequestContext, _: AppealContext)) {
        case (`conversation`, `rc`, `ac`) =>
          val escalateBotVerdict = TechsupportBot.Verdict.Escalate(Some(replyToClientMsg))
          TechsupportBot.Result(escalateBotVerdict, Ignore).pure[F]
      }

      val ClientMessageProcessor.Verdict(_, sendToOperator, _, _) =
        processor(bot = bot, conversationReducer = ConversationReducerImpl)
          .process(conversation)(rc, ac)
          .await
      sendToOperator shouldBe a[Some[_]]
      val firstMessageText = sendToOperator.head.message.payload.map(_.text).getOrElse("")
      val replyText = replyToClientMsg.payload.map(_.text).getOrElse("")
      firstMessageText should include(replyText)
    }
  }
}

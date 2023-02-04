package ru.yandex.vertis.vsquality.techsupport.service.request

import cats.Id

import java.time.Instant
import cats.kernel.Monoid
import cats.syntax.applicative._
import cats.syntax.monoid._
import org.mockito.Mockito.verify
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.{AppealPatch, ConversationPatch}
import ru.yandex.vertis.vsquality.techsupport.model.Request.TechsupportAppeal
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.AppealPatchService.PatchResult
import ru.yandex.vertis.vsquality.techsupport.service.ChatService.{
  ChatCompleteConversation,
  ChatMarks,
  ChatMessage,
  ChatPayload,
  ChatTags
}
import ru.yandex.vertis.vsquality.techsupport.service.ClientMessageProcessor.RefuseConversation
import ru.yandex.vertis.vsquality.techsupport.service.request.impl.RequestActionsCalculatorImpl
import ru.yandex.vertis.vsquality.techsupport.service.{
  AppealPatchService,
  AppealPatcher,
  ChatService,
  ClientMessageProcessor
}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.lang_utils.{Use, UseOrIgnore}
import ru.yandex.vertis.vsquality.techsupport.model.UserId.Operator
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author potseluev
  */
class RequestActionsCalculatorSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val patchService: AppealPatchService[Id] = mock[AppealPatchService[Id]]
  private val patcher: AppealPatcher[Id] = mock[AppealPatcher[Id]]
  private val clientMessageProcessor: ClientMessageProcessor[F] = mock[ClientMessageProcessor[F]]

  private def requestGen[U <: UserId](implicit arb: Arbitrary[U]): Gen[Request.TechsupportAppeal.ProcessMessage] =
    for {
      user <- gen[U]
      t <-
        if (isClient(user)) Gen.const(MessageType.Common)
        else Gen.oneOf(MessageType.Common, MessageType.BotCommand, MessageType.Feedback)
      result <- gen[Request.TechsupportAppeal.ProcessMessage].map { request =>
        request.copy(
          context = request.context.copy(userInfo = request.context.userInfo.copy(userId = user)),
          message = request.message.copy(author = user, `type` = t)
        )
      }
    } yield result

  private def isClient(user: UserId) = user match {
    case _: UserId.Client => true
    case _: Operator      => false
  }

  private val clientMsgRequestGen: Gen[Request.TechsupportAppeal.ProcessMessage] =
    requestGen[UserId.Client]

  private val humanOperatorMsgRequestGen: Gen[Request.TechsupportAppeal] =
    requestGen[UserId.Operator.Human]

  private val botOperatorMsgRequestGen: Gen[Request.TechsupportAppeal] =
    requestGen[UserId.Operator.Bot]

  private def activeConversationWithBot(appeal: Option[Appeal]) = appeal match {
    case Some(appeal: Appeal) =>
      appeal.conversations.toList.last.techsupportRespondent match {
        case TechsupportRespondent.Bot if !appeal.state.isTerminal => true
        case _                                                     => false
      }
    case _ => false
  }

  private def appealFilter(request: Request.TechsupportAppeal)(appeal: Appeal): Boolean = {
    val allowFinished = request match {
      case _: TechsupportAppeal.ProcessMessage => false
      case _: TechsupportAppeal.CompleteConversation | _: TechsupportAppeal.AddTags | _: TechsupportAppeal.AddMarks =>
        true
    }
    allowFinished || !appeal.state.isTerminal
  }

  private def actionsCalculator(
      patchService: AppealPatchService[Id] = patchService,
      clientMessageProcessor: ClientMessageProcessor[F] = clientMessageProcessor): RequestActionsCalculator[F] =
    new RequestActionsCalculatorImpl[F](clientMessageProcessor, patchService, patcher, maxMessagesCountPerAppeal = 2000)

  private def assertEmptyActions(appeal: Option[Appeal], request: Request.TechsupportAppeal) = {
    val expectedActions = Actions(Seq.empty, Seq.empty, Seq.empty)
    val actualActions = actionsCalculator().calculateActions(appeal, request).await
    actualActions shouldBe expectedActions
  }

  "RequestActionsCalculator" should {
    "fail with IllegalStateException if patchService returns PatchError.EmptyAppeal" in {
      val request = generate[Request.TechsupportAppeal](!_.isBotMessage)
      val lastAppeal = None
      stub(patchService.patch(_, _)) { case (`lastAppeal`, `request`) =>
        Left(AppealPatchService.PatchError.EmptyAppeal)
      }
      actionsCalculator()
        .calculateActions(lastAppeal, request)
        .shouldFailWith[IllegalStateException]
    }

    "filter out non active old appeals if request is ProcessMessage" in {
      val lastAppeal = generate[Appeal](_.state.isTerminal)
      val request =
        generate[Request.TechsupportAppeal.ProcessMessage](_.message.author != UserId.Operator.VertisTechsupportBot)
      stub(patchService.patch(_, _)) { case (None, `request`) =>
        Right(PatchResult(AppealPatch.empty(lastAppeal.key, request.context.processingTime), lastAppeal))
      }
      actionsCalculator().calculateActions(Some(lastAppeal), request).await
      verify(patchService).patch(None, request)
    }

    "return empty actions if patchService returned empty patch" in {
      val lastAppeal = generate[Option[Appeal]]()
      val request = generate[Request.TechsupportAppeal]()
      stub(patchService.patch(_, _)) { case _ =>
        val emptyPatch = AppealPatch.empty(generate[Appeal.Key](), request.context.processingTime)
        val patchedAppeal = generate[Appeal]()
        Right(PatchResult(emptyPatch, patchedAppeal))
      }
      assertEmptyActions(lastAppeal, request)
    }

    "return empty action for operator request if active conversation with bot" in {

      def requestWithContext[R <: Request.TechsupportAppeal](
          requestContext: ClientRequestContext
        )(implicit a: Arbitrary[R]): Request.TechsupportAppeal =
        (generate[R](): @unchecked) match {
          case pm: Request.TechsupportAppeal.ProcessMessage =>
            pm.copy(context = requestContext, message = pm.message.copy(author = requestContext.userInfo.userId))
          case at: Request.TechsupportAppeal.AddTags =>
            at.copy(context = requestContext)
          case cc: Request.TechsupportAppeal.CompleteConversation =>
            cc.copy(context = requestContext)
        }

      val operator = generate[UserId.Operator.Jivosite]()
      val context = generate[ClientRequestContext]().copy(userInfo = UserInfo(operator, None, None))

      val lastAppeal = Some(generate[Appeal](appeal => activeConversationWithBot(Some(appeal))))
      val patchResult = generate[PatchResult](!_.patch.isEmpty)

      stub(patchService.patch(_, _)) { case _ =>
        Right(patchResult)
      }

      assertEmptyActions(lastAppeal, requestWithContext[TechsupportAppeal.CompleteConversation](context))
      assertEmptyActions(lastAppeal, requestWithContext[TechsupportAppeal.AddTags](context))
      assertEmptyActions(lastAppeal, requestWithContext[TechsupportAppeal.ProcessMessage](context))
    }

    "return empty action for chat_open request if active appeal exists" in {
      val lastAppeal = generate[Appeal](!_.state.isTerminal)
      val request =
        generate[Request.TechsupportAppeal.ProcessMessage](_.message.`type` == MessageType.ChatOpen)
      assertEmptyActions(Some(lastAppeal), request)
    }

    "return correct DB patch and chat service request if request is not ProcessMessage from client" in {
      Checkers.check(Prop.forAll(humanOperatorMsgRequestGen) { request: Request.TechsupportAppeal =>
        val lastAppeal = generate[Option[Appeal]](!activeConversationWithBot(_))
        val patchResult @ PatchResult(patch, patchedAppeal) =
          generate[PatchResult](res => !res.patch.isEmpty)
        stub(patchService.patch(_, _)) { case _ => Right(patchResult) }
        val actualResult = actionsCalculator().calculateActions(lastAppeal, request).await
        val chatPayload = request match {
          case msgRequest: TechsupportAppeal.ProcessMessage => ChatMessage(msgRequest.message)
          case TechsupportAppeal.CompleteConversation(_, _, needFeedback) =>
            ChatCompleteConversation(patchResult.patchedAppeal.key, request.context.processingTime, needFeedback)
          case tagsRequest: TechsupportAppeal.AddTags   => ChatTags(tagsRequest.tags)
          case marksRequest: TechsupportAppeal.AddMarks => ChatMarks(marksRequest.marks)
        }
        val chatRequest =
          ChatService.Envelope(patchedAppeal.chat, chatPayload, patchedAppeal.client, request.context.userInfo)
        val event = Event.AppealUpdate(patchedAppeal, lastAppeal.filter(appealFilter(request)), request.context)
        val expectedResult = Actions(
          dbPatches = Seq(patch),
          chatRequests = Seq(chatRequest),
          events = Seq(event)
        )
        actualResult == expectedResult
      })
    }

    "return correct result if request is ProcessMessage from client" in {
      def toChatRequest(timestamp: Instant, appeal: Appeal)(payload: ChatPayload): ChatService.Envelope =
        ChatService.Envelope(appeal.chat, payload, appeal.client, UserInfo.bot)

      def toActions(
          appeal: Appeal
        )(refuseConversation: RefuseConversation
        )(implicit rc: ClientRequestContext): Actions = {
        refuseConversation match {
          case RefuseConversation.Complete(needFeedback) =>
            Actions(
              dbPatches = Seq.empty,
              chatRequests = Seq(
                toChatRequest(rc.timestamp, appeal)(
                  ChatCompleteConversation(appeal.key, rc.processingTime, needFeedback)
                )
              )
            )
          case RefuseConversation.Reassign(newRespondent, sendToOperatorMessage) =>
            val newConversation = ConversationPatch(
              createTime = rc.processingTime,
              techsupportRespondent = Use(newRespondent),
              message = Use(sendToOperatorMessage)
            ).asAppealPatch(appealKey = appeal.key, rc.processingTime)
            Actions(
              dbPatches = Seq(newConversation)
            )
        }
      }

      Checkers.check(Prop.forAll { verdict: ClientMessageProcessor.Verdict =>
        val ClientMessageProcessor.Verdict(sendToClient, sendToOperator, refuseConversation, newMetadata) = verdict
        val lastAppeal = generate[Option[Appeal]]()
        val request = clientMsgRequestGen.generate()
        implicit val rc: ClientRequestContext = request.context
        val patchResult @ PatchResult(requestPatch, patchedAppeal) = generate[PatchResult](!_.patch.isEmpty)
        val lastConversation = patchedAppeal.conversations.last
        stub(patchService.patch(_, _)) { case _ => Right(patchResult) }
        stub(clientMessageProcessor.process(_: Conversation)(_: ClientRequestContext, _: AppealContext)) { case _ =>
          verdict.pure[F]
        }
        val newState = refuseConversation.collect { case RefuseConversation.Complete(_) =>
          AppealState.Completed(rc.processingTime, feedback = None)
        }
        val additionalPatch = AppealPatch(
          key = patchedAppeal.key,
          updateTime = rc.processingTime,
          state = UseOrIgnore(newState),
          conversation = Use(
            ConversationPatch(
              createTime = lastConversation.createTime,
              message = UseOrIgnore(sendToClient),
              metadata = newMetadata
            )
          )
        )
        val dbPatches = Seq(
          requestPatch,
          additionalPatch
        )
        val ts = Instant.now()

        val reassignmentActions = refuseConversation
          .map(toActions(patchedAppeal))
          .getOrElse(Monoid[Actions].empty)

        val actions = Actions(
          dbPatches = dbPatches,
          chatRequests = sendToClient
            .map(ChatMessage)
            .map(ChatService.Envelope(patchedAppeal.chat, _, patchedAppeal.client, UserInfo.bot))
            .toSeq,
          techRequests = sendToOperator.toSeq
        ) |+| reassignmentActions
        val additionalPatches = actions.dbPatches.tail
        val fullyPatchedAppeal = generate[Appeal]()
        stub(patcher.applyPatches(_, _)) { case (`patchedAppeal`, `additionalPatches`) => Right(fullyPatchedAppeal) }
        val event =
          Event.AppealUpdate(fullyPatchedAppeal, lastAppeal.filter(appealFilter(request)), request.context)
        val eventActions = Actions(
          events = Seq(event)
        )
        val expectedResult = actions |+| eventActions
        val actualResult = actionsCalculator().calculateActions(lastAppeal, request).await
        def prepared(actions: Actions): Actions =
          actions.copy(dbPatches = actions.dbPatches.map(_.copy(updateTime = ts)))
        prepared(actualResult) == prepared(expectedResult)
      })
    }

    "skip message from bot if it would be first in appeal" in {
      val botRequest = botOperatorMsgRequestGen.generate()
      val lastAppeal = generate[Option[Appeal]](!activeConversationWithBot(_))
        .map(_.copy(state = AppealState.Completed(Instant.now(), None)))

      val actualResultIfNoAppeal = actionsCalculator().calculateActions(None, botRequest).await
      val actualResultIfHasTerminalAppeal = actionsCalculator().calculateActions(lastAppeal, botRequest).await

      actualResultIfNoAppeal shouldBe Monoid[Actions].empty
      actualResultIfHasTerminalAppeal shouldBe Monoid[Actions].empty
    }
  }
}

package ru.auto.chatbot.lifecycle

import org.mockito.Mockito.{reset, verifyNoMoreInteractions}
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.client.{PassportClient, PublicApiClient, VinDecoderClient, VosClient}
import ru.auto.chatbot.dao.StateDao
import ru.auto.chatbot.exception.ExceptionHandler
import ru.auto.chatbot.manager.ChatManager.SendMessageResponse
import ru.auto.chatbot.manager.{CatalogManager, ChatManager, ClusteringManager, ReportManager, YavisionManager}
import ru.yandex.vertis.chat.model.api.api_model.{MessagePayload, CreateMessageParameters => ScalaCreateMessageParameters}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
trait MessageProcessorSuit extends FunSuite with MockitoSupport with BeforeAndAfter with Matchers with ScalaFutures {

  import TestContext._

  protected val chatManager: ChatManager = mock[ChatManager]
  protected val vosClient: VosClient = mock[VosClient]
  protected val vinDecoderClient: VinDecoderClient = mock[VinDecoderClient]
  protected val yavisionManager: YavisionManager = mock[YavisionManager]
  protected val clusteringManager: ClusteringManager = mock[ClusteringManager]
  protected val stateDao: StateDao = mock[StateDao]
  protected val publicApiClient: PublicApiClient = mock[PublicApiClient]
  protected val passportClient = mock[PassportClient]
  protected val catalogManager = mock[CatalogManager]
  protected val reportManager = mock[ReportManager]

  protected val exceptionHandler = ExceptionHandler(chatManager)

  protected val fsm: MessageProcessor = MessageProcessor(
    catalogManager,
    chatManager,
    vosClient,
    vinDecoderClient,
    yavisionManager,
    clusteringManager,
    publicApiClient,
    passportClient,
    bunkerMessages,
    bunkerButtons,
    bunkerQuestionsVariations,
    reportManager,
    exceptionHandler
  )

  protected val messageCode = "test_code"
  protected val sentMessage = ScalaCreateMessageParameters(payload = Some(MessagePayload(value = "test value")))
  protected val sendMessageResponse = SendMessageResponse(sentMessage, messageCode)

  before {
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(chatManager.sendState(?)).thenReturn(Future.successful(sendMessageResponse))
    when(chatManager.sendOpenQuestion(?, ?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(chatManager.sendCloseQuestion(?, ?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
  }

  after {
    verifyNoMoreInteractions(chatManager, vosClient, vinDecoderClient, yavisionManager, clusteringManager, stateDao)
    reset(chatManager, vosClient, vinDecoderClient, yavisionManager, clusteringManager, stateDao)
  }

}

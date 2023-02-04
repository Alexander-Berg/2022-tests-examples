package ru.auto.chatbot

import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.client.{PassportClient, PublicApiClient, VinDecoderClient, VosClient}
import ru.auto.chatbot.dao.StateDao
import ru.auto.chatbot.exception.ExceptionHandler
import ru.auto.chatbot.lifecycle.MessageProcessor
import ru.auto.chatbot.manager.ChatManager.SendMessageResponse
import ru.auto.chatbot.manager._
import ru.yandex.vertis.chat.model.api.api_model.{CreateMessageParameters => ScalaCreateMessageParameters}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-19.
  */
class MessageProcessorTest extends FunSuite with MockitoSupport with BeforeAndAfter with Matchers with ScalaFutures {

  import TestContext._

  private val chatManager = mock[ChatManager]
  private val vosClient = mock[VosClient]
  private val vinDecoderClient = mock[VinDecoderClient]
  private val yavisionManager = mock[YavisionManager]
  private val clusteringManager = mock[ClusteringManager]
  private val stateDao = mock[StateDao]
  private val publicApiClient = mock[PublicApiClient]
  private val passportClient = mock[PassportClient]
  private val catalogManager = mock[CatalogManager]
  private val reportManager = mock[ReportManager]

  private val messageParams = ScalaCreateMessageParameters()
  private val sendMessageResponse = SendMessageResponse(messageParams, "")

  lazy val exceptionHandler = ExceptionHandler(chatManager)

  private val fsm = MessageProcessor(
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

  before {
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
  }

  after {
    verifyNoMoreInteractions(chatManager, vosClient, vinDecoderClient, yavisionManager, clusteringManager, stateDao)
    reset(chatManager, vosClient, vinDecoderClient, yavisionManager, clusteringManager, stateDao)
  }

}

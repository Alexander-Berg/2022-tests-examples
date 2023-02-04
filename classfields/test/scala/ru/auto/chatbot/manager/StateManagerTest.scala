package ru.auto.chatbot.manager

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{reset, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import ru.auto.api.vin.ResponseModel.VinResponse
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.basic_model.RecognizedNumber
import ru.auto.chatbot.client.{PassportClient, PublicApiClient, VinDecoderClient, VosClient}
import ru.auto.chatbot.dao.StateDao
import ru.auto.chatbot.exception.ExceptionHandler
import ru.auto.chatbot.lifecycle.Events._
import ru.auto.chatbot.lifecycle.MessageProcessor
import ru.auto.chatbot.manager.ChatManager.SendMessageResponse
import ru.auto.chatbot.model.ButtonCode.{GOING_TO_CHECKUP, SEND_ME_CHECKUP_REPORT}
import ru.auto.chatbot.model.Context
import ru.auto.chatbot.model.MessageCode.{WELL_OKAY_THEN, WELL_OKAY_THEN_CHECKUP_REPORT}
import ru.auto.chatbot.state_model.Step._
import ru.auto.chatbot.state_model.{QuestionAnswer, State}
import ru.auto.chatbot.utils.EventLogWriter
import ru.yandex.vertis.chat.model.api.api_model.{CreateMessageParameters => ScalaCreateMessageParameters}
import ru.yandex.vertis.mockito.MockitoSupport
import scalikejdbc.DBSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-11.
  */
class StateManagerTest extends FunSuite with MockitoSupport with BeforeAndAfter with Matchers with ScalaFutures {

  after {
    reset(chatManager, vosClient, vinDecoderClient, yavisionManager, clusteringManager, stateDao)
  }

  import TestContext._

  private lazy val stateDao = mock[StateDao]
  private lazy val chatManager = mock[ChatManager]
  private lazy val vosClient = mock[VosClient]
  private lazy val vinDecoderClient = mock[VinDecoderClient]
  private lazy val yavisionManager = mock[YavisionManager]
  private lazy val clusteringManager = mock[ClusteringManager]
  private val publicApiClient = mock[PublicApiClient]
  implicit private val dbSession = mock[DBSession]
  private val eventLogger = mock[EventLogWriter]
  private val passportClient = mock[PassportClient]
  private val senderManager = mock[SenderManager]
  private val catalogManager = mock[CatalogManager]
  private val reportManager = mock[ReportManager]

  private lazy val fsm = MessageProcessor(
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

  private lazy val exceptionHandler = ExceptionHandler(chatManager)
  private lazy val stateManager = new StateManager(stateDao, senderManager, fsm, eventLogger)

  private val messageParams = ScalaCreateMessageParameters()
  private val sendMessageResponse = SendMessageResponse(messageParams, "")

  def genQuestionAnswers(size: Int): Map[String, QuestionAnswer] =
    Seq
      .tabulate(size)(identity)
      .map { idx =>
        val n = idx + 1
        val question = s"question$n"
        val answer = s"answer$n"
        question -> QuestionAnswer(question, answer)
      }
      .toMap

  test("process state") {
    val loadedState = State(step = IDLE, roomId = "1", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.loadStateByRoomSkipLocked(?, ?)(?)).thenReturn(Future.successful(Some(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])
    val roomId = Random.alphanumeric.take(20).mkString

    val res = for {
      r <- stateManager.processState(roomId, "", GoingToCheckup("321"), Context.empty)
    } yield r

    Await.result(res, 10.seconds)

    verify(stateDao).saveHistory(?, captor.capture())(?)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe OFFER_AWAIT
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "321"

  }

  test("process async state") {
    val loadedState = State(
      step = LICENSE_PLATE_RECOGNITION_ASYNC,
      roomId = "1",
      isAsync = true,
      licensePlateOffer = "test",
      vinOffer = "test_vin",
      messageId = "123"
    )
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.getAsyncStates(?)).thenReturn(Future.successful(Seq(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(yavisionManager.recognizeLicensePlate(?)).thenReturn(Future.successful(RecognizedNumber(number = "test")))
    when(vinDecoderClient.getVinByLicensePlate(?))
      .thenReturn(Future.successful(VinResponse.newBuilder().setVin("test_vin").build()))

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    val res = for {
      r <- stateManager.processAsyncStates()
    } yield r

    Await.result(res, 10.seconds)

    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT
    savedState.messageId shouldBe "123"
  }

  test("reset and save history when process state IDLE") {
    val loadedState = State(step = IDLE, roomId = "1", offerId = "111", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.loadStateByRoomSkipLocked(?, ?)(?)).thenReturn(Future.successful(Some(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])
    val roomId = Random.alphanumeric.take(20).mkString

    val res = for {
      r <- stateManager.processState(roomId, "", GoingToCheckup("321"), Context.empty)
    } yield r

    Await.result(res, 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe OFFER_AWAIT
    savedState.offerId.isEmpty shouldBe true
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "321"
  }

  test("reset and save history when process state FINISH") {
    val loadedState = State(step = FINISH, roomId = "1", offerId = "111", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.loadStateByRoomSkipLocked(?, ?)(?)).thenReturn(Future.successful(Some(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])
    val roomId = Random.alphanumeric.take(20).mkString

    val res = for {
      r <- stateManager.processState(roomId, "", GoingToCheckup("321"), Context.empty)
    } yield r

    Await.result(res, 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe OFFER_AWAIT
    savedState.offerId.isEmpty shouldBe true
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "321"
  }

  test("process time outed async state") {
    val loadedState = State(step = LICENSE_PLATE_RECOGNITION_ASYNC, roomId = "1", offerId = "111", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.getTimeOutedAsync(?)).thenReturn(Future.successful(Seq(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    val res = for {
      r <- stateManager.processTimedOutAsyncStates()
    } yield r

    Await.result(res, 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT
    savedState.offerId.isEmpty shouldBe false
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "123"
  }

  test("reset and save history when process time outed state") {
    val loadedState = State(step = VIN_TYPE_IN_AWAIT, roomId = "1", offerId = "111", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.getTimeOuted(?)).thenReturn(Future.successful(Seq(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    val res = for {
      r <- stateManager.processTimedOutStates()
    } yield r

    Await.result(res, 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe TIME_OUT_CONTINUE_AWAIT
    savedState.offerId.isEmpty shouldBe false
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "123"
  }

  test("process ready for feedback states") {
    val loadedState = State(step = FINISH, roomId = "1", offerId = "111", messageId = "123")
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.getReadyForFeedback(?)).thenReturn(Future.successful(Seq(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    val res = for {
      r <- stateManager.processReadyForFeedback()
    } yield r

    Await.result(res, 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe HOW_DO_YOU_LIKE_ME_AWAIT
    savedState.offerId.isEmpty shouldBe false
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "123"
  }

  test("process state after AlterMessage") {
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.saveStateWithoutUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    stub(eventLogger.logState _) { case _ => () }

    val prevState =
      State(step = MILEAGE_AWAIT, vinOffer = "vin", licensePlateOffer = "lp", isAsync = true, userId = "1")
    val event = AltStartMessage("msgId", "offerId")
    val newState = Await.result(fsm.transition(event, prevState), 10.seconds)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    Await.result(stateManager.processNewState(prevState, newState, event, Context.empty), 10.seconds)
    verify(stateDao).saveStateWithoutUpdateTime(?, captor.capture())(?)

    val savedState = captor.getValue

    savedState.step shouldBe GET_OFFER_INFORMATION_ASYNC
    savedState.vinOffer shouldBe ""
    savedState.licensePlateOffer shouldBe ""
    savedState.isAsync shouldBe true
    savedState.userId shouldBe "1"
    savedState.offerId shouldBe "offerId"
  }

  test("process state after Cancel when answers size < 5") {
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    stub(eventLogger.logState _) { case _ => () }

    val mapQuestionAnswers = genQuestionAnswers(Random.nextInt(5))
    assert(mapQuestionAnswers.size < 5)

    val prevState = State(
      step = MILEAGE_AWAIT,
      questionAnswers = mapQuestionAnswers
    )

    val event = Cancel("msgId")
    val newState = Await.result(fsm.transition(event, prevState), 10.seconds)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    Await.result(stateManager.processNewState(prevState, newState, event, Context.empty), 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)
    verify(chatManager).sendMessage(?, eq(WELL_OKAY_THEN), eq(Seq(GOING_TO_CHECKUP)))

    val savedState = captor.getValue

    savedState.step shouldBe IDLE
  }

  test("process state after Cancel when answers size >= 5") {
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    when(stateDao.saveHistory(?, ?)(?)).thenReturn(Future.unit)

    stub(eventLogger.logState _) { case _ => () }

    val mapQuestionAnswers = genQuestionAnswers(Random.nextInt(5) + 5)
    assert(mapQuestionAnswers.size >= 5)

    val prevState = State(
      step = MILEAGE_AWAIT,
      questionAnswers = mapQuestionAnswers
    )

    val event = Cancel("msgId")
    val newState = Await.result(fsm.transition(event, prevState), 10.seconds)

    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])

    Await.result(stateManager.processNewState(prevState, newState, event, Context.empty), 10.seconds)
    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)
    verify(chatManager).sendMessage(
      ?,
      eq(WELL_OKAY_THEN_CHECKUP_REPORT),
      eq(Seq(GOING_TO_CHECKUP, SEND_ME_CHECKUP_REPORT))
    )

    val savedState = captor.getValue

    savedState.step shouldBe CHECKUP_REPORT_AGREE_AWAIT
  }

  test("process FINISH state with send letter = true") {
    val loadedState = State(step = EMAIL_APPROVE_AWAIT, roomId = "1", messageId = "123", sendLetter = true)
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.loadStateByRoomSkipLocked(?, ?)(?)).thenReturn(Future.successful(Some(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.saveLetter(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])
    val roomId = Random.alphanumeric.take(20).mkString

    val res = for {
      r <- stateManager.processState(roomId, "", Yes("321"), Context.empty)
    } yield r

    Await.result(res, 10.seconds)

    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)
    verify(stateDao).saveLetter(?, eq(true))(?)

    val savedState = captor.getValue

    savedState.step shouldBe FINISH
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "321"

  }

  test("process FINISH state with send letter = false") {
    val loadedState = State(step = EMAIL_APPROVE_AWAIT, roomId = "1", messageId = "123", sendLetter = false)
    stub(eventLogger.logState _) { case _ => () }
    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))
    when(stateDao.loadStateByRoomSkipLocked(?, ?)(?)).thenReturn(Future.successful(Some(loadedState)))
    when(stateDao.saveStateWithUpdateTime(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.saveLetter(?, ?)(?)).thenReturn(Future.unit)
    when(stateDao.checkRoomExists(?)).thenReturn(Some(true))
    val captor: ArgumentCaptor[State] = ArgumentCaptor.forClass(classOf[State])
    val roomId = Random.alphanumeric.take(20).mkString

    val res = for {
      r <- stateManager.processState(roomId, "", NoThankYou("321"), Context.empty)
    } yield r

    Await.result(res, 10.seconds)

    verify(stateDao).saveStateWithUpdateTime(?, captor.capture())(?)
    verify(stateDao).saveLetter(?, eq(false))(?)

    val savedState = captor.getValue

    savedState.step shouldBe FINISH
    savedState.history.size shouldBe 1
    savedState.messageId shouldBe "321"

  }

}

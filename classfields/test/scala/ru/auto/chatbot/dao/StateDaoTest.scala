package ru.auto.chatbot.dao

import org.scalatest.fixture
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.IDLE
import scalikejdbc.scalatest.AutoRollback
import ru.auto.chatbot.app.TestContext._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-20.
  */
class StateDaoTest extends fixture.FunSuite with AutoRollback {

  dbInit()
  private val dao = new StateDao

  test("save state") { implicit session =>
    val state = State(step = IDLE, roomId = "test_room")

    val res = for {
      _ <- dao.saveStateWithUpdateTime("test", state)
      savedState <- dao.loadStateByRoomSkipLocked("test", isAsync = false)
    } yield savedState

    val optState = Await.result(res, 10.seconds)
    assert(optState.contains(state))
  }

  test("save async state") { implicit session =>
    val state = State(step = IDLE, roomId = "test_async_room").withIsAsync(true)
    val res =
      for {
        _ <- dao.saveStateWithUpdateTime("test_async", state)
        savedStates <- dao.getAsyncStates
      } yield savedStates

    val states = Await.result(res, 10.seconds)
    assert(states.contains(state))
  }

}

package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.verify
import ru.auto.chatbot.exception.OwnCarReviewException
import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.I_AM_HERE
import ru.auto.chatbot.model.MessageCode.{OWN_CAR_REVIEW, RECOMMENDATIONS_BEFORE_REVIEW}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CLUSTERING_AWAIT_ASYNC, OFFER_AWAIT, ON_PLACE_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class ClusteringAwaitAsyncTest extends MessageProcessorSuit {

  test("CLUSTERING_AWAIT_ASYNC own car review") {
    val state = State(step = CLUSTERING_AWAIT_ASYNC, userId = "test")
    when(clusteringManager.checkSameClusterUsers(?, ?)).thenReturn(Future.failed(OwnCarReviewException()))

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(OWN_CAR_REVIEW), eq(Seq()))

    verify(clusteringManager).checkSameClusterUsers(?, ?)
  }

  test("CLUSTERING_AWAIT_ASYNC not own car review") {
    val state = State(step = CLUSTERING_AWAIT_ASYNC, userId = "test")
    when(clusteringManager.checkSameClusterUsers(?, ?)).thenReturn(Future.unit)

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe ON_PLACE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(RECOMMENDATIONS_BEFORE_REVIEW), eq(Seq(I_AM_HERE)))

    verify(clusteringManager).checkSameClusterUsers(?, ?)
  }

  test("CLUSTERING_AWAIT_ASYNC time out") {
    val state = State(step = CLUSTERING_AWAIT_ASYNC, userId = "test")

    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe ON_PLACE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(RECOMMENDATIONS_BEFORE_REVIEW), eq(Seq(I_AM_HERE)))

  }
}


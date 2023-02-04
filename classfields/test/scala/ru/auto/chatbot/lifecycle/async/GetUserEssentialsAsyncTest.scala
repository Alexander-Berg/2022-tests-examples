package ru.auto.chatbot.lifecycle.async

import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CLUSTERING_AWAIT_ASYNC, GET_USER_ESSENTIALS_ASYNC}
import ru.yandex.passport.model.api.ApiModel.UserEssentials

import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-05-21.
  */
class GetUserEssentialsAsyncTest extends MessageProcessorSuit {

  test("GET_USER_ESSENTIALS_ASYNC ping") {
    when(passportClient.getEssentials(?))
      .thenReturn(
        Future.successful(
          UserEssentials
            .newBuilder()
            .setEmail("email")
            .build()
        )
      )
    val state = State(step = GET_USER_ESSENTIALS_ASYNC, userId = "user:1234")

    val res = fsm.transition(Ping(""), state).futureValue

    res.userPassportEmail shouldBe "email"

  }

  test("GET_USER_ESSENTIALS_ASYNC time out") {

    val state =
      State(step = GET_USER_ESSENTIALS_ASYNC, offerMarkCode = "mark", offerModelCode = "model", offerGenerationNum = 10)
    val res = fsm.transition(TimeOut(""), state).futureValue

    res.step shouldBe CLUSTERING_AWAIT_ASYNC
  }

}

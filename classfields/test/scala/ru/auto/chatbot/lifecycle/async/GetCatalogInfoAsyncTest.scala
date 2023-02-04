package ru.auto.chatbot.lifecycle.async

import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.HumanMarkModelGen
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{GET_CATALOG_INFO_ASYNC, GET_USER_ESSENTIALS_ASYNC}

import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-17.
  */
class GetCatalogInfoAsyncTest extends MessageProcessorSuit {

  test("GET_CATALOG_INFO_ASYNC ping") {

    when(catalogManager.getHumanMarkModelGen(?, ?, ?))
      .thenReturn(Future.successful(HumanMarkModelGen("markName", "modelName", "genName")))
    val state =
      State(step = GET_CATALOG_INFO_ASYNC, offerMarkCode = "mark", offerModelCode = "model", offerGenerationNum = 10)

    val res = fsm.transition(Ping(""), state).futureValue

    res.offerMark shouldBe "markName"
    res.offerModel shouldBe "modelName"
    res.offerGeneration shouldBe "genName"
    res.step shouldBe GET_USER_ESSENTIALS_ASYNC
  }

  test("GET_CATALOG_INFO_ASYNC time out") {

    val state =
      State(step = GET_CATALOG_INFO_ASYNC, offerMarkCode = "mark", offerModelCode = "model", offerGenerationNum = 10)
    val res = fsm.transition(TimeOut(""), state).futureValue

    res.step shouldBe GET_USER_ESSENTIALS_ASYNC
  }

}

package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{GoingToCheckup, IamHere}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode._
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECK_MILEAGE_AWAIT, LICENCE_PLATE_PHOTO_AWAIT, ON_PLACE_AWAIT}
import ru.auto.chatbot.model.ButtonCode.{NO, YES}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class OnPlaceAwaitTest extends MessageProcessorSuit {

  test("ON_PLACE_AWAIT IamHere when empty vin & lp") {
    val state = Await.result(fsm.transition(IamHere("10"), State(step = ON_PLACE_AWAIT)), 10.seconds)
    state.step shouldBe CHECK_MILEAGE_AWAIT
    verify(chatManager).sendMessage(?, eq(CHECK_MILEAGE), eq(Seq(YES, NO)))
  }

  test("ON_PLACE_AWAIT IamHere with vin") {
    val state = Await.result(fsm.transition(IamHere("10"), State(step = ON_PLACE_AWAIT, vinOffer = "vin")), 10.seconds)
    state.step shouldBe LICENCE_PLATE_PHOTO_AWAIT
    verify(chatManager).sendMessage(?, eq(GRZ_TAKE_PHOTO), eq(Seq()))
  }

  test("ON_PLACE_AWAIT IamHere with lp") {
    val state =
      Await.result(fsm.transition(IamHere("10"), State(step = ON_PLACE_AWAIT, licensePlateOffer = "lp")), 10.seconds)
    state.step shouldBe LICENCE_PLATE_PHOTO_AWAIT
    verify(chatManager).sendMessage(?, eq(GRZ_TAKE_PHOTO), eq(Seq()))
  }

  test("ON_PLACE_AWAIT begin") {
    val state = Await.result(fsm.transition(GoingToCheckup("10"), State(step = ON_PLACE_AWAIT)), 10.seconds)
    state.step shouldBe ON_PLACE_AWAIT
    state.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), ?)
  }

}

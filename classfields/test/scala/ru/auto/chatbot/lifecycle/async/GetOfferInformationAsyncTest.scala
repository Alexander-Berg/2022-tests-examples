package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Documents, Offer}
import ru.auto.chatbot.exception.OfferNotFoundException
import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.INVALID_LINK
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{GET_CATALOG_INFO_ASYNC, GET_OFFER_INFORMATION_ASYNC, OFFER_AWAIT}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class GetOfferInformationAsyncTest extends MessageProcessorSuit {

  test("GET_OFFER_INFORMATION_ASYNC 404 offer") {
    when(vosClient.getOffer(?)).thenReturn(Future.failed(OfferNotFoundException("1077918705-79ff12da")))

    val state = State(step = GET_OFFER_INFORMATION_ASYNC)
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(INVALID_LINK), ?)

    verify(vosClient).getOffer(?)
  }

  test("GET_OFFER_INFORMATION_ASYNC process offer") {
    val mileage = 10000
    val licensePlate = "a100aa10"
    val documents = Documents.newBuilder().setLicensePlate(licensePlate)
    val offerState = ApiOfferModel.State.newBuilder().setMileage(mileage)
    val state = State(step = GET_OFFER_INFORMATION_ASYNC, userId = "test")
    val tags = Seq("vin_offers_history", "vin_offers_bad_mileage", "vin_resolution_ok")
    val offer = Offer
      .newBuilder()
      .setUserRef("test")
      .setState(offerState)
      .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
      .setDocuments(documents)
      .addAllTags(tags.asJava)
      .build()
    when(vosClient.getOffer(?)).thenReturn(Future.successful(offer))

    val res = fsm.transition(Ping(""), state).futureValue

    verifyZeroInteractions(chatManager)
    verify(vosClient).getOffer(?)

    res.step shouldBe GET_CATALOG_INFO_ASYNC
    res.mileageOffer shouldBe mileage
    res.isAsync shouldBe true
    res.licensePlateOffer shouldBe licensePlate
    res.offerTags shouldBe tags
  }

  test("GET_OFFER_INFORMATION_ASYNC process inactive offer") {
    val mileage = 10000
    val licensePlate = "a100aa10"
    val documents = Documents.newBuilder().setLicensePlate(licensePlate)
    val offerState = ApiOfferModel.State.newBuilder().setMileage(mileage)
    val state = State(step = GET_OFFER_INFORMATION_ASYNC, userId = "test")
    val tags = Seq("vin_offers_history", "vin_offers_bad_mileage", "vin_resolution_ok")
    val offer = Offer
      .newBuilder()
      .setUserRef("test")
      .setState(offerState)
      .setDocuments(documents)
      .setStatus(ApiOfferModel.OfferStatus.INACTIVE)
      .addAllTags(tags.asJava)
      .build()
    when(vosClient.getOffer(?)).thenReturn(Future.successful(offer))

    val res = fsm.transition(Ping(""), state).futureValue

    verify(vosClient).getOffer(?)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(INVALID_LINK), ?)

    verify(vosClient).getOffer(?)
  }

  test("GET_OFFER_INFORMATION_ASYNC time out") {
    when(vosClient.getOffer(?)).thenReturn(Future.failed(OfferNotFoundException("1077918705-79ff12da")))

    val state = State(step = GET_OFFER_INFORMATION_ASYNC)
    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe state.step
    verifyZeroInteractions(chatManager)
  }

}

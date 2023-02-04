package ru.yandex.vertis.parsing.scheduler.workers.vosdiffs.processors

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.DiffLogModel.OfferChangeEvent

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CommonFunctionsTest extends FunSuite {
  test("isOfferFromCallCenter") {
    val event = OfferChangeEvent.newBuilder()
    event.getNewOfferBuilder.getAdditionalInfoBuilder
      .setRemoteUrl("https://auto.e1.ru/moto/used/yamaha/wr_450f/9204636")
    event.getNewOfferBuilder.getSourceInfoBuilder.setIsCallcenter(true)

    assert(!CommonFunctions.isOfferFromCallCenter(event.build()))
  }
}

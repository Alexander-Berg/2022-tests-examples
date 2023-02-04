package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import slick.dbio.{DBIOAction, Effect, NoStream}

trait ProductScheduleStateDbActionsSpec extends AsyncSpecBase with DbSpecBase with SellerModelGenerators {

  implicit class RichDbAction[+R, +S <: NoStream, -E <: Effect](action: DBIOAction[R, S, E]) {
    def execute: R = action.databaseValue.futureValue
  }

  def actions: ProductScheduleStateDbActions

  "ProductScheduleStateDbActions" should {

    "correctly create state and get it" in {
      val state = scheduleStateGen.next

      actions.create(Seq(state)).execute

      val got = actions.get(Set(state.offerId)).execute

      got.size shouldBe 1
      got.head shouldBe state
    }

    "correctly update state properties" in {
      val state = scheduleStateGen.next
      actions.create(Seq(state)).execute

      val newStub = state.copy(
        turnedOn = !state.turnedOn,
        scheduleContext = scheduleContextGen.next
      )
      val updated = actions
        .update(
          newStub
        )
        .execute
      updated shouldBe Some(newStub)

      val got = actions.get(Set(newStub.offerId)).execute

      got.size shouldBe 1
      got.head shouldBe newStub
    }
  }
}

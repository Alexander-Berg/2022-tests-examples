package ru.yandex.vertis.vsquality.techsupport.service

import cats.Id
import cats.kernel.Eq
import cats.syntax.monoid._
import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.{AppealPatch, ConversationPatch}
import ru.yandex.vertis.vsquality.techsupport.model.{Appeal, AppealState, Request}
import ru.yandex.vertis.vsquality.techsupport.service.impl.AppealPatchCalculatorImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.lang_utils.Use

/**
  * @author potseluev
  */
class AppealPatchCalculatorSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val patchCalculator: AppealPatchCalculator[Id] = AppealPatchCalculatorImpl

  implicit private val eqTags: Eq[Appeal.Tags] = Eq.fromUniversalEquals

  "AppealPatchCalculator" should {
    "calculate patch correctly for new tags request" in {
      val request = generate[Request.TechsupportAppeal.AddTags](!_.tags.isEmpty)
      val appeal = generate[Appeal]()
      val actualPatch = patchCalculator.calculatePatch(appeal, request)
      val expectedPatch = AppealPatch(
        key = appeal.key,
        updateTime = actualPatch.updateTime,
        tags = Use(appeal.tags |+| request.tags)
      )
      actualPatch shouldBe expectedPatch
    }

    "return empty patch for Request.AddTags if added tags already exist" in {
      val request = generate[Request.TechsupportAppeal.AddTags]()
      val appeal = generate[Appeal]().copy(
        tags = generate[Appeal.Tags]() |+| request.tags
      )
      val actualPatch = patchCalculator.calculatePatch(appeal, request)
      val expectedPatch = AppealPatch.empty(appeal.key, actualPatch.updateTime)

      actualPatch shouldBe expectedPatch
    }

    "calculate patch correctly for complete conversation request" in {
      val request = generate[Request.TechsupportAppeal.CompleteConversation]()
      val appeal = generate[Appeal](!_.state.isTerminal)
      val actualPatch = patchCalculator.calculatePatch(appeal, request)
      val expectedPatch = AppealPatch(
        key = appeal.key,
        updateTime = actualPatch.updateTime,
        state = Use(AppealState.Completed(request.timestamp, feedback = None))
      )
      actualPatch shouldBe expectedPatch
    }

    "return empty patch for Request.CompleteConversation if appeal is in terminal state" in {
      val request = generate[Request.TechsupportAppeal.CompleteConversation]()
      val appeal = generate[Appeal](_.state.isTerminal)
      val actualPatch = patchCalculator.calculatePatch(appeal, request)
      val expectedPatch = AppealPatch.empty(appeal.key, actualPatch.updateTime)

      actualPatch shouldBe expectedPatch
    }

    "calculate patch correctly for process message request" in {
      val request = generate[Request.TechsupportAppeal.ProcessMessage]()
      val appeal = generate[Appeal]()
      val actualPatch = patchCalculator.calculatePatch(appeal, request)
      val expectedPatch = AppealPatch(
        key = appeal.key,
        updateTime = actualPatch.updateTime,
        conversation = Use(
          ConversationPatch(
            createTime = appeal.conversations.last.createTime,
            message = Use(request.message)
          )
        )
      )
      actualPatch shouldBe expectedPatch
    }

    "return empty patch for Request.ProcessMessage if contains message with equal timestamp" in {
      val appeal = generate[Appeal]()
      val existedMessage = Gen.oneOf(appeal.messages.toList).generate()
      val request = generate[Request.TechsupportAppeal.ProcessMessage]()
      val requestWithSameMessageTimestamp = request.copy(
        message = request.message.copy(messageId = existedMessage.messageId)
      )
      val actualPatch = patchCalculator.calculatePatch(appeal, requestWithSameMessageTimestamp)
      val expectedPatch = AppealPatch.empty(appeal.key, actualPatch.updateTime)

      actualPatch shouldBe expectedPatch
    }
  }
}

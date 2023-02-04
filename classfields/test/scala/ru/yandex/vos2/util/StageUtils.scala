package ru.yandex.vos2.util

import org.scalatest.matchers.{MatchResult, Matcher}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.{Offer, OfferOrBuilder}
import ru.yandex.vos2.watching.stages.ProcessingStage
import ru.yandex.vos2.watching.{AsyncState, ProcessingState}

/**
  * Created by andrey on 7/20/17.
  */
trait StageUtils {
  def asyncProcess(stage: ProcessingStage,
                   state: ProcessingState,
                   checkShouldProcess: Boolean = false): ProcessingState = {
    if (!checkShouldProcess || stage.shouldProcess(state.offer)) {
      val state1 = stage.process(state)
      val asyncUpdate = new AsyncState(state1)
      state1.asyncOperations.foreach(operation => operation.op(asyncUpdate))
      asyncUpdate.toProcessingState(state.oldOffer, state1.offer)
    } else {
      state
    }
  }

  def asyncProcess(stage: ProcessingStage, offer: OfferModel.Offer): ProcessingState = {
    asyncProcess(stage, ProcessingState(offer, offer))
  }

  def asyncProcessWithStatusHistoryUpdate(stage: ProcessingStage,
                                          state: ProcessingState,
                                          getStatus: OfferOrBuilder => CompositeStatus): ProcessingState = {
    val state1 = stage.process(state)
    val asyncUpdate = new AsyncState(state1)
    state1.asyncOperations.foreach(operation => operation.applyOperation(asyncUpdate, getStatus))
    asyncUpdate.toProcessingState(state.oldOffer, state.offer)
  }

  def process(offer: Offer): Matcher[ProcessingStage] = {
    Matcher { stage ⇒
      MatchResult(stage.shouldProcess(offer),
        "stage should process offer",
        "stage should not process offer"
      )
    }
  }
  def notProcess(offer: Offer): Matcher[ProcessingStage] = {
    Matcher { stage ⇒
      MatchResult(!stage.shouldProcess(offer),
        "stage should not process offer",
        "stage should process offer"
      )
    }
  }

}

package ru.auto.tests.publicapi.dealer.auction

import com.google.inject.Inject
import io.qameta.allure.Owner
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor
import ru.auto.tests.publicapi.consts.DealerConsts.{CHERY_BONUS_MODEL, CHERY_MARK}
import ru.auto.tests.publicapi.consts.Owners.MONEY_DUTY
import ru.auto.tests.publicapi.model.RuAutoApiAuctionCallAuctionStateResponse

import scala.jdk.CollectionConverters.CollectionHasAsScala

@Owner(MONEY_DUTY)
class AuctionBaseTest {

  @Inject
  protected val adaptor: PublicApiDealerAdaptor = null

  protected def getCurrentState(sessionId: String,
                                mark: String = CHERY_MARK,
                                model: String = CHERY_BONUS_MODEL): Option[RuAutoApiAuctionCallAuctionStateResponse] = {
    val state = adaptor.getCurrentAuctionState(sessionId)
    state.getStates.asScala.find(state =>
      (state.getContext.getMarkCode == mark
        && state.getContext.getModelCode == model)
    )
  }

  protected def computeNewBid(currentState: Option[RuAutoApiAuctionCallAuctionStateResponse]): Long = {
    val currentBid = currentState.get.getCurrentBid
    if (currentBid > 0)
      currentBid + currentState.get.getOneStep
    else
      currentState.get.getMinBid
  }
}

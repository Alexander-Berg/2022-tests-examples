package ru.yandex.vos2.watching.stages.notifications

/**
  * @author roose
  */
object NotificationUtils {

  def isEventGenerated(state: EventProcessingState, offerId: String = null): Boolean = offerId match {
    case null => state.notifications.nonEmpty
    case _ => state.notifications.exists(_.getOfferId == offerId)
  }
}

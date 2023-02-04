package ru.yandex.complaints.services

import ru.yandex.complaints.model.OfferComplaints
import ru.yandex.complaints.util.SignalRank

import scala.util.Try

/**
  * Created by s-reznick on 21.07.16.
  */
abstract class FailingNotifyModerationService
  extends CountingModerationService {

  class NotifyModerationObjectFailedException extends Exception

  override def notify(info: OfferComplaints,
                      rank: SignalRank): Try[Unit] = Try {
    super.notify(info, rank)
    throw new NotifyModerationObjectFailedException
  }
}
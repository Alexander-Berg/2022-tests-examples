package ru.yandex.complaints.services

import ru.yandex.complaints.model.{ModerationObjectRequest, OfferComplaints}
import ru.yandex.complaints.util.SignalRank

import scala.util.Try

/**
  * Created by s-reznick on 20.07.16.
  */
abstract class CountingModerationService
  extends ModerationService {

  var askCount = 0
  var notifyCount = 0

  def totalCount: Int = askCount + notifyCount

  override def askModerationObject(request: ModerationObjectRequest): Try[ModerationObjectInfo] = Try {
    askCount += 1
    new ModerationObjectInfo("moi12345")
  }

  override def notify(info: OfferComplaints,
                      rank: SignalRank): Try[Unit] = Try {
    notifyCount += 1

    ()
  }
}
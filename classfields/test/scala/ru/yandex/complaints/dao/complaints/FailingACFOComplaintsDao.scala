package ru.yandex.complaints.dao.complaints

import ru.yandex.complaints.dao._
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.{Complaint, User}

import scala.util.Try

/**
  * Created by s-reznick on 21.07.16.
  */
abstract class FailingACFOComplaintsDao extends CountingComplaintsDao {
  class FailedACFOException extends Exception

  override def allComplaintsForOffer(offerId: OfferID)
  : Try[(Seq[Complaint], Map[UserId, User])] = Try {
    super.allComplaintsForOffer(offerId)

    throw new FailedACFOException
  }
}
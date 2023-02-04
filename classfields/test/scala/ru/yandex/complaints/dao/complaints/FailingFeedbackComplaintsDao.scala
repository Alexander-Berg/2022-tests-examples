package ru.yandex.complaints.dao.complaints

import ru.yandex.complaints.dao._
import ru.yandex.complaints.model.ComplaintType

import scala.util.Try

/**
  * Created by s-reznick on 21.07.16.
  */
abstract class FailingFeedbackComplaintsDao extends CountingComplaintsDao {
  class FailedFeedbackException extends Exception

  override def feedback(modobjID: ModObjID,
                        positive: Boolean,
                        exceptedComplaints: Set[ComplaintType]): Try[Unit] = Try {
    super.feedback(modobjID, positive)

    throw new FailedFeedbackException
  }
}
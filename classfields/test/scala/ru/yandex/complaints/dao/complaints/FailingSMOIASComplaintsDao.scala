package ru.yandex.complaints.dao.complaints

import ru.yandex.complaints.dao._
import ru.yandex.complaints.model.Complaint

import scala.util.Try

/**
  * Created by s-reznick on 20.07.16.
  */
abstract class FailingSMOIComplaintsDao extends CountingComplaintsDao {
  class FailedSMOIException extends Exception

  override def setModObjId(id: OfferID, modobjId: ModObjID): Try[Unit] = Try {
    super.setModObjId(id, modobjId)

    throw new FailedSMOIException
  }
}
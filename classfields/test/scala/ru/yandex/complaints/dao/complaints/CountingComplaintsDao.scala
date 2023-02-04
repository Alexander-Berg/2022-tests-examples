package ru.yandex.complaints.dao.complaints

import java.sql.Timestamp
import ru.yandex.complaints.dao._
import ru.yandex.complaints.dao.complaints.ComplaintsDao.ComplaintsAndUsers
import ru.yandex.complaints.model.Complaint.{Application, Group, UserData}
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.UserType
import ru.yandex.complaints.model._

import scala.util.Try

/**
  * Created by s-reznick on 20.07.16.
  */
abstract class CountingComplaintsDao extends ComplaintsDao {
  var createCount = 0
  var removeCount = 0
  var vcfoCount = 0
  var acfoCount = 0
  var acwdCount = 0
  var feedbackCount = 0
  var smoiCount = 0
  var hfcCount = 0
  var pcfoCount = 0
  var prCount = 0
  var snCount = 0

  var feedbackPosCount = 0
  var feedbackNegCount = 0


  def totalCount: Int = {
    createCount + removeCount + vcfoCount + acfoCount +
      acwdCount + feedbackCount + smoiCount + hfcCount +
      pcfoCount + prCount
  }

  override def create(complaintId: ComplaintID,
                      userId: UserId,
                      userType: UserType,
                      offerId: OfferID,
                      descr: String,
                      ctype: Byte,
                      createTime: Timestamp,
                      userData: UserData,
                      source: Option[String]): Try[Unit] = Try {
    createCount += 1

    ComplaintRef(UserId("u12345"), Plain("o12345"), "c12345")
  }


  override def remove(complaintID: ComplaintID,
                      modobjID: Option[ModObjID],
                      offerID: OfferID): Try[Unit] = Try {
    removeCount += 1

    ()
  }

  override def validComplaintsForOffer(offerID: OfferID): Try[ComplaintsAndUsers] =
    Try {
      vcfoCount += 1
      (Seq[Complaint](), Map[UserId, User]())
    }

  override def allComplaintsForOffer(offerID: OfferID): Try[ComplaintsAndUsers] =
    Try {
      acfoCount += 1
      (Seq[Complaint](), Map[UserId, User]())
    }

  override def allComplaintsWithDecisions(offerID: OfferID): Try[(Seq[Complaint],
    Map[UserId, User],
    Map[ModObjID, Option[Decision]])] = Try {
    acwdCount += 1

    (Seq[Complaint](), Map[UserId, User](),
      Map[ModObjID, Option[Decision]]())
  }

  override def feedback(modobjID: ModObjID, positive: Boolean, exceptedComplaints: Set[ComplaintType]): Try[Unit] =
    Try {
      feedbackCount += 1

      if (positive) {
        feedbackPosCount += 1
      } else {
        feedbackNegCount += 1
      }

      ()
    }

  override def setModObjId(id: OfferID, modobjId: ModObjID): Try[Unit] = Try {
    smoiCount += 1
    ()
  }

  override def hasFreshComplaints(offerID: OfferID): Try[Boolean] = Try {
    hfcCount += 1
    false
  }


  override def plainComplaintsForOffer(offerID: OfferID)
  : Try[Seq[Complaint]] = Try {
    pcfoCount += 1
    Seq[Complaint]()
  }

  override def plainRemove(complaintIDs: Seq[ComplaintID])
  : Try[Unit] = Try {
    prCount += 1
  }

  override def setNotified(ids: Set[ComplaintID]): Try[Unit] = Try {
    snCount += 1
  }
}
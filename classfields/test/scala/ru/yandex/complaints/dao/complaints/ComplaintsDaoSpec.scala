package ru.yandex.complaints.dao.complaints

import java.sql.Timestamp
import org.scalatest.{Matchers, WordSpec}
import org.springframework.dao.DataIntegrityViolationException
import ru.yandex.complaints.api.util.ScheduledVisitor
import ru.yandex.complaints.dao.offers.OffersDao
import ru.yandex.complaints.dao.users.UsersDao
import ru.yandex.complaints.dao.{Plain, getNow}
import ru.yandex.complaints.model.Complaint.{Application, Group, UserData}
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.{Complaint, ComplaintType, Offer, UserType}

import scala.util.{Failure, Success}

/**
  * Specs for [[ComplaintsDao]]
  *
  * @author alesavin
  */
trait ComplaintsDaoSpec
  extends WordSpec
  with Matchers {

  def complaintsDao: ComplaintsDao
  def usersDao: UsersDao
  def offersDao: OffersDao

  "ComplaintsDao" should {

    val userId = UserId("123")
    val OfferId = Plain("456")

    "return empty on start" in {
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints should be (empty)
          users should be (empty)
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if create complaint for non-exist user" in {
      val now = new Timestamp(getNow)
      complaintsDao.create("1", userId, UserType.Undefined, OfferId,
        "descr", ComplaintType.UserFraud.code, now, UserData.Empty, Some("WATCH")) match {
        case Failure(_: DataIntegrityViolationException) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints should be (empty)
          users should be (empty)
        case other => fail(s"Unexpected $other")
      }
    }
    "fail if create complaint for exist user and non-exist offer-id" in {
      usersDao.upsert(userId, UserType.Undefined) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      val now = new Timestamp(getNow)
      complaintsDao.create("1", userId, UserType.Undefined, OfferId, "descr", ComplaintType.UserFraud.code,
        now, UserData.Empty, Some("WATCH")) match {
        case Failure(_: DataIntegrityViolationException) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints should be (empty)
          users should be (empty)
        case other => fail(s"Unexpected $other")
      }
    }
    "create complaint for exist user and offer-id" in {
      val now = new Timestamp(getNow / 1000 * 1000)

      offersDao.useRef(OfferId, Some(e => Offer(id = OfferId,
        createTime = now,authorId = "authorId", hash = "")))(_ =>
          new ScheduledVisitor[Offer]) should be(true)

      usersDao.upsert(userId, UserType.Undefined) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.create("1", userId, UserType.Undefined, OfferId, "тест", ComplaintType.UserFraud.code,
        now, UserData.Empty, Some("WATCH")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints should be (empty)
          users should be (empty)
        case other => fail(s"Unexpected $other")
      }

      complaintsDao.setModObjId(OfferId, s"modobj_$OfferId") match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be(1)
          val c = complaints.head
          c.complaintId should be("1")
          c.userId should be(userId)
          c.offerId should be(OfferId)
          c.description should be("тест")
          c.ctype should be(ComplaintType.UserFraud)
          c.created should be(now)
          c.userData should be(Complaint.UserData.Empty)
          c.source should  be(Some("WATCH"))
          users should not be empty
        case other => fail(s"Unexpected $other")
      }
    }
    "create complaint with user data" in {
      val now = new Timestamp(getNow / 1000 * 1000)

      val userData =
        UserData(
          Some(Group.MobileApp),
          Some(Application.Web),
          Some("placement"),
          Some(true)
        )

      complaintsDao.create("2", userId, UserType.Undefined, OfferId, "descr",
        ComplaintType.NoAnswer.code, now, userData, Some("WATCH")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
           complaints.size should be (1)
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.setModObjId(OfferId, s"modobj_$OfferId") match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be (2)
          complaints.count(_.userData.group.isDefined) should be (1)
          val c = complaints.find(_.userData.group.isDefined).get
          c.complaintId should be ("2")
          c.modobjId should be (Some(s"modobj_$OfferId"))
          c.userId should be (userId)
          c.offerId should be (OfferId)
          c.description should be ("descr")
          c.ctype should be (ComplaintType.NoAnswer)
          c.created should be (now)
          c.userData should be (userData)
        case other => fail(s"Unexpected $other")
      }
    }
    "return valid complaints" in {
      complaintsDao.validComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be(2)
          complaints.count(_.userData.group.isDefined) should be(1)
          val c = complaints.find(_.userData.group.isDefined).get
          c.complaintId should be("2")
          c.userId should be(userId)
          c.offerId should be(OfferId)
          c.description should be("descr")
          c.ctype should be(ComplaintType.NoAnswer)
          c.userData.group should be(Some(Group.MobileApp))
          users should not be (empty)
        case other => fail(s"Unexpected $other")
      }
    }
    "has fresh complaints" in {
      val now = new Timestamp(getNow / 1000 * 1000)

      complaintsDao.hasFreshComplaints(OfferId) match {
        case Success(false) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.create("3", userId, UserType.Undefined, OfferId, "descr2",
        ComplaintType.WrongAddress.code, now, UserData.Empty, Some("WATCH")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.hasFreshComplaints(OfferId) match {
        case Success(true) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "return complaints with decisions" in {
      complaintsDao.allComplaintsWithDecisions(Plain("unknown")) match {
        case Success((complaints, users, modObjs)) =>
          complaints should be (empty)
          users should be (empty)
          modObjs should be (empty)
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsWithDecisions(OfferId) match {
        case Success((complaints, users, modObjs)) =>
          complaints.size should be(3)
          complaints.count(_.userData.group.isDefined) should be(1)
          val c = complaints.find(_.userData.group.isDefined).get
          c.complaintId should be("2")
          c.userId should be(userId)
          c.offerId should be(OfferId)
          c.description should be("descr")
          c.ctype should be(ComplaintType.NoAnswer)
          c.userData.group should be(Some(Group.MobileApp))
          complaints.count(_.ctype == ComplaintType.WrongAddress) should be(1)
          users should not be (empty)
        case other => fail(s"Unexpected $other")
      }
    }
    "allow to push feedback" in {
      complaintsDao.feedback("unknown", positive = true) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.feedback("unknown", positive = false) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.feedback(s"modobj_$OfferId", positive = true) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.feedback(s"modobj_$OfferId", positive = false) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "allow to change modobj for new complaints" in {
      complaintsDao.setModObjId(OfferId, s"modobj2_$OfferId") match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be (3)
          complaints.count(_.modobjId.contains(s"modobj2_$OfferId")) should be (1)
        case other => fail(s"Unexpected $other")
      }
    }
    "return plain complaints" in {
      complaintsDao.plainComplaintsForOffer(OfferId) match {
        case Success(complaints) =>
          complaints.size should be (3)
        case other => fail(s"Unexpected $other")
      }
    }
    "set notified for complaints" in {
      val complaints = for {
        oldComplaints <- complaintsDao.allComplaintsForOffer(OfferId).map(_._1)
        ids = oldComplaints.map(_.complaintId).toSet
        _ <- complaintsDao.setNotified(ids)
        newComplaints <- complaintsDao.allComplaintsForOffer(OfferId).map(_._1)
      } yield (oldComplaints, newComplaints)

      complaints match {
        case Success((oldComplaints, newComplaints)) =>
          oldComplaints.forall(!_.notified) shouldBe true
          newComplaints.forall(_.notified) shouldBe true
        case other => fail(s"Unexpected $other")
      }
    }
    "remove only complaint" in {
      complaintsDao.plainRemove(Seq("1")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be (2)
          complaints.find(_.complaintId == "1") should be (empty)
          users should not be empty
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.plainRemove(Seq("0", "1")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "remove all"  in {
      complaintsDao.remove("1", None, OfferId) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be (2)
          complaints.find(_.complaintId == "1") should be (empty)
          users should not be (empty)
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.remove("2", Some(s"modobj2_$OfferId"), OfferId) match {
        case Failure(_: DataIntegrityViolationException) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints.size should be (2)
          complaints.find(_.complaintId == "1") should be (empty)
          complaints.find(_.complaintId == "2") should not be (empty)
          complaints.flatMap(_.modobjId) should not be (empty)
          users should not be (empty)
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.plainRemove(Seq("2")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.remove("3", Some(s"modobj2_$OfferId"), OfferId) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      complaintsDao.allComplaintsForOffer(OfferId) match {
        case Success((complaints, users)) =>
          complaints should be (empty)
          users should be (empty)
        case other => fail(s"Unexpected $other")
      }

    }
  }
}

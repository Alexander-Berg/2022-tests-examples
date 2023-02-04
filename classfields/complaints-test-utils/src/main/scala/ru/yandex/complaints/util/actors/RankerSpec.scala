package ru.yandex.complaints.util.actors

import java.sql.Timestamp

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.dao.{Plain, getNow}
import ru.yandex.complaints.model.Complaint.Group
import ru.yandex.complaints.model.ComplaintType.{Another, Commercial}
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.{Complaint, ComplaintType, User, UserType}
import ru.yandex.complaints.util.actors.Ranker.ComplaintsEnriched

import scala.util.Try

/**
  * Base specs for [[Ranker]]
  *
  * Created by s-reznick on 25.07.16.
  */
@RunWith(classOf[JUnitRunner])
class RankerSpec extends WordSpec with Matchers {
  import RankerSpec._

  val calcUserRank = Ranker.DefaultContext.calcUserRank _

  import DefaultRanker._

  "Ranker" should {
    "properly calculate rank for novice user" in {
      assert(calcUserRank(0.0f, 0.0f) == 0.5f)
    }

    "properly calculate rank when positive/negative components are equal" in {
      Seq(1.0f, 100.0f, 2.345f).foreach(e ⇒ {
        assert(calcUserRank(e, e) == 0.5f)
      })
    }
  }

  "Rank" should {
    "be > 0.5f if r component is greater then s component" in {
      0.5f.until(100f, 3.4f).foreach(e ⇒ {
        assert(calcUserRank(e + 0.01f, e) > 0.5f)
      })
    }

    "be < 0.5f if r component is less then s component" in {
      0.5f.until(100f, 3.4f).foreach(e ⇒ {
        assert(calcUserRank(e - 0.01f, e) < 0.5f)
      })
    }
  }

  val ManyTimes = 1000

  "Rank" should {
    "increase as long as r component is increased but be less then 1" in {
      Seq(0.5f, 10f, 123.45f).foreach(s ⇒ {
        val ranks = 1.until(ManyTimes).map(r ⇒ {
          calcUserRank(r, s)
        })

        assert(ranks.forall(_ > 0.0f))
        assert(ranks.forall(_ < 1.0f))

        assert(
          ranks.tail.zip(ranks.take(ranks.size - 1)).forall(e ⇒ e._1 > e._2))
      })
    }

    "decrease as long as s component" +
      " is increased but be greater then 0" in {
      Seq(0.5f, 10f, 123.45f).foreach(r ⇒ {
        val ranks = 1.until(ManyTimes).map(s ⇒ {
          calcUserRank(r, s)
        })

        assert(ranks.forall(_ > 0.0f))
        assert(ranks.forall(_ < 1.0f))

        assert(
          ranks.tail.zip(ranks.take(ranks.size - 1)).forall(e ⇒ e._1 < e._2))
      })
    }

    "decrease as long as r component" +
      " is decreased but be greater then 1/(s + 2)" in {
      Seq(0.5f, 10f, 123.45f).foreach(s ⇒ {
        val ranks = 1.until(ManyTimes).map(r ⇒ {
          calcUserRank(s / r.toFloat, s)
        })

        assert(ranks.forall(_ > 1.0f / (s + 2.0f)))
        assert(ranks.forall(_ < 1.0f))

        assert(
          ranks.tail.zip(ranks.take(ranks.size - 1)).forall(e ⇒ e._1 < e._2))
      })
    }

    "increase as long as s component" +
      " is increased but be less then (r + 1)/(r + 2)" in {
      Seq(0.5f, 10f, 123.45f).foreach(r ⇒ {
        val ranks = 1.until(ManyTimes).map(s ⇒ {
          calcUserRank(r, r / s.toFloat)
        })

        assert(ranks.forall(_ < (r + 1.0f) / (r + 2.0f)))
        assert(ranks.forall(_ > 0.0f))

        assert(
          ranks.tail.zip(ranks.take(ranks.size - 1)).forall(e ⇒ e._1 > e._2))
      })
    }
  }

  private def calcUserRank(user: User): Float =
    calcUserRank(user.ratingR, user.ratingS)

  "offerRank" should {
    "return None if empty list of complaints is passed" in {
      assert(offerRank(ComplaintsEnriched(Seq(), DefaultUserMap)).isEmpty)
      assert(offerRank(ComplaintsEnriched(Seq(), Map())).isEmpty)
    }
    
    "throw exception if empty map of users is passed" +
      " and some complaint is present" in {
      val result = Try {offerRank(ComplaintsEnriched(
        Seq(DefaultComplaint), Map()))}

      assert(result.isFailure)
    }

    "throw exception if key in map of users is not found for complaint" in {
      val result = Try {
        offerRank(ComplaintsEnriched(
          Seq(DefaultComplaint), Map(UserId("other") -> DefaultUser)))
      }

      assert(result.isFailure)
    }

    "return Some(<type weight> * <user rank>)" +
      " if single complaint is filled by user" in {
      Seq(DefaultUser, BetterUser).foreach(user ⇒
        ComplaintType.Instances.foreach(e ⇒ {
          val rank = offerRank(
            ComplaintsEnriched(
              Seq(DefaultComplaint.copy(userId = user.id, ctype = e)),
              singleUserMap(user)))
          val expected = Some(
            Ranker.DefaultContext.complaintsRank(e, None) * calcUserRank(user))
          assert(expected == rank)
        })
      )
    }

    "return Some(<type weight> * (<user1 rank> + <user2 rank>))" +
      " if two users provided complaint of single type" in {
      def complPair(ctype: ComplaintType): Seq[Complaint] = {
        Seq(
          DefaultComplaint.copy(userId = DefaultUser.id, ctype = ctype),
          DefaultComplaint.copy(userId = BetterUser.id, ctype = ctype))
      }

      ComplaintType.Instances.foreach(e ⇒ {
          val rank = offerRank(
            ComplaintsEnriched(
              complPair(e),
              DefaultUserMap ++ BetterUserMap)
          )
        val typeRank = Ranker.DefaultContext.complaintsRank(e, None)
          val expected = Some(typeRank * calcUserRank(DefaultUser) +
            typeRank * calcUserRank(BetterUser))
          assert(expected == rank)
        })
    }

    "ignore another complaint of the same type for the same user" in {
      def compl2(ctype: ComplaintType): Seq[Complaint] = {
        Seq(
          DefaultComplaint.copy(userId = DefaultUser.id, ctype = ctype),
          DefaultComplaint.copy(userId = BetterUser.id, ctype = ctype))
      }

      def compl3(ctype: ComplaintType, id: UserId): Seq[Complaint] =
        compl2(ctype) ++
          Seq(DefaultComplaint.copy(userId = id, ctype = ctype))


      ComplaintType.Instances.foreach(e ⇒ {
        val rank2 = offerRank(ComplaintsEnriched(
          compl2(e), DefaultUserMap ++ BetterUserMap))
        val rank3d = offerRank(ComplaintsEnriched(
          compl3(e, DefaultUser.id),
          DefaultUserMap ++ BetterUserMap))
        val rank3b = offerRank(ComplaintsEnriched(
          compl3(e, BetterUser.id),
          DefaultUserMap ++ BetterUserMap))

        assert(rank2 == rank3d)
        assert(rank2 == rank3b)
      })
    }

    "take into account only first found complaint in list from single user" in {
      object Ctx extends Ranker.BaseContext {
        override def complaintsRank(complaintType: ComplaintType,
                                    group: Option[Group]): Float = {
          complaintType.code.toFloat
        }
      }
      def pairs[T](elems: Seq[T]): Seq[(T, T)] = {
        def shift(n: Int) = elems.drop(n) ++ elems.take(n)
        shift(1).zip(shift(elems.size - 1))
      }
      def default(c: ComplaintType) = DefaultComplaint.copy(ctype = c)
      def rank(c: ComplaintType) = offerRank(
        ComplaintsEnriched(Seq(default(c)), DefaultUserMap)
      )(Ctx)
      def rankPair(c: (ComplaintType, ComplaintType)) = offerRank(
        ComplaintsEnriched(
          Seq(default(c._1), default(c._2)),
          DefaultUserMap)
      )(Ctx)

      for (pair <- pairs(ComplaintType.Instances)) {
        val rank1 = rank(pair._1)
        val rank2 = rank(pair._2)
        val rankDirect = rankPair(pair)
        val rankSwap = rankPair(pair.swap)

        assert(
          Seq(
            rank1, rank2,
            rankDirect, rankSwap
          ).forall(_.isDefined))
        assert(rank1 == rankDirect)
        assert(rank2 == rankSwap)
      }
    }

    "support for groups" in {
      object Ctx extends Ranker.BaseContext

      import Complaint.Group._

      Ctx.groupRank(MobileApp) should be (1.1f)
      Ctx.complaintsRank(Commercial, None) should be (5.0f)
      Ctx.complaintsRank(Commercial, Some(MobileApp)) should be (5.5f)
      Ctx.complaintsRank(Another, None) should be (2.1f)
      Ctx.complaintsRank(Another, Some(MobileApp)) should be (2.31f)
    }
  }
}

object RankerSpec {
  val DefaultUser = User(
    id = UserId("u12345"),
    `type` = UserType.Undefined,
    ratingR = Ranker.DefaultUserRankR,
    ratingS = Ranker.DefaultUserRankS,
    fixedRating = false,
    isVip = false,
    isAuto = false)
  val BetterUser = User(UserId("ub12345"), UserType.Undefined, 1, 0, fixedRating = true, isVip = true, isAuto = false)

  val now = new Timestamp(getNow)
  val DefaultComplaint = Complaint(
    userId = UserId("u12345"),
    userType = UserType.Undefined,
    offerId = Plain("o12345"),
    complaintId = "c12345",
    modobjId = Some("m12345"),
    ctype = ComplaintType.Commercial,
    description = "some text",
    created = now,
    userData = Complaint.UserData.Empty,
    notified = false,
    source = None
  )

  private def singleUserMap(user: User) = Map(user.id → user)

  def usersMap(users: Seq[User]): Map[UserId, User] = users.map(e => e.id -> e).toMap

  val DefaultUserMap = singleUserMap(DefaultUser)
  val BetterUserMap = singleUserMap(BetterUser)
}
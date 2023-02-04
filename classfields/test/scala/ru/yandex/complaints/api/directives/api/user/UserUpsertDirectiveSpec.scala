package ru.yandex.complaints.api.directives.api.user

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.api.directives.DirectiveSpec
import ru.yandex.complaints.api.directives.api.users.UserUpsertInfoDirective
import spray.routing.directives.BasicDirectives

/**
  * Created by s-reznick on 20.03.17.
  */
@RunWith(classOf[JUnitRunner])
class UserUpsertDirectiveSpec
  extends WordSpec
  with Matchers
  with DirectiveSpec with BasicDirectives {

  val GoodRanks = Seq("0", "0.0", "0.1", "1.0", "125")
  val BadRanks = Seq("gewgr", " ")

  val GoodIds = Seq("123456", "yandex_uid_987654321")

  def createMap(id: String, rankR: Option[String], rankS: Option[String],
                fixed: Option[String] = None,
                auto: Option[String] = None,
                vip: Option[String] = None): Map[String, String] = {
    normaizeMap(Seq(
      UserUpsertInfoDirective.RankR -> rankR,
      UserUpsertInfoDirective.RankS -> rankS,
      UserUpsertInfoDirective.UserID -> Some(id),
      UserUpsertInfoDirective.FixedRank -> fixed,
      UserUpsertInfoDirective.IsAuto -> auto,
      UserUpsertInfoDirective.IsVip -> vip
    ))
  }

  val GoodRequests =
    for {
      r <- withNone(GoodRanks)
      s <- withNone(GoodRanks)
      id <- GoodIds
      isFixed <- withNone(GoodFlags)
      isAuto <- withNone(GoodFlags)
      isVip <- withNone(GoodFlags)
    } yield createMap(id = id,
      rankR = r, rankS = s,
      fixed = isFixed, auto = isAuto, vip = isVip)


  "UserUpsertInfoDirective" should {
    "accept correct values" in {
        for (params <- GoodRequests) {
          val res = check(UserUpsertInfoDirective.instance, params)
          assert(res.isAccepted)
          assert(res.rejectReasons.isEmpty)
        }
    }

    "reject incorrect rankR" in {
      for (params <- withParam(GoodRequests,
        UserUpsertInfoDirective.RankR, BadRanks)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }

    "reject incorrect rankS" in {
      for (params <- withParam(GoodRequests,
        UserUpsertInfoDirective.RankS, BadRanks)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }

    "reject incorrect fixed rank flag" in {
      for (params <- withParam(GoodRequests,
        UserUpsertInfoDirective.FixedRank, BadFlags)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }

    "reject incorrect auto flag" in {
      for (params <- withParam(GoodRequests,
        UserUpsertInfoDirective.IsAuto, BadFlags)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }

    "reject incorrect vip flag" in {
      for (params <- withParam(GoodRequests,
        UserUpsertInfoDirective.IsVip, BadFlags)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }

    "reject request without id" in {
      for (params <- withoutParam(GoodRequests,
        UserUpsertInfoDirective.UserID)) {
        val res = check(UserUpsertInfoDirective.instance, params)
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }
  }
}
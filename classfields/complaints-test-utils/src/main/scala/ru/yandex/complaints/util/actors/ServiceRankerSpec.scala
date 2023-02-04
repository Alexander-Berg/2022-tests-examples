package ru.yandex.complaints.util.actors

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.config.ThresholdAware
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.{ComplaintType, User}
import ru.yandex.complaints.util.SignalRank
import ru.yandex.complaints.util.actors.Ranker.ComplaintsEnriched
import ru.yandex.complaints.util.environment.{DefaultEnvProvider, Env}
import ru.yandex.complaints.util.app.ConfigUtils.RichConfig

/**
  * Base spec for service [[Ranker]]
  *
  * Created by s-reznick on 20.02.17.
  */
abstract class ServiceRankerSpec extends WordSpec with Matchers {
  import ServiceRankerSpec._

  val service = serviceConfig
  val cfg = thresholdConfig

  import DefaultRanker._

  s"$service ranker" should {
    "never ban on first complaint from novice user" in {
      testFirstComplaint(cfg, offerRank)
    }
  }

  s"$service ranker" should {
    "never check on first complaint from low ranked user" in {
      object FixedUserRankCtx extends Ranker.BaseContext {
        override def calcUserRank(r: Float, s: Float): Float = 0.2f
      }

      for (ct <- ComplaintType.Instances) {
        val rank = offerRank(
          ComplaintsEnriched(
            Seq(RankerSpec.DefaultComplaint.copy(ctype = ct)),
            RankerSpec.DefaultUserMap))(FixedUserRankCtx)
        assert(!rank.exists(_ > cfg.check))
      }
    }
  }

  s"$service ranker" should {
    "never ban if complaints are only from low-ranked users" in {
      val LowUserRank = Ranker.UserBanThreshold - 0.01f
      val HighUserRank = Ranker.UserBanThreshold + 0.01f
      object Ctx extends Ranker.BaseContext {
        override def calcUserRank(r: Float, s: Float): Float = r
      }
      def singleRank(ctype: ComplaintType, userRank: Float): Float = {
        Ctx.complaintsRank(ctype, None) * userRank
      }
      def usersNeeded(ctype: ComplaintType, threshold: Float): Int =
        Math.ceil(threshold /
          singleRank(ctype, LowUserRank)).toInt
      def lowRankedUsers(count: Int): Seq[User] = {
        (0 until count).map(e => {
          val id = UserId(s"low_$e")
          RankerSpec.DefaultUser.copy(id = id, ratingR = LowUserRank)
        })
      }
      def highRankedUser = {
        RankerSpec.DefaultUser.copy(id = UserId("high"), ratingR = HighUserRank)
      }
      def enriched(ctype: ComplaintType, users: Seq[User]): ComplaintsEnriched = {
        ComplaintsEnriched(
          users.map(u => RankerSpec.DefaultComplaint.copy(userId = u.id, ctype = ctype)),
          RankerSpec.usersMap(users)
        )
      }
      implicit val thresholds = new ThresholdAware {
        val thresholdCheck = cfg.check
        val thresholdBan = cfg.ban
      }

      for (ctype <- ComplaintType.Instances) {
        val thresholdForLow = cfg.ban - singleRank(ctype, HighUserRank)
        val usersNoBan = lowRankedUsers(usersNeeded(ctype, cfg.ban))
        val usersBan = Seq(highRankedUser) ++
          usersNoBan.take(usersNeeded(ctype, thresholdForLow))
        val ceNoBan = enriched(ctype, usersNoBan)
        val ceBan = enriched(ctype, usersBan)

        val infoNoBan = extractInfo(ceNoBan)(Ctx)
        val infoBan = extractInfo(ceBan)(Ctx)

        assert(infoNoBan.isDefined)
        assert(infoBan.isDefined)

        assert(infoNoBan.get.rank > thresholds.thresholdBan)
        assert(infoBan.get.rank > thresholds.thresholdBan)

        val rankNoBan = signalRank(infoNoBan.get)
        val rankBan = signalRank(infoBan.get)

        assert(rankNoBan != SignalRank.Ban)
        assert(rankBan == SignalRank.Ban)
      }
    }
  }
}

object ServiceRankerSpec {

  case class ThresholdCfg(check: Float, ban: Float)

  val env: Env = new Env(DefaultEnvProvider)

  val serviceConfig =
    env.getConfig("complaints").getString("service")

  val thresholdConfig = {
    val service  =  env.getConfig("complaints").getString("service")

    val moderationConfig =
      RichConfig(env.getConfig(s"complaints.$service.moderation"))

    ThresholdCfg(
      check = moderationConfig.float("check-threshold"),
      ban = moderationConfig.float("ban-threshold")
    )
  }

  def testFirstComplaint(cfg: ThresholdCfg,
                         rankMethod: ComplaintsEnriched => Option[Float]) = {
    for (ct <- ComplaintType.Instances) {
      val rank = rankMethod(
        ComplaintsEnriched(
          Seq(RankerSpec.DefaultComplaint.copy(ctype = ct)),
          RankerSpec.DefaultUserMap))
      assert(!rank.exists(_ > cfg.ban))
    }
  }
}

package common.zio.tvm.test

import common.zio.tvm.{InvalidTicket, TvmClient, TvmConfig, UserTickets}
import ru.yandex.passport.tvmauth.{BlackboxEnv, TicketStatus}
import zio.ZLayer
import zio.test.Assertion._
import zio.test._

object UserTicketsSpec extends DefaultRunnableSpec {

  // generated via `ya tools tvmknife unittest user --default 2`
  private val ticket = "3:user:CA0Q__________9_Gg4KAggCEAIg0oXYzAQoAQ:F-9D-r5-bFSTUaKGM8V7h6jxhRG6gO26VIvRVaHKx5LuK7p" +
    "ruSdLlOhLNPPnX-vCoy0sP8HMoJcxl_YbBSolNwNO_UFxU7_E-CaSnjUrsFnKwhkWohYl0hfGvFV8CSsxAe-n39x7gbdSTkxJmgU8SIZ5nc3khvt" +
    "mS_qvgin-m0c"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("UserTickets")(
      testM("parse user ticket") {
        for {
          res <- UserTickets.verifyUserTicket(ticket).flip
        } yield assert(res)(isSubtype[InvalidTicket](hasField("status", _.status, equalTo(TicketStatus.MISSING_KEY))))
      },
      testM("parse invalid ticket") {
        for {
          res <- UserTickets.verifyUserTicket(ticket.dropRight(1)).flip
        } yield assert(res)(isSubtype[InvalidTicket](hasField("status", _.status, equalTo(TicketStatus.MISSING_KEY))))
      }
    ).provideCustomLayer {
      (ZLayer.succeed(
        TvmConfig(None, None, None, None, Some(BlackboxEnv.TEST))
      ) >>> TvmClient.live) >>> UserTickets.live
    }
  }
}

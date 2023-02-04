package common.clients.begemot.testkit

import common.clients.begemot.BegemotClient
import common.clients.begemot.BegemotClient.BegemotClient
import common.clients.begemot.model.{BegemotResponse, GeoAddrResponse, Rule, RulesResponse, TokenMorph}
import zio.{Task, ULayer, ZIO, ZLayer}

object TestBegemotClient extends BegemotClient.Service {
  override def getLemmas(text: String): Task[Seq[TokenMorph]] = ZIO.succeed(Nil)

  override def requestRules(text: String, rules: Set[Rule]): Task[BegemotResponse] = ZIO.succeed(
    BegemotResponse(
      rules = RulesResponse(None),
      markupResponse = None
    )
  )

  val layer: ULayer[BegemotClient] = ZLayer.succeed(TestBegemotClient)
}

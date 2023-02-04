package auto.dealers.match_maker.logic

import zio._
import auto.dealers.match_maker.logic.clients.BlacklistedPhones
import auto.dealers.match_maker.logic.clients.BlacklistedPhones.BlacklistedPhones

class TestBlacklistedPhones(bl: Set[String]) extends BlacklistedPhones.Service {

  override def contains(phone: String): UIO[Boolean] =
    ZIO.succeed(bl.contains(phone))

}

object TestBlacklistedPhones {

  def make(bl: String*): ZLayer[Any, Nothing, BlacklistedPhones] =
    ZLayer.succeed(new TestBlacklistedPhones(bl.toSet))

  val empty = make()

}

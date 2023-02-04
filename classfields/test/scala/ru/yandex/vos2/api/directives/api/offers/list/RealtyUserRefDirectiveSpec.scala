package ru.yandex.vos2.api.directives.api.offers.list

import akka.http.scaladsl.model.Uri.Path.{Empty, Segment}
import akka.http.scaladsl.server.PathMatcher.Matched
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vos2.model.{UserRef, UserRefPhone}

class RealtyUserRefDirectiveSpec extends FlatSpec with Matchers {

  behavior of RealtyUserRefDirective.getClass.getName

  private def process(segment: String): Option[UserRef] = {
    RealtyUserRefDirective.withUserRef.apply(Segment(segment, Empty)) match {
      case Matched(_, extractions) => Some(extractions._1)
      case _ => None
    }
  }

  it should "accept phone (hashed) refs" in {
    process("phone:somehash") shouldBe Some(UserRefPhone("somehash"))
  }

}

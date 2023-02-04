package ru.yandex.vertis.moderation.model.instance

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.instance.User.{Autoru, Yandex}

/**
  * Specs instance id unapply
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class InstanceIdImplSpec extends SpecBase {

  "InstanceIdImpl" should {

    import InstanceIdImpl._

    "unapply" in {
      unapply("CAESJggBEg0IARIJMjM2MTQ0MjYyGhMyODEzNDQ3ODI2NTA3MTMyMTYwGhBkNTk0NWU3NTMyYTYzZTE4") match {
        case Some(ExternalId(Yandex("236144262"), "2813447826507132160")) => ()
        case other                                                        => fail(s"Unexpected $other")
      }
      unapply("CAESJggBEg0IARIJMjM2MTQ0MjYyGhMyODEzNDQ3ODI2NTA3MTMyMTYwGhAzNTMxOGExNzRmZDZjZDBm") match {
        case Some(ExternalId(Yandex("236144262"), "2813447826507132160")) => ()
        case other                                                        => fail(s"Unexpected $other")
      }
      unapply("CAESIQgBEgwIATIIMjQ4ODYyOTQaDzEwNTMwMzM1MTgtYWVkMxoQMzEwZTBlZGQ0N2RiNmY3Mg==") match {
        case Some(ExternalId(Autoru("24886294"), "1053033518-aed3")) => ()
        case other                                                   => fail(s"Unexpected $other")
      }
    }
  }
}

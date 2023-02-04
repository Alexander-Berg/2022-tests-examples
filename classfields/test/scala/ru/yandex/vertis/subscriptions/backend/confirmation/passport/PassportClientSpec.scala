package ru.yandex.vertis.subscriptions.backend.confirmation.passport

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

/**
  * Specs on [[PassportClient]]
  */
@RunWith(classOf[JUnitRunner])
class PassportClientSpec extends Matchers with WordSpecLike {

  "PassportClient" should {
    "get validated from XML" in {
      PassportClient.getValidatedFromXml(
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |<uid hosted="0" domid="">123</uid>
          |<karma confirmed="0">0</karma>
          |<dbfield id="accounts.login.uid">name-family</dbfield>
          |<address-list>
          |<address validated="1" default="0" native="1" born-date="2009-07-10 21:07:52">test-user123@yandex.ru</address>
          |</address-list>
          |</doc>""".stripMargin
      ) should be(true)
    }

    "get not validated from XML" in {
      PassportClient.getValidatedFromXml(
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |<uid hosted="0" domid="">456</uid>
          |<karma confirmed="0">0</karma>
          |<address-list>
          |<address validated="0" default="0" native="1" born-date="2009-07-10 21:07:52">test-user123@yandex.ru</address>
          |</address-list>
          |</doc>""".stripMargin
      ) should be(false)
    }

    "get not validated from XML without validated attribute" in {
      PassportClient.getValidatedFromXml(
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |<uid hosted="0" domid="">456</uid>
          |<karma confirmed="0">0</karma>
          |<address-list>
          |<address default="0" native="1" born-date="2009-07-10 21:07:52">test-user123@yandex.ru</address>
          |</address-list>
          |</doc>""".stripMargin
      ) should be(false)
    }

    "get not validated from XML without address element" in {
      PassportClient.getValidatedFromXml(
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |<uid hosted="0" domid="">456</uid>
          |<karma confirmed="0">0</karma>
          |<address-list>
          |</address-list>
          |</doc>""".stripMargin
      ) should be(false)
    }

    "get not validated from XML without address-list element" in {
      PassportClient.getValidatedFromXml(
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |<uid hosted="0" domid="">456</uid>
          |<karma confirmed="0">0</karma>
          |<address-list>
          |</address-list>
          |</doc>""".stripMargin
      ) should be(false)
    }
  }
}

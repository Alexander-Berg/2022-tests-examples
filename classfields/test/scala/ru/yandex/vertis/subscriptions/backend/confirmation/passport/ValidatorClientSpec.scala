package ru.yandex.vertis.subscriptions.backend.confirmation.passport

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

/**
  * Specs on [[ValidatorClient]].
  */
@RunWith(classOf[JUnitRunner])
class ValidatorClientSpec extends Matchers with WordSpecLike {

  "ValidatorClient" should {
    "extract ValidationKey from response" in {
      ValidatorClient.parseGetKeyResponse(
        """
          |<?xml version="1.0" encoding="windows-1251"?>
          |<page>
          |  <validation-key>2061194517135584471291808636162681659946</validation-key>
          |</page>
        """.stripMargin
      ) should be(ValidatorClient.Response.ValidationKey("2061194517135584471291808636162681659946"))
    }
    "extract Validated from response" in {
      ValidatorClient.parseConfirmResponse(
        """
          |<?xml version="1.0" encoding="windows-1251"?>
          |<page>
          |  <validator-key-ok address="art@yandex-team.ru" uid="123456789">2061194517135584471291808636162681659946</validator-key-ok>
          |</page>
        """.stripMargin
      ) should be(ValidatorClient.Response.Validated("2061194517135584471291808636162681659946"))
    }
    "extract AlreadyValidated from response" in {
      ValidatorClient.parseConfirmResponse(
        """
          |<?xml version="1.0" encoding="windows-1251"?>
          |<page>
          |  <validator-key-already-validated address="art@yandex-team.ru" uid="123456789">2061194517135584471291808636162681659946</validator-key-already-validated>
          |</page>
        """.stripMargin
      ) should be(ValidatorClient.Response.KeyAlreadyValidated("2061194517135584471291808636162681659946"))
    }
    "extract InvalidArgument from response" in {
      ValidatorClient.parseConfirmResponse(
        """
          |<?xml version="1.0" encoding="windows-1251"?>
          |<page>
          |  <validator-invalid-argument>Address art@yandex.ru valid as native.</validator-invalid-argument>
          |</page>
        """.stripMargin
      ) should be(ValidatorClient.Response.InvalidArgument("Address art@yandex.ru valid as native."))
    }
  }

}

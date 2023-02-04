package ru.yandex.vos2.autoru.model.extdata

import java.io.StringReader

import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.shaded.org.apache.commons.io.input.ReaderInputStream

class ProvenOwnerValidationDictionarySpec extends AnyFunSuite {

  private val data: String =
    """[{
      |  "name": "proven_owner",
      |  "fullName": "/vertis-moderation/autoru/proven_owner",
      |  "flushDate": "2019-09-09T14:29:05.244Z",
      |  "version": 12,
      |  "mime": "application/json; charset=utf-8",
      |  "content": {
      |    "default": {
      |		    "text_proven_owner_ok": "ok",
      |	    	"text_proven_owner_failed": "failed",
      |		    "text_proven_owner_not_enough_photos": "not enough photos",
      |	     	"text_proven_owner_bad_photos": "bad photos",
      |	     	"push_proven_owner_information": "say hi",
      |		    "sending_active": true
      |	   }
      |  }
      |}]""".stripMargin
  test("parse json from bunker") {
    val expected = ProvenOwnerValidationDictionary(
      textProvenOwnerOk = Some("ok"),
      textProvenOwnerFailed = Some("failed"),
      textProvenOwnerNotEnoughPhotos = Some("not enough photos"),
      textProvenOwnerBadPhotos = Some("bad photos"),
      pushProvenOwnerInformation = Some("say hi"),
      sendingActive = true
    )
    val actual = ProvenOwnerValidationDictionary.parse(new ReaderInputStream(new StringReader(data), "utf-8"))
    assert(actual == expected)
  }
}

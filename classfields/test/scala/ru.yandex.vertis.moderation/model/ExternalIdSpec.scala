package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.instance.ExternalId
import ru.yandex.vertis.moderation.proto.Model.Service

/**
  * Specs for [[ExternalId]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class ExternalIdSpec extends SpecBase {

  "ExternalId" should {

    import Service.{AUTORU, REALTY}

    "get alternativeObjectId correctly" in {
      ExternalId("auto_ru_1#123").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#123-").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#123-123").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#123-aaa").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#-aaa").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#123_aaa").alternativeObjectId(AUTORU) should be(None)
      ExternalId("auto_ru_1#123-aaaa").alternativeObjectId(AUTORU) should be(Some("123"))
      ExternalId("auto_ru_1#123-a123").alternativeObjectId(AUTORU) should be(Some("123"))
      ExternalId("auto_ru_1#123-a123bc").alternativeObjectId(AUTORU) should be(Some("123"))
      ExternalId("auto_ru_1#123-a123bcffffff").alternativeObjectId(AUTORU) should be(Some("123"))
      ExternalId("auto_ru_1#1-a123bcffffff").alternativeObjectId(AUTORU) should be(Some("1"))
      ExternalId("auto_ru_1#aaaaaaaaaa-ffff").alternativeObjectId(AUTORU) should be(Some("aaaaaaaaaa"))
      ExternalId("auto_ru_1#1053016832-b0499").alternativeObjectId(AUTORU) should be(Some("1053016832"))
      ExternalId("auto_ru_1#1053016832-b0499").alternativeObjectId(REALTY) should be(None)
    }
  }

}

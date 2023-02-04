package ru.yandex.vertis.moderation.stopwords

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.extdatacore.Dsl
import ru.yandex.vertis.moderation.extdatacore.model.verba.stopwords.VerbaActions
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.stopwords.impl.verba.{VerbaKey, VerbaStopwordsProtobufConversions}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class VerbaStopwordsProtobufConversionsSpec extends SpecBase {

  "converter" should {

    "do correct conversion from Protobuf" in {
      val protoStopwords =
        Dsl.newVerbaStopwords(
          List(
            Dsl.newVerbaStopwordsTerm("all", "ban", "all_ban"),
            Dsl.newVerbaStopwordsTerm("auto-cars", "ban", "autoru_ban")
          )
        )
      val result = VerbaStopwordsProtobufConversions.fromMessage(protoStopwords)
      result(VerbaKey(Service.REALTY, VerbaActions.Ban)) should be(Set("all_ban"))
      result(VerbaKey(Service.AUTORU, VerbaActions.Ban)) should be(Set("autoru_ban", "all_ban"))
    }
  }
}

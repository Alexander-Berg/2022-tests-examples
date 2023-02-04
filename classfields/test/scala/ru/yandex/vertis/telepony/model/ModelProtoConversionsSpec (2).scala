package ru.yandex.vertis.telepony.model

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.proto.ModelProtoConversions._
import ru.yandex.vertis.telepony.{ProtoSpecBase, SpecBase}

/**
  * @author neron
  */
class ModelProtoConversionsSpec extends SpecBase with ScalaCheckPropertyChecks with ProtoSpecBase {

  implicit val generatorConfig = PropertyCheckConfiguration(1000)

  "Proto transformer" should {
    "transform redirect key" in {
      forAll(RedirectKeyGen)(test(_, RedirectKeyProtoConversion))
    }
    "transform redirect tag" in {
      forAll(TagGen)(test(_, RedirectTagProtoConversion))
    }
    "transform redirect antifraud" in {
      forAll(AntiFraudOptionSetGen)(test(_, AntiFraudProtoConversion))
    }
  }

}

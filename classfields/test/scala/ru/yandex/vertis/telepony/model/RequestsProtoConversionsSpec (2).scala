package ru.yandex.vertis.telepony.model

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.proto.RequestsProtoConversions._
import ru.yandex.vertis.telepony.{ProtoSpecBase, SpecBase}

/**
  * @author neron
  */
class RequestsProtoConversionsSpec extends SpecBase with ScalaCheckPropertyChecks with ProtoSpecBase {

  implicit val generatorConfig = PropertyCheckConfiguration(1000)

  "Proto transformer" should {
    "transform update redirect request" in {
      forAll(UpdateRedirectRequestGen)(test(_, UpdateRedirectRequestProtoConversion))
    }
    "transform create-from-existing redirect request" in {
      forAll(CreateFromExistingRedirectRequestGen)(test(_, CreateFromExistingRedirectRequestProtoConversion))
    }
  }

}

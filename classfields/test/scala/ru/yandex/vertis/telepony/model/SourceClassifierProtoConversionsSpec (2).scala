package ru.yandex.vertis.telepony.model

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.{ProtoSpecBase, SpecBase}
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.proto.ModelProtoConversions.CallResultProtoConversion
import ru.yandex.vertis.telepony.proto.SourceClassifierProtoConversions._

class SourceClassifierProtoConversionsSpec extends SpecBase with ScalaCheckPropertyChecks with ProtoSpecBase {

  implicit val generatorConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(1000)

  "SourceClassifier models transformer" should {
    "transform OperatorNumber" in {
      forAll(SourceClassifierOperatorNumberGen)(test(_, OperatorNumberProtoConversion))
    }

    "transform CallerNumber" in {
      forAll(SourceClassifierCallerNumberGen)(test(_, CallerNumberProtoConversion))
    }

    "transform CallResult" in {
      forAll(CallResultGen)(test(_, CallResultProtoConversion))
    }

    "transform PhoneType" in {
      forAll(PhoneTypeGen)(test(_, PhoneTypeProtoConversion))
    }

    "transform SourceClass" in {
      forAll(SourceClassifierSourceClassesGen)(test(_, SourceClassProtoConversion))
    }

    "transform Operator" in {
      forAll(OperatorGen)(test(_, OperatorProtoConversion))
    }

    "transform SourceClassifierRequest" in {
      forAll(SourceClassifierRequestGen)(test(_, SourceClassifierRequestProtoConversion))
    }

    "transform SourceClassifierResponse" in {
      forAll(SourceClassifierResponseGen)(test(_, SourceClassifierResponseProtoConversion))
    }

    "transform Call" in {
      forAll(SourceClassifierCallGen)(test(_, CallProtoConversion))
    }

    "transform BlockedCall" in {
      forAll(SourceClassifierBlockedCallGen)(test(_, BlockedCallProtoConversion))
    }

    "transform UnmatchedCall" in {
      forAll(SourceClassifierUnmatchedGen)(test(_, UnmatchedCallProtoConversion))
    }
  }

}

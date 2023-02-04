package ru.yandex.vertis.telepony.proto

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.generator.TeleponyCallGenerator.{TeleponyNonRedirectBlockedCallGen, TeleponyRedirectBlockedCallGen}
import ru.yandex.vertis.telepony.model.{proto, TeleponyCall}
import ru.yandex.vertis.telepony.proto.TeleponyCallProto.CallToProtoBlockedCallConversion

class TeleponyCallProtoSpec extends SpecBase with ScalaCheckPropertyChecks {

  "CallToProtoBlockedCallConversion" should {
    "fail to convert a non-blocked call" in {
      val call: TeleponyCall = TeleponyNonRedirectBlockedCallGen.next
      intercept[IllegalArgumentException](CallToProtoBlockedCallConversion.toProto(call))
    }

    "convert blocked call" in {
      val call: TeleponyCall = TeleponyRedirectBlockedCallGen.next
      val actual = CallToProtoBlockedCallConversion.toProto(call)
      actual.getCallType shouldBe proto.TeleponyCall.CallType.REDIRECT_CALL
      actual.getCallId shouldBe call.id
    }
  }
}

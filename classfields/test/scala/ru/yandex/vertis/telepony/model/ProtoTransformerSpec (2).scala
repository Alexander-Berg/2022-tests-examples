package ru.yandex.vertis.telepony.model

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.{TeleponyCallGenerator, UnmatchedTeleponyCallGenerator}
import ru.yandex.vertis.telepony.proto.CallbackEventProtoFormat.SimpleVoxCallbackEventConversion
import ru.yandex.vertis.telepony.proto.ProtoConversions._
import ru.yandex.vertis.telepony.proto.RawCallbackProtoFormat.RawCallbackConversion
import ru.yandex.vertis.telepony.proto.RedirectUpdateActionProto.RedirectUpdateActionProtoConversion
import ru.yandex.vertis.telepony.proto.TeleponyCallProto
import ru.yandex.vertis.telepony.proto.UnmatchedTeleponyCallProto.UnmatchedTeleponyCallConversion
import ru.yandex.vertis.telepony.{ProtoSpecBase, SpecBase}

/**
  * @author neron
  */
class ProtoTransformerSpec extends SpecBase with ScalaCheckPropertyChecks with ProtoSpecBase {

  implicit val generatorConfig = PropertyCheckConfiguration(1000)

  "Proto transformer" should {
    "transform raw call" in {
      forAll(RawCallGen)(test(_, RawCallProtoConversion))
    }
    "transform history redirect" in {
      forAll(HistoryRedirectGen)(test(_, HistoryRedirectProtoConversion))
    }
    "transform maybe matched call" in {
      forAll(MaybeMatchedCallGen)(test(_, MaybeMatchedCallProtoConversion))
    }
    "transform touch redirect" in {
      forAll(TouchRedirectRequestGen)(test(_, TouchRedirectProtoConversion))
    }
    "transform redirect update action" in {
      forAll(RedirectUpdateActionGen)(test(_, RedirectUpdateActionProtoConversion))
    }
    "transform UpdateTargetAudioRequest" in {
      forAll(UpdateTargetAudioRequestGen)(test(_, UpdateTargetAudioRequestProtoConversion))
    }
    "transform redirect options" in {
      forAll(RedirectOptionsGen)(test(_, RedirectOptionsProtoConversion))
    }
    "transform complaint cause type" in {
      forAll(ComplaintCauseTypeGen)(test(_, ComplaintCauseTypeProtoConversion))
    }
    "transform complaint status" in {
      forAll(ComplaintStatusGen)(test(_, ComplaintStatusProtoConversion))
    }
    "transform complaint order direction" in {
      forAll(ComplaintOrderDirectionGen)(test(_, ComplaintOrderDirectionProtoConversion))
    }
    "transform complaint order field" in {
      forAll(ComplaintOrderFieldGen)(test(_, ComplaintOrderFieldProtoConversion))
    }
    "transform complaints filter" in {
      forAll(ComplaintFilterGen)(test(_, ComplaintFilterProtoConversion))
    }
    "transform complaints filter seq" in {
      forAll(ComplaintFilterSeqGen)(test(_, ComplaintFilterListProtoConversion))
    }
    "transform complaints sort order" in {
      forAll(ComplaintSortOrderGen)(test(_, ComplaintSortOrderProtoConversion))
    }
    "transform complaints slice request" in {
      forAll(ComplaintSliceRequestGen)(test(_, ComplaintSliceRequestProtoConversion))
    }
    "transform complaint comment" in {
      forAll(ComplaintCommentGen)(test(_, ComplaintCommentProtoConversion))
    }
    "transform complaint cause" in {
      forAll(ComplaintCauseGen)(test(_, ComplaintCauseProtoConversion))
    }
    "transform complaint payload" in {
      forAll(ComplaintPayloadGen)(test(_, ComplaintPayloadProtoConversion))
    }
    "transform complaint" in {
      forAll(ComplaintGen)(test(_, ComplaintProtoConversion))
    }
    "transform complaints slice result" in {
      forAll(ComplaintSlicedResultGen)(test(_, ComplaintSlicedResultProtoConversion))
    }
    "transform complaint cause update request" in {
      forAll(ComplaintCauseUpdateRequestGen)(test(_, ComplaintCauseUpdateRequestProtoConversion))
    }
    "transform complaint call info" in {
      forAll(ComplaintCallInfoGen)(test(_, ComplaintCallInfoProtoConversion))
    }
    "transform complaint call info update request" in {
      forAll(ComplaintCallInfoUpdateRequestGen)(test(_, ComplaintCallInfoUpdateRequestProtoConversion))
    }
    "transform complaint create request" in {
      forAll(ComplaintCreateRequestGen)(test(_, ComplaintCreateRequestProtoConversion))
    }
    "transform hobo telepony call mark and call marks" in {
      forAll(PositiveCallMarkGen)(test(_, HoboMarkingResolutionConversion))
    }
    "transform telepony call" in {
      forAll(TeleponyCallGenerator.TeleponyCallGen)(test(_, TeleponyCallProto.TeleponyCallConversion))
    }
    "transform callback events" in {
      forAll(CallbackGenerator.VoxCallbackEventGen)(test(_, SimpleVoxCallbackEventConversion))
    }
    "transform raw callback" in {
      forAll(CallbackGenerator.RawCallbackGen)(test(_, RawCallbackConversion))
    }
    "transform unmatched telepony call" in {
      forAll(UnmatchedTeleponyCallGenerator.UnmatchedTeleponyCallGen)(test(_, UnmatchedTeleponyCallConversion))
    }
  }
}

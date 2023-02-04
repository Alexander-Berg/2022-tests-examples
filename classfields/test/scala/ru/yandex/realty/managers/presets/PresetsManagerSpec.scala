package ru.yandex.realty.managers.presets

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.giraffic.GirafficClient
import ru.yandex.realty.errors.CommonError
import ru.yandex.realty.giraffic.presets.{GirafficPresetsRequest, GirafficPresetsResponse, PresetBlock}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.proto.search.PresetBlockIdentity
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  *
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class PresetsManagerSpec extends AsyncSpecBase with RequestAware {

  private val girafficClient = mock[GirafficClient]
  private val manager = new PresetsManager(girafficClient)

  private val theFailure = Future.failed(CommonError("ERROR_CODE", "Error message"))

  private def makeGirafficResponse(blocks: PresetBlockIdentity*) =
    GirafficPresetsResponse(
      blocks.map(PresetBlock(_, Seq.empty))
    )

  "PresetManager" should {
    "correctly build response from giraffic response" in {
      val src = UserRef.web("2386512367")
      val rgid = 12L

      (girafficClient
        .presets(_: GirafficPresetsRequest)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(makeGirafficResponse(PresetBlockIdentity.APARTMENT_SELL)))

      withRequestContext(src) { implicit r =>
        val result = manager.extractPresets(rgid).futureValue
        result.hasApartmentSell shouldBe true
        result.hasApartmentRent shouldBe false
      }
    }

    "fail if searcher returns error" in {
      val src = UserRef.web("2386512367")
      val rgid = 12L

      (girafficClient
        .presets(_: GirafficPresetsRequest)(_: Traced))
        .expects(*, *)
        .returning(theFailure)

      interceptCause[CommonError] {
        withRequestContext(src) { implicit r =>
          manager.extractPresets(rgid).futureValue
        }
      }
    }
  }
}

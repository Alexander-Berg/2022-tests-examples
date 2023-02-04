package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.raw.RawOffer
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.vertis.moderation.proto.Model.DetailedReason
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.PhoneInDesc
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.PhoneInDesc.Phone

@RunWith(classOf[JUnitRunner])
class DescriptionUnifierSpec extends AsyncSpecBase with MockFactory with Matchers with OneInstancePerTest {

  val rawPhone = "+8-123-456-78 90 (девяносто a)"
  val maskedPhone = "+*-***-***-** ** (********* *)"

  implicit val trace: Traced = Traced.empty

  val badDesc: String =
    "Аренда этажа в БЦ \"Н2О\": площадь 760 кв. м. Освобождается с февраля 2019 года." +
      " В это же время освобождается 8-й этаж.<br>&#10;\n\n\n\n\n\t Видовые " +
      s"характеристики: <br> отличные виды с высоты &lt;p&gt;7-го этажа на Охтинский разлив $rawPhone"

  val unifiedDesc: String =
    "Аренда этажа в БЦ \"Н2О\": площадь 760 кв. м. Освобождается с февраля 2019 года." +
      " В это же время освобождается 8-й этаж.\n\t Видовые " +
      s"характеристики: \n отличные виды с высоты 7-го этажа на Охтинский разлив $maskedPhone"

  val offer: Offer = mock[Offer]
  val rawOffer: RawOffer = mock[RawOffer]
  val offerWrapper: OfferWrapper = mock[OfferWrapper]
  val descUnifier = new DescriptionUnifier

  val moderationOpinion: Model.Opinion = Model.Opinion
    .newBuilder()
    .setVersion(1)
    .addWarnDetailedReasons(
      DetailedReason
        .newBuilder()
        .setDetails(
          Details
            .newBuilder()
            .setPhoneInDesc(
              PhoneInDesc
                .newBuilder()
                .addPhones(
                  Phone
                    .newBuilder()
                    .setRawPhone(rawPhone)
                )
            )
        )
    )
    .build()

  "DescriptionUnifierTest" should {

    "unify with Description" in {
      (rawOffer.getModerationOpinion _).expects().returning(moderationOpinion)
      (rawOffer.getDescription _).expects().returning(badDesc)
      (offerWrapper.getRawOffer _).expects().returning(rawOffer)

      (offer.setDescription _).expects(unifiedDesc).returning(())
      (offerWrapper.getOffer _).expects().returning(offer)

      descUnifier.unify(offerWrapper).futureValue shouldBe true
    }

    "not setDescription without Description" in {
      (rawOffer.getModerationOpinion _).expects().returning(null)
      (rawOffer.getDescription _).expects().returning(null)
      (offerWrapper.getRawOffer _).expects().returning(rawOffer)

      descUnifier.unify(offerWrapper).futureValue shouldBe true
    }

  }
}

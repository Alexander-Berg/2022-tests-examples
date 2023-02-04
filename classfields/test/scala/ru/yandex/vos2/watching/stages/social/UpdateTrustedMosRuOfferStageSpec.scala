package ru.yandex.vos2.watching.stages.social

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.PersonFullName
import ru.yandex.realty.proto.social.trusted.{MosRuTrustedStatus, TrustedOfferInfo, TrustedUserInfo}
import ru.yandex.realty.util.CryptoUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.UserModel.UserType
import ru.yandex.vos2.{OfferModel, UserModel}
import ru.yandex.vos2.model.TrustedGenerator
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.ProcessingState
import ru.yandex.vos2.watching.stages.social.UpdateTrustedMosRuOfferStageSpec._
import ru.yandex.vos2.watching.utils.ProbeIdxClient

@RunWith(classOf[JUnitRunner])
class UpdateTrustedMosRuOfferStageSpec extends WordSpec with Matchers with MockitoSupport with PropertyChecks {

  private val randomAes256KeyBase64 = "Qj9FKEgrTWJRZVRoV21acTR0N3cheiVDKkYpSkBOY1I="
  private val crypto: CryptoUtils.Crypto = CryptoUtils.Crypto.create(randomAes256KeyBase64)

  // use encryption inside, so need to use the same encryption in generator and stage
  private val trustedGen = new TrustedGenerator(crypto)
  val client = new ProbeIdxClient
  private val stage = new UpdateTrustedMosRuOfferStage(crypto, client)

  "UpdateTrustedMosRuOfferStage" should {

    "not update because trusted user and not matched name " in {
      forAll(
        trustedGen.TrustedUserInfoGen,
        RealtyOfferGenerator.offerGen()
      ) { (trustedUserInfo, offer) =>
        val trustedUser =
          trustedUserInfo.setIsTrustedUser(true).build()
        val diffSurname = trustedUser.getMosRuPersonEncrypted.toBuilder.setSurname(crypto.encrypt("Мояфамилия")).build()
        val offerProc = offer.toBuilder
          .setCadastrPersonToOffer(diffSurname)
          .setUserType()
          .setTrustedInfoToOfferUser(trustedUser)
          .build()
        val offerResTrusted = getOfferTrusted(processOffer(offerProc))
        stage.shouldProcess(offerProc) shouldBe true
        offerResTrusted.getIsFullTrustedOwner shouldBe false
      }
    }

    "update full trusted user" in {
      forAll(
        trustedGen.TrustedUserInfoGen,
        RealtyOfferGenerator.offerGen()
      ) { (trustedUserInfo, offer) =>
        val surname = "Мояфамилия"

        /**
          * during encryption we use IV (init vector) parameter which means the result
          * of encryption string will be different on each encryption process. Example:
          * val first = encrypt(name)
          * val second = encrypt(name)
          * first.toString != second.toString
          * but decrypt(first) == decrypt(second)
          */
        val diffSurnamePerson =
          trustedUserInfo.getMosRuPersonEncrypted.toBuilder.setSurname(crypto.encrypt(surname)).build()
        val trustedUser =
          trustedUserInfo
            .setIsTrustedUser(true)
            .setMosRuPersonEncrypted(
              trustedUserInfo.getMosRuPersonEncrypted.toBuilder.setSurname(crypto.encrypt(surname))
            )
            .build()
        val offerProc = offer.toBuilder
          .setTrustedInfoToOfferUser(trustedUser)
          .setUserType()
          .setCadastrPersonToOffer(diffSurnamePerson)
          .build()
        val offerResTrusted = getOfferTrusted(processOffer(offerProc))
        stage.shouldProcess(offerProc) shouldBe true
        offerResTrusted.getIsFullTrustedOwner shouldBe true
      }
    }
  }

  private def processOffer(offer: OfferModel.Offer): ProcessingState = stage.process(ProcessingState(offer, offer))
  private def getOfferTrusted(state: ProcessingState) = state.offer.getOfferRealty.getTrustedOfferInfo
}

object UpdateTrustedMosRuOfferStageSpec {
  implicit class UserUpdate(userInfo: TrustedUserInfo.Builder) {

    def setIsTrustedUser(trusted: Boolean): TrustedUserInfo.Builder =
      userInfo.setMosRuTrustedStatus({
        if (trusted) MosRuTrustedStatus.TRUSTED else MosRuTrustedStatus.NOT_TRUSTED
      })
  }
  implicit class OfferUpdate(offer: OfferModel.Offer.Builder) {

    def setCadastrPersonToOffer(person: PersonFullName): OfferModel.Offer.Builder =
      setCadastrPersonToOffer(person.toBuilder)

    def setCadastrPersonToOffer(person: PersonFullName.Builder): OfferModel.Offer.Builder =
      offer.setOfferRealty(
        offer.getOfferRealty.toBuilder
          .setCadastrInfo(
            offer.getOfferRealty.getCadastrInfo.toBuilder
              .addOwnerPersonsEncrypted(person)
              .build()
          )
          .build()
      )

    def setTrustedInfoToOfferUser(trustedUserInfo: TrustedUserInfo): OfferModel.Offer.Builder =
      offer.setUser(
        offer.getUser.toBuilder
          .setTrustedUserInfo(
            trustedUserInfo
          )
          .build()
      )

    def setUserType(ut: UserType = UserType.UT_OWNER): OfferModel.Offer.Builder =
      offer.setUser(
        offer.getUser.toBuilder
          .setUserType(
            ut
          )
          .build()
      )
  }
}

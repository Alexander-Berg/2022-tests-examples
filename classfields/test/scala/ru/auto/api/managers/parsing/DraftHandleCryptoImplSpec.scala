package ru.auto.api.managers.parsing

import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators.{OfferIDGen, PhoneGen, PrivateUserRefGen, StrictCategoryGen}
import ru.auto.api.util.crypt.base64.Base64
import ru.auto.api.util.crypt.blowfish.{BlowfishCrypto, BlowfishKeyGen}
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.mockito.MockitoSupport

class DraftHandleCryptoImplSpec extends BaseSpec with MockitoSupport {

  trait Fixture {
    val seed = readableString.next
    val key = BlowfishKeyGen.generate(seed)
    val crypto = new BlowfishCrypto(key, Base64.UrlSafe2)
    val draftHandleCrypto = new DraftHandleCryptoImpl(crypto)
  }

  "DraftHandleCryptoImpl" should {
    "encrypt and decrypt successfully" when {
      "everything is ok" in new Fixture {
        val category = StrictCategoryGen.next
        val offerId = OfferIDGen.next
        val user = PrivateUserRefGen.next
        val phone = PhoneGen.next
        val draftHandle = draftHandleCrypto.encrypt(user, phone, category, offerId)
        val decrypted = draftHandleCrypto.decrypt(draftHandle)
        decrypted.isSuccess shouldBe true
        decrypted.get shouldBe DraftHandle(user, phone, category, offerId)
      }
    }

    "decrypt with error" when {
      "pass wrong draftHandle" in new Fixture {
        val draftHandle = readableString.next
        val decrypted = draftHandleCrypto.decrypt(draftHandle)
        decrypted.isFailure shouldBe true
      }
    }
  }
}

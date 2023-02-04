package ru.yandex.realty.util.crypto

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.util.CryptoUtils

@RunWith(classOf[JUnitRunner])
class CryptoUtilsSpec extends AsyncSpecBase {

  "CryptoUtils" should {
    "correctly encrypt/decrypt data" in {
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val key = CryptoUtils.Aes256KeyGen.generate
      val crypto = new CryptoUtils.Crypto(key)
      val encrypted = crypto.encrypt(text)
      val decrypted = crypto.decryptToStr(encrypted)
      decrypted shouldBe text
    }

    "encrypt and decrypt msg with different instances of the Crypto" in {
      import CryptoUtils.RichSecretKey
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val key1 = CryptoUtils.Aes256KeyGen.generate
      val crypto1 = new CryptoUtils.Crypto(key1)
      val key2 = CryptoUtils.Aes256KeyGen.fromBase64Str(key1.toBase64Str)
      val crypto2 = new CryptoUtils.Crypto(key2)
      crypto2.decryptToStr(crypto1.encrypt(text)) shouldBe text
    }
  }
}

package ru.auto.api.util.crypt.blowfish

import org.apache.commons.codec.binary.Base64
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.BaseSpec
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.util.crypt.CryptoUtils.RichSecretKey

class BlowfishCryptoSpec extends BaseSpec {
  "BlowfishKeyGen" should {
    "generate key from seed" in {
      pending
      val seed = BasicGenerators.readableString.next
      val key = BlowfishKeyGen.generate(seed).key.toBase64Str
    }

    "generate same key from same seed" in {
      val seed = BasicGenerators.readableString.next
      val key1 = BlowfishKeyGen.generate(seed).key.toBase64Str
      val key2 = BlowfishKeyGen.generate(seed).key.toBase64Str
      key1 shouldBe key2
    }

    "generate different keys without seed" in {
      val key1 = BlowfishKeyGen.generateWithRandomSeed.key.toBase64Str
      val key2 = BlowfishKeyGen.generateWithRandomSeed.key.toBase64Str
      key1 shouldNot be(key2)
    }
  }

  "BlowfishCrypto" should {
    "deserialize key from base64 and bytes" in {
      val key = BlowfishKeyGen.generateWithRandomSeed.key
      val bytesKey = key.getEncoded
      val base64StrKey = key.toBase64Str
      val base64Key = key.toBase64

      val fromBase64Key = BlowfishKeyGen.fromBase64(base64Key).key
      val fromBase64StrKey = BlowfishKeyGen.fromBase64Str(base64StrKey).key
      val fromBytesKey = BlowfishKeyGen.fromBytes(bytesKey).key
      fromBase64Key.getEncoded shouldBe key.getEncoded
      fromBase64StrKey.getEncoded shouldBe key.getEncoded
      fromBytesKey.getEncoded shouldBe key.getEncoded
    }

    "serialize key to base64" in {
      val key = BlowfishKeyGen.generateWithRandomSeed.key
      val base64StrKey = key.toBase64Str
      val base64Key = key.toBase64
      Base64.isBase64(base64Key) shouldBe true
      Base64.isBase64(base64StrKey) shouldBe true
    }

    "encrypt and decrypt msg with same Crypto" in {
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val key = BlowfishKeyGen.generateWithRandomSeed
      val crypto = new BlowfishCrypto(key)
      val encrypted = crypto.encrypt(text)
      val decrypted = crypto.decryptToStr(encrypted)
      decrypted shouldBe text
    }

    "encrypt and decrypt msg with different Crypto" in {
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val key1 = BlowfishKeyGen.generateWithRandomSeed
      val crypto1 = new BlowfishCrypto(key1)
      val key2 = BlowfishKeyGen.fromBase64Str(key1.key.toBase64Str)
      val crypto2 = new BlowfishCrypto(key2)
      crypto2.decryptToStr(crypto1.encrypt(text)) shouldBe text
    }

    "encrypt and decrypt protobuf with parser correctly" in {
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val msg = Offer.newBuilder.setNote(text).build
      val key1 = BlowfishKeyGen.generateWithRandomSeed
      val crypto1 = new BlowfishCrypto(key1)
      val key2 = BlowfishKeyGen.fromBase64Str(key1.key.toBase64Str)
      val crypto2 = new BlowfishCrypto(key2)
      crypto2.decryptToProto(crypto1.encrypt(msg))(Offer.getDefaultInstance.getParserForType) shouldBe msg
    }

    "encrypt and decrypt protobuf with prototype correctly" in {
      val text =
        """
          |A block cipher works on units of
          |a fixed size (known as a block size),
          |but messages come in a variety of lengths.
          |So some modes (namely ECB and CBC) require that the final block be padded before encryption.
          |Several padding schemes exist.
        """.stripMargin
      val msg = Offer.newBuilder.setNote(text).build
      val key1 = BlowfishKeyGen.generateWithRandomSeed
      val crypto1 = new BlowfishCrypto(key1)
      val key2 = BlowfishKeyGen.fromBase64Str(key1.key.toBase64Str)
      val crypto2 = new BlowfishCrypto(key2)
      crypto2.decryptToProtoWithPrototype(crypto1.encrypt(msg))(Offer.getDefaultInstance) shouldBe msg
    }
  }
}

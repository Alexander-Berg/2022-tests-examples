package ru.yandex.vertis.passport.util.crypt

import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.passport.test.ModelGenerators

/**
  *
  * @author zvez
  */
trait EncryptorSpec extends WordSpec with Matchers with PropertyChecks {

  def encryptor: Encryptor

  "Encryptor" should {
    "encrypt and decrypt" in {
      forAll(ModelGenerators.readableString) { s =>
        val encrypted = encryptor.encrypt(s)
        encrypted should not be s
        val decrypted = encryptor.decrypt(encrypted)
        decrypted shouldBe s
      }
    }
  }

}

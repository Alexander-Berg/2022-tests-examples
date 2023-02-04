package vertis.palma.encrypted.cipher

import java.security.SecureRandom

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import vertis.palma.dao.model.Encrypted.PlainKey
import vertis.palma.encrypted.cipher.AESGCMCipher
import vertis.palma.gen.ModelGenerators.StrGen
import vertis.zio.test.ZioSpecBase

/** @author ruslansd
  */
class AESGCMCipherSpec extends ZioSpecBase with ScalaCheckPropertyChecks {

  private val cipher = new AESGCMCipher

  private val secretKey = PlainKey {
    val random = new SecureRandom()
    val array = new Array[Byte](32)
    random.nextBytes(array)
    array
  }

  private def encrypt(bytes: Array[Byte]) =
    cipher.encrypt(bytes, secretKey)

  private def decrypt(bytes: Array[Byte]) =
    cipher.decrypt(bytes, secretKey)

  "AESGSMEncrypter" should {

    "generate valid aes gcm key" in ioTest {
      val text = "Hello World!"
      for {
        key <- cipher.generatePlainKey
        encrypted <- cipher.encrypt(text.getBytes, key)
        decrypted <- cipher.decrypt(encrypted, key)
        _ <- check(new String(decrypted) shouldBe text)
      } yield ()
    }

    "encrypt and decrypt static text" in ioTest {
      val text = "Hello World!"
      for {
        encrypted <- encrypt(text.getBytes)
        decrypted <- decrypt(encrypted)
        _ <- check(new String(decrypted) shouldBe text)
      } yield ()
    }

    "encrypt and decrypt random text" in {
      forAll(StrGen) { text =>
        ioTest {
          for {
            encrypted <- encrypt(text.getBytes)
            decrypted <- decrypt(encrypted)
            _ <- check(new String(decrypted) shouldBe text)
          } yield ()
        }
      }
    }
  }
}

package vertis.palma.encrypted

import org.scalatest.Assertion
import vertis.palma.dao.model.Encrypted.{EncryptedKeyContent, EncryptedKeyPair, KeyPair}
import vertis.zio.test.ZioSpecBase
import zio.Task

trait EncryptedSpecBase extends ZioSpecBase {

  def checkBytes(actual: Array[Byte], expected: Array[Byte]): Task[Assertion] =
    check((actual should contain).theSameElementsInOrderAs(expected))

  def checkKeyPair(actual: KeyPair, expected: KeyPair): Task[Unit] = {
    for {
      _ <- check(actual.keyId shouldBe expected.keyId)
      _ <- checkBytes(actual.key.bytes, expected.key.bytes)
    } yield ()
  }

  def checkEncryptedKeyPair(actual: EncryptedKeyPair, expected: EncryptedKeyPair): Task[Unit] = {
    for {
      _ <- check(actual.keyId shouldBe expected.keyId)
      _ <- checkBytes(actual.encryptedKey.bytes, expected.encryptedKey.bytes)
    } yield ()
  }

  def checkKeys(actual: EncryptedKeyContent, expected: EncryptedKeyContent): Task[Unit] = {
    for {
      _ <- check(actual.encryptedKeyContext.dictionaryId shouldBe expected.encryptedKeyContext.dictionaryId)
      _ <- check(actual.encryptedKeyContext.masterKeyHash shouldBe expected.encryptedKeyContext.masterKeyHash)
      _ <- checkEncryptedKeyPair(actual.encryptedKeyPair, expected.encryptedKeyPair)
    } yield ()
  }
}

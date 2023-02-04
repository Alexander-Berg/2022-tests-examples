package vertis.palma.gen

import java.util.UUID

import vertis.palma.DictionaryId
import vertis.palma.conf.MasterKeyConfig
import vertis.palma.dao.model.Encrypted.{
  EncryptedKey,
  EncryptedKeyContent,
  EncryptedKeyContext,
  EncryptedKeyPair,
  PlainKey
}
import vertis.palma.encrypted.cipher.AESGCMCipher
import zio.{Task, ZIO}

object EncryptedGenerators {

  private val cipher = new AESGCMCipher

  def generateKey(dictionaryId: DictionaryId, masterKeyOpt: Option[PlainKey] = None): Task[EncryptedKeyContent] = {
    for {
      id <- Task(UUID.randomUUID().toString)
      key <- cipher.generatePlainKey
      masterKey <- ZIO.fromOption(masterKeyOpt).orElse(cipher.generatePlainKey)
      encryptedBytes <- cipher.encrypt(key.bytes, masterKey)
    } yield EncryptedKeyContent(
      EncryptedKeyPair(id, EncryptedKey(encryptedBytes)),
      EncryptedKeyContext(dictionaryId, MasterKeyConfig.cryptoHash(masterKey.bytes))
    )
  }
}

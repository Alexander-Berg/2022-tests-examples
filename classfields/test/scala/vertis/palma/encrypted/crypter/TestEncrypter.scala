package vertis.palma.encrypted.crypter

import vertis.palma.DictionaryId
import vertis.palma.dao.model.Encrypted
import vertis.zio.RTask

/** @author kusaeva
  */
class TestEncrypter extends Encrypter {

  override def encrypt(dictionaryId: DictionaryId, content: Encrypted.PlainContent): RTask[Encrypted.EncryptedContent] =
    ???

  override def decrypt(dictionaryId: DictionaryId, content: Encrypted.EncryptedContent): RTask[Encrypted.PlainContent] =
    ???
}

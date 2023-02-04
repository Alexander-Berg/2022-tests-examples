package ru.yandex.vertis.passport.util.crypt

/**
  *
  * @author zvez
  */
class BlowfishEncryptorSpec extends EncryptorSpec {
  override val encryptor: Encryptor = new BlowfishEncryptor("Some secret key")
}

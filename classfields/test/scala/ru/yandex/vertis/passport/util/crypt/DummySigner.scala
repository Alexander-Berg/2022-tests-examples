package ru.yandex.vertis.passport.util.crypt

object DummySigner extends Signer {
  override def sign(data: Array[Byte]): Array[Byte] = data
}

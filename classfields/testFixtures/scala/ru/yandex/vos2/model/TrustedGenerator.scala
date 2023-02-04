package ru.yandex.vos2.model

import org.scalacheck.Gen
import ru.yandex.realty.proto.PersonFullName
import ru.yandex.realty.proto.social.trusted.{MosRuTrustedStatus, TrustedUserInfo}
import ru.yandex.realty.util.CryptoUtils.Crypto
import ru.yandex.vos2.model.CommonGen.genRusString

class TrustedGenerator(crypto: Crypto) {

  val PersonCryptGen = for {
    firstName <- genRusString(5)
    lastName <- genRusString(7)
    patr <- genRusString(8)
  } yield PersonFullName
    .newBuilder()
    .setName(crypto.encrypt(firstName))
    .setSurname(crypto.encrypt(lastName))
    .setPatronymic(crypto.encrypt(patr))

  val TrustedUserInfoGen = for {
    mosRuTrusted <- Gen.oneOf(
      MosRuTrustedStatus.REQUESTED,
      MosRuTrustedStatus.TRUSTED,
      MosRuTrustedStatus.NOT_TRUSTED,
      MosRuTrustedStatus.NOT_PROCESSED
    )
    person <- PersonCryptGen
  } yield TrustedUserInfo
    .newBuilder()
    .setMosRuTrustedStatus(mosRuTrusted)
    .setMosRuPersonEncrypted(person)
}

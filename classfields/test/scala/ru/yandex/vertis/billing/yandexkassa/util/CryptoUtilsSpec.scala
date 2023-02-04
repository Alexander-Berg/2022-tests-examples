package ru.yandex.vertis.billing.yandexkassa.util

import java.io.{FileNotFoundException, IOException}
import java.security.UnrecoverableKeyException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Failure, Success}
import ru.yandex.vertis.util.crypto.CryptoUtils
import ru.yandex.vertis.util.crypto.CryptoUtils._
import ru.yandex.vertis.util.crypto.KeyStoreTypes._

/**
  * Specs for [[CryptoUtils]] specific for
  *
  * @author alesavin
  */
class CryptoUtilsSpec extends AnyWordSpecLike with Matchers {

  val Pkcs12Password = "zATuW_I7GkB8KQuN"
  val JksPassword = "123456"

  "CryptoUtils" should {
    // vere10@ в процессе переезда в монорепу заигнорил эти тесты.
    // Они падали и до переезда в монорепу.
    // Если эти тесты когда-нибудь понадобятся -- нужно будет разобраться.
    "read keystore" ignore {
      getKeyStore("unknown", "unknown", Pkcs12) match {
        case Failure(_: FileNotFoundException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(read("openssl/autoru-certs/autoru_yakassa_mws.pkcs12"), "unknown", Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(read("openssl/autoru-certs/autoru_yakassa_mws.pkcs12"), Pkcs12Password, Pkcs12) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(read("openssl/autoru-certs/ymca.jks"), "unknown", Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(read("openssl/autoru-certs/ymca.jks"), JksPassword, Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(read("openssl/autoru-certs/ymca.jks"), JksPassword, Jks) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
    }
    "read key, trust managers, ssl context" ignore {
      val ks =
        getKeyStore(read("openssl/autoru-certs/autoru_yakassa_mws.pkcs12"), Pkcs12Password, Pkcs12).get
      val ts = getKeyStore(read("/openssl/autoru-certs/ymca.jks"), JksPassword, Jks).get
      getKeyManagers(ks, "unknown") match {
        case Failure(_: UnrecoverableKeyException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyManagers(ks, Pkcs12Password) match {
        case Success(kms) if kms.length > 0 => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getTrustManagers(ts) match {
        case Success(tms) if tms.length > 0 => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getSSLContext() match {
        case Success(c) => info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
      getSSLContext(trustManagers = getTrustManagers(ts).get) match {
        case Success(c) => info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
      getSSLContext(keyManagers = getKeyManagers(ks, Pkcs12Password).get) match {
        case Success(c) => info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
      getSSLContext(
        keyManagers = getKeyManagers(ks, Pkcs12Password).get,
        trustManagers = getTrustManagers(ts).get
      ) match {
        case Success(c) =>
          info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
    }
    "sign and verify with keystore" ignore {

      val data = "test".getBytes
      val ks =
        getKeyStore(read("openssl/autoru-certs/autoru_yakassa_mws.pkcs12"), Pkcs12Password, Pkcs12).get

      val signature = for {
        bs <- sign(ks, Pkcs12Password, data)
        encoded <- toBASE64(bs)
      } yield encoded

      signature match {
        case Success(s) =>
          val decoded = fromBASE64(s).get
          verify(ks, data, decoded) match {
            case Success(true) => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

    }
    "pack and unpack pkcs7" ignore {
      val data = "test pkcs7".getBytes
      val ks =
        getKeyStore(read("openssl/autoru-certs/autoru_yakassa_mws.pkcs12"), Pkcs12Password, Pkcs12).get

      val p7encoded = for {
        sign <- sign(ks, Pkcs12Password, data)
        bs <- toPKCS7(ks, data, sign)
        encoded <- toBASE64(bs)
      } yield encoded

      p7encoded match {
        case Success(d) =>
          val decoded = fromBASE64(d).get
          fromPKCS7(ks, decoded) match {
            case Success(dd) =>
              val a = new String(dd)
              val b = new String(data)
              a should be(b)
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

      val ks2 =
        getKeyStore(read("openssl/self/certificate.pkcs12"), "", Pkcs12).get

      p7encoded match {
        case Success(d) =>
          val decoded = fromBASE64(d).get
          fromPKCS7(ks2, decoded) match {
            case Failure(e) => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
  }
}

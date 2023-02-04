package ru.yandex.vertis.util.crypto

import java.io.{FileNotFoundException, IOException, InputStream}
import java.security.UnrecoverableKeyException

import org.scalatest.{Matchers, WordSpecLike}

import scala.util.{Failure, Success}

/**
  * Specs for [[CryptoUtils]]
  *
  * @author alesavin
  */
class CryptoUtilsSpec
  extends WordSpecLike
    with Matchers {

  val Password1 = "12345"
  val Password2 = "123456"

  def read(path: String): InputStream = getClass.getResourceAsStream(path)
  def path(path: String): String = getClass.getResource(path).getPath

  import CryptoUtils._
  import KeyStoreTypes._

  "CryptoUtils" should {
    "read keystore without type" in {
      getKeyStore("unknown", "unknown") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore("unknown.pkcs12", "unknown") match {
        case Failure(_: FileNotFoundException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore("unknown.jks", "unknown") match {
        case Failure(_: FileNotFoundException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore("unknown.JKS", "unknown") match {
        case Failure(_: FileNotFoundException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "read keystore" in {
      getKeyStore("unknown", "unknown", Pkcs12) match {
        case Failure(_: FileNotFoundException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        read("/certs/1/certificate.pkcs12"),
        "unknown", Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        read("/certs/1/certificate.pkcs12"),
        Password1, Pkcs12) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        path("/certs/1/certificate.pkcs12"),
        Password1) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        path("/certs/2/certificate.pkcs12"),
        Password1) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        read("/certs/1/certificate.jks"),
        "unknown", Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        read("/certs/1/certificate.jks"),
        Password1, Pkcs12) match {
        case Failure(_: IOException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        read("/certs/1/certificate.jks"),
        Password2, Jks) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
      getKeyStore(
        path("/certs/1/certificate.jks"),
        Password2) match {
        case Success(ks) => info(s"Done $ks")
        case other => fail(s"Unexpected $other")
      }
    }
    "read key, trust managers, ssl context" in {
      val ks =
        getKeyStore(
          read("/certs/1/certificate.pkcs12"),
          Password1, Pkcs12).get
      val ts = getKeyStore(
        read("/certs/1/certificate.jks"),
        Password2, Jks).get
      getKeyManagers(ks, "unknown") match {
        case Failure(_: UnrecoverableKeyException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      getKeyManagers(ks, Password1) match {
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
      getSSLContext(keyManagers = getKeyManagers(ks, Password1).get) match {
        case Success(c) => info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
      getSSLContext(
        keyManagers = getKeyManagers(ks, Password1).get,
        trustManagers = getTrustManagers(ts).get) match {
        case Success(c) =>
          info(s"Done $c")
        case other => fail(s"Unexpected $other")
      }
    }
    "sign and verify with keystore" in {

      val data = "test".getBytes
      val ks =
        getKeyStore(
          read("/certs/1/certificate.pkcs12"),
          Password1, Pkcs12).get

      val signature = for {
        bs <- sign(ks, Password1, data)
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
    "pack and unpack pkcs7" in {
      val data = "test pkcs7".getBytes
      val ks =
        getKeyStore(
          read("/certs/1/certificate.pkcs12"),
          Password1, Pkcs12).get

      val p7encoded = for {
        sign <- sign(ks, Password1, data)
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
              a should be (b)
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

      val ks2 =
        getKeyStore(
          read("/certs/2/certificate.pkcs12"),
          Password1, Pkcs12).get

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
    "wrap begin, end of pkcs7" in {

      val data = "test".getBytes
      val ks =
        getKeyStore(
          read("/certs/1/certificate.pkcs12"),
          Password1, Pkcs12).get

      val signature = (for {
        bs <- sign(ks, Password1, data)
        encoded <- toBASE64(bs)
      } yield encoded).get

      wrapBeginEnd()(signature) match {
        case Success(wrapped) =>
          unwrapBeginEnd()(wrapped) match {
            case Success(un) =>
              un should be (signature)
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

      unwrapBeginEnd()("") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\n-----END PKCS7-----") match {
        case Success(un) if un.isEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\n\n\n-----END PKCS7-----") match {
        case Success(un) if un.isEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\na\n-----END PKCS7-----") match {
        case Success(un) if un.length == 1 => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----EGIN PKCS7-----\na\n-----END PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\na\n-----EN PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKC7-----\na\n-----END PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\na\n-----END PS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----a\n-----END PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("\n\n-----BEGIN PKCS7-----a\n-----END PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("_-----BEGIN PKCS7-----a\n\n-----END PKCS7-----") match {
        case Failure(_: IllegalArgumentException) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      unwrapBeginEnd()("-----BEGIN PKCS7-----\nProc-Type: 4,ENCRYPTED\n\na\n-----END PKCS7-----") match {
        case Success(un) if un.length == 1 => info("Done")
        case other => fail(s"Unexpected $other")
      }
      unwrapBeginEnd()("-----BEGIN PKCS7-----\nProc-Type: 4,ENCRYPTED\nDEK-Info: DES-EDE3-CBC,9FB34B847D0C7AE0\na\n-----END PKCS7-----") match {
        case Success(un) if un.length == 1 => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
  }
}




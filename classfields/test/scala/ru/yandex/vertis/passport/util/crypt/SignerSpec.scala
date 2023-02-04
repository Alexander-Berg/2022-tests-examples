package ru.yandex.vertis.passport.util.crypt

import org.scalatest.{FreeSpec, Matchers}
import ru.yandex.vertis.passport.config.SessionIdConfig

import scala.util.Random

/**
  * Tests for [[Signer]] implementations
  *
  * @author zvez
  */
class SignerSpec extends FreeSpec with Matchers {

  "HmacSigner:" - signerTest(new HmacSigner(SessionIdConfig.readFromResource("/crypt/session.secret").secret))

  def signerTest(signer: Signer): Unit = {
    "sign and validate" in {
      val data = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      val signature = signer.sign(data)
      signer.validate(data, signature) shouldBe true
    }

    "sign and validate as Uuid" in {
      val data = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      val signature = signer.signAsUuid(data)
      signer.validateUuid(data, signature) shouldBe true
    }

    "should see data or signature were altered" in {
      val data = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      val signature = signer.sign(data)
      val otherData = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      signer.validate(otherData, signature) shouldBe false
      signer.validate(data + "1", signature) shouldBe false
      signer.validate(data, signature + "1") shouldBe false
    }

    "should see data or signature as Uuid were altered" in {
      val data = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      val signature = signer.signAsUuid(data)
      val otherData = Random.alphanumeric.take(Random.nextInt(20) + 10).mkString
      signer.validate(otherData, signature) shouldBe false
      signer.validateUuid(data + "1", signature) shouldBe false
      signer.validateUuid(data, signature + "1") shouldBe false
    }
  }

}

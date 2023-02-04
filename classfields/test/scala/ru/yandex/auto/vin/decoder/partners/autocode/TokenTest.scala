package ru.yandex.auto.vin.decoder.partners.autocode

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.partners.autocode.model.Token

import java.time.LocalDateTime

class TokenTest extends AnyFlatSpec with MockitoSugar with Matchers with BeforeAndAfter {

  "Token" should "exprire" in {

    val token = Token("hello", LocalDateTime.now(), 12)

    token.isExpired() shouldBe false

    Thread.sleep(3000)

    token.isExpired() shouldBe true

    Thread.sleep(10000)

    token.isExpired() shouldBe true

  }

}

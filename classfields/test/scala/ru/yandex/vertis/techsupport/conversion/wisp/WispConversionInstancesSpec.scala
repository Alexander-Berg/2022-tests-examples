package ru.yandex.vertis.vsquality.techsupport.conversion.wisp

import com.google.protobuf.timestamp.Timestamp
import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.conversion.Result.ValidationResult
import ru.yandex.vertis.vsquality.techsupport.conversion.wisp.WispConversionInstancesSpec.{
  WispCommonMessageGen,
  WispRatingMessageGen
}
import ru.yandex.vertis.vsquality.techsupport.model.api.RequestMeta
import ru.yandex.vertis.vsquality.techsupport.model.{Domain, Request, Url}
import ru.yandex.vertis.techsupport.proto.model.Api
import ru.yandex.vertis.techsupport.proto.model.Api.WispMessage
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

import java.time.Instant

class WispConversionInstancesSpec extends SpecBase {

  private def checkValid(x: ValidationResult[Request.TechsupportAppeal.ProcessMessage]): Unit =
    if (x.isInvalid) fail(s"Invalid result $x")

  "WispConversionInstances" should {

    "deserialize only General domain" in {
      val rm = gen[RequestMeta].generate()
      val message1 = WispCommonMessageGen.generate()
      val message2 = WispRatingMessageGen.generate()

      Domain.values.filterNot(_ == Domain.General).foreach { domain =>
        WispConversionInstances.deserialize(message1, domain)(rm).isInvalid shouldBe true
        WispConversionInstances.deserialize(message2, domain)(rm).isInvalid shouldBe true
      }
    }

    "deserialize only correct userId" in {
      val rm = gen[RequestMeta].generate()
      val message = WispCommonMessageGen.generate()
      val messageWithPrefixed = message.copy(userId = "yandex_uid_1234567890")
      val messageWithNegative = message.copy(userId = "-1234567890")
      val messageWithCorrectId = message.copy(userId = "1234567890")

      WispConversionInstances.deserialize(messageWithPrefixed, Domain.General)(rm).isInvalid shouldBe true
      WispConversionInstances.deserialize(messageWithNegative, Domain.General)(rm).isInvalid shouldBe true
      WispConversionInstances.deserialize(messageWithCorrectId, Domain.General)(rm).isValid shouldBe true
    }

    "deserialize only if timestamp is set" in {
      val rm = gen[RequestMeta].generate()
      val message = WispCommonMessageGen.generate().copy(timestamp = None)

      WispConversionInstances.deserialize(message, Domain.General)(rm).isInvalid shouldBe true
    }

    "deserialize only if message type is not empty" in {
      val rm = gen[RequestMeta].generate()
      val message = WispCommonMessageGen.generate().copy(messageType = WispMessage.MessageType.Empty)

      WispConversionInstances.deserialize(message, Domain.General)(rm).isInvalid shouldBe true
    }

    "deserialize expected messages successfully" in {
      (1 to 100).foreach { _ =>
        val message = WispCommonMessageGen.generate()
        val rm = gen[RequestMeta].generate()
        checkValid(WispConversionInstances.deserialize(message, Domain.General)(rm))
      }

      (1 to 100).foreach { _ =>
        val message = WispRatingMessageGen.generate()
        val rm = gen[RequestMeta].generate()
        checkValid(WispConversionInstances.deserialize(message, Domain.General)(rm))
      }
    }
  }
}

object WispConversionInstancesSpec {

  private val MessagePayloadGen: Gen[Api.Message] =
    for {
      text      <- alphaNumStr.arbitrary
      imageUrls <- gen[Url].listUpToN(3)
    } yield {
      Api.Message(
        text = Some(text),
        imageUrls = imageUrls
      )
    }

  private val RatingMessagePayloadGen: Gen[Api.RatingMessage] =
    for {
      rating <- Gen.choose(1, 3)
    } yield {
      Api.RatingMessage(
        rating = rating
      )
    }

  private val WispCommonMessageGen: Gen[WispMessage] =
    wispMessageGen(MessagePayloadGen.map(WispMessage.MessageType.Message))

  private val WispRatingMessageGen: Gen[WispMessage] =
    wispMessageGen(RatingMessagePayloadGen.map(WispMessage.MessageType.RatingMessage))

  def wispMessageGen(messageType: Gen[_ <: WispMessage.MessageType]): Gen[WispMessage] = {
    for {
      ts        <- gen[Instant].map(x => Timestamp.of(x.getEpochSecond, 0))
      userId    <- longNonNegative.arbitrary.map(_.toString)
      messageId <- longNonNegative.arbitrary
      message   <- messageType
    } yield {
      WispMessage(
        timestamp = Some(ts),
        userId = userId,
        messageId = messageId,
        messageType = message
      )
    }
  }
}

package vertis.broker.tests

import com.google.protobuf.{ByteString, DynamicMessage, Message}
import ru.yandex.vertis.broker.requests.WriteRequest
import ru.yandex.vertis.validation.model.{Invalid, MissingRequiredField, Valid, ValidationResult}
import vertis.broker.api.parse.Parser
import vertis.broker.api.parse.Parser.{ParseError, ParsedMessage, UnknownError}
import vertis.broker.api.validate.Validator
import zio.{IO, UIO}

import java.time.Instant

/** @author kusaeva
  */
object ProducerSessionSupport {

  object FailingParser extends Parser {
    val unknownError = UnknownError("Just always fail")

    override def parseProto(bin: ByteString): IO[ParseError, ParsedMessage] =
      IO.fail(unknownError)
  }

  object FailingValidator extends Validator {
    val reasons = Seq(MissingRequiredField("none"))

    override def validate(message: Message): IO[Validator.ValidationError, ValidationResult] =
      IO.fail(Validator.InvalidError(Invalid(reasons)))
  }

  object DummyParser extends Parser {

    override def parseProto(bin: ByteString): IO[ParseError, ParsedMessage] =
      IO.succeed {
        ParsedMessage(
          DynamicMessage.getDefaultInstance(WriteRequest.WriteData.javaDescriptor),
          Instant.now()
        )
      }
  }

  object DummyValidator extends Validator {

    override def validate(message: Message): IO[Validator.ValidationError, ValidationResult] =
      UIO(Valid)
  }
}

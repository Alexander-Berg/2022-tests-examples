package vertis.anubis.api.test

import vertis.anubis.api.services.validate.errors.CommonValidationError.NotASnakeCase
import vertis.anubis.api.services.validate.ErrorTypes
import vertis.anubis.api.services.validate.validators.FieldNameValidator
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class FieldNameValidatorSpec extends ZioSpecBase with ValidationSpecBase {

  val validator = new FieldNameValidator(ErrorTypes.Error)

  "FieldNameValidator" should {
    "fail for not a snake_case field names" in ioTest {
      val descriptor = NotASnakeCaseMessage.getDescriptor

      validator
        .validate(descriptor)
        .map(
          _.fold(
            errors =>
              errors.toChain.toList should contain theSameElementsAs List(
                NotASnakeCase(descriptor, "camelCase")
              ),
            _ => fail("validation passed but shouldn't")
          )
        )
    }
  }
}

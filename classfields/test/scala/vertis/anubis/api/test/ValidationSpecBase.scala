package vertis.anubis.api.test

import com.google.protobuf.Descriptors.Descriptor
import vertis.anubis.api.services.validate.errors.AnubisValidationError
import vertis.anubis.api.services.validate.validators.ProtoValidator
import vertis.zio.test.ZioSpecBase

/** @author ruslansd
  */
trait ValidationSpecBase {
  this: ZioSpecBase =>

  protected def checkFail(
      validator: ProtoValidator,
      descriptor: Descriptor,
      expectedErrors: List[_ <: AnubisValidationError]) =
    validator
      .validate(descriptor)
      .map(
        _.fold(
          errors => errors.toChain.toList should contain theSameElementsAs expectedErrors,
          _ => fail(s"validation of ${descriptor.getFullName} passed but shouldn't")
        )
      )

  protected def checkSucceed(validator: ProtoValidator, descriptor: Descriptor) =
    validator
      .validate(descriptor)
      .map(
        _.fold(e => fail(s"validation of ${descriptor.getFullName} failed with $e but shouldn't"), _ => succeed)
      )
}

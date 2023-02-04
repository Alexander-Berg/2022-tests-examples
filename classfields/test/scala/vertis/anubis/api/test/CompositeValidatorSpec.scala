package vertis.anubis.api.test

import ru.yandex.vertis.broker.Partitioning
import vertis.anubis.api.services.validate.ErrorTypes
import vertis.anubis.api.services.validate.errors.BrokerValidationError._
import vertis.anubis.api.services.validate.errors.CommonValidationError.NotASnakeCase
import vertis.anubis.api.services.validate.validators.broker.BrokerValidator
import vertis.anubis.api.services.validate.validators.{CompositeValidator, FieldNameValidator}
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class CompositeValidatorSpec extends ZioSpecBase with ValidationTestSupport {

  val validator = new CompositeValidator(
    List(
      new BrokerValidator(world, Set.empty),
      new FieldNameValidator(ErrorTypes.Error)
    )
  )

  "CompositeValidator" should {
    "fail for invalid messages" in ioTest {
      val descriptor = InvalidMessage.getDescriptor

      validator
        .validate(descriptor)
        .map(
          _.fold(
            errors =>
              errors.toChain.toList should contain theSameElementsAs List(
                IllegalPartitioning(descriptor, Partitioning.BY_MONTH, "spawn"),
                IllegalPartitioning(descriptor, Partitioning.BY_MONTH, "repartition"),
                NotASnakeCase(descriptor, "camelCase"),
                UnknownField(descriptor, "repartition", Seq("event_timestamp")),
                NoTimestampField(descriptor)
              ),
            _ => fail("validation passed but shouldn't")
          )
        )
    }
  }
}

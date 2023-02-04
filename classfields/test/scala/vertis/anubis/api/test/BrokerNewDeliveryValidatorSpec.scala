package vertis.anubis.api.test

import ru.yandex.vertis.broker.Partitioning
import vertis.anubis.api.services.validate.errors.BrokerValidationError._
import vertis.anubis.api.services.validate.validators.broker.BrokerNewDeliveryValidator
import vertis.zio.test.ZioSpecBase

/** @author reimai
  */
class BrokerNewDeliveryValidatorSpec extends ZioSpecBase with ValidationTestSupport {

  "BrokerNewDeliveryValidator" should {
    "detect new deliveries" in ioTest {
      val myWorld =
        world.copy(fdsToCheck = world.fdsToCheck.toBuilder.addFile(ValidMessage.getDescriptor.getFile.toProto).build())
      val validator = new BrokerNewDeliveryValidator(myWorld)
      val descriptor = ValidMessage.getDescriptor

      checkFail(
        validator,
        descriptor,
        List(NewDeliveryWarning(descriptor), NonDefaultPartitioningWarning(descriptor, Partitioning.BY_MONTH))
      )
    }

    "check name for new deliveries" in ioTest {
      val myWorld =
        world.copy(fdsToCheck = world.fdsToCheck.toBuilder.addFile(ValidMessage.getDescriptor.getFile.toProto).build())
      val validator = new BrokerNewDeliveryValidator(myWorld)
      val descriptor = SnakeNamedMessage.getDescriptor

      checkFail(
        validator,
        descriptor,
        List(NewDeliveryWarning(descriptor), IllegalStreamName(descriptor, "anubis/api/test_new"))
      )
    }

    "check services for new deliveries" in ioTest {
      val myWorld =
        world.copy(fdsToCheck = world.fdsToCheck.toBuilder.addFile(ValidMessage.getDescriptor.getFile.toProto).build())
      val validator = new BrokerNewDeliveryValidator(myWorld)
      val descriptor = NoServiceMessage.getDescriptor

      checkFail(
        validator,
        descriptor,
        List(NewDeliveryWarning(descriptor), NoServices(descriptor))
      )
    }

    "ignore existing ones" in ioTest {
      val validator = new BrokerNewDeliveryValidator(world.copy(masterSchema = fds(defaultDescriptors)))
      val descriptor = InvalidMessage.getDescriptor

      checkSucceed(validator, descriptor)
    }

    "show message" ignore ioTest {
      import cats.implicits._
      import vertis.anubis.api.services.validate.validators.ProtoValidator._

      val descriptor = ValidMessage.getDescriptor
      val x = Left(NewDeliveryWarning(descriptor)).toValidatedNec
      logger
        .info(x.getMessage)
        .as(succeed)
    }
  }
}

package vertis.anubis.api.test

import vertis.anubis.api.services.validate.errors.BrokerValidationError._
import vertis.anubis.api.services.validate.validators.broker.BrokerValidator
import vertis.shiva.client.ShivaClientSupport

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class BrokerValidatorIntSpec extends ShivaClientSupport with ValidationTestSupport {

  "BrokerValidator" should {
    "check services via service-map" in withShivaClient { shivaClient =>
      shivaClient.listServices() >>= { services =>
        val validator = new BrokerValidator(world, services.map(_.name).toSet)
        val descriptor = IllegalServiceMessage.getDescriptor
        checkFail(
          validator,
          descriptor,
          List(
            IllegalService(descriptor, "wubba-lubba-dub-dub"),
            IllegalService(descriptor, "not-a-service")
          )
        )
      }
    }
  }
}

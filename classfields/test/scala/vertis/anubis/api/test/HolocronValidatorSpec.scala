package vertis.anubis.api.test

import ru.yandex.vertis.broker.Partitioning
import vertis.anubis.api.services.validate.errors.BrokerValidationError._
import vertis.anubis.api.services.validate.errors.CommonValidationError.NoRequiredField
import vertis.anubis.api.services.validate.validators.broker.HolocronValidator
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class HolocronValidatorSpec extends ZioSpecBase with ValidationTestSupport {
  private val validator = new HolocronValidator

  "HolocronValidator" should {
    "validate required holocron fields" in ioTest {
      val descriptor = HolocronMessage.getDescriptor
      checkSucceed(
        validator,
        descriptor
      )
    }
    "fail when some of the required fields are absent or have invalid type" in ioTest {
      val descriptor = InvalidHolocronMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          NoRequiredField(descriptor, "id", "STRING"),
          NoRequiredField(descriptor, "change_version", "UINT32"),
          NoRequiredField(descriptor, "action", "vertis.holocron.Action"),
          NoRequiredField(descriptor, "event_timestamp", "google.protobuf.Timestamp")
        )
      )
    }
    "fail on illegal config" in ioTest {
      val descriptor = IllegalConfigHolocronMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          IllegalPartitioning(descriptor, Partitioning.BY_MONTH, "holocron"),
          IllegalHolocronPath(descriptor, "anubis/holocron-illegal-config/events", "anubis/events")
        )
      )
    }
  }
}

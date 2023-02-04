package vertis.anubis.api.test

import vertis.anubis.api.services.validate.errors.PalmaValidationError.RenamedDictionaryError
import vertis.anubis.api.services.validate.validators.palma.PalmaGlobalValidator
import vertis.anubis.api.test
import vertis.anubis.api.test.evolution.EvolutionValidatorSpecBase.MadWorld
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class PalmaGlobalValidatorSpec extends ZioSpecBase with ValidationTestSupport {

  "fail on dictionary renaming" in ioTest {
    val descriptor = test.RenamedDictionary.getDescriptor
    val masterDescriptor = test.NewDictionary.getDescriptor
    val world = new MadWorld(Map(descriptor -> masterDescriptor))
    val validator = new PalmaGlobalValidator(world)
    checkFail(
      validator,
      descriptor,
      List(RenamedDictionaryError(descriptor, "test/new_dictionary", "test/renamed_dictionary"))
    )
  }
}

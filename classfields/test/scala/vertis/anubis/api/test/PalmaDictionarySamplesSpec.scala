package vertis.anubis.api.test

import vertis.anubis.api.test
import ru.yandex.vertis.palma.{Mark, PalmaOptions}
import vertis.anubis.api.services.validate.World
import vertis.anubis.api.services.validate.errors.PalmaValidationError._
import vertis.anubis.api.services.validate.validators.CompositeValidator
import vertis.anubis.api.services.validate.validators.palma.{PalmaDictionaryValidator, PalmaGlobalValidator}
import vertis.zio.test.ZioSpecBase
import zio.ZIO

/** @author ruslansd
  */
class PalmaDictionarySamplesSpec extends ZioSpecBase with ValidationTestSupport {

  override val world: World = createWorld(Seq(Mark.getDescriptor), fds(Seq(Mark.getDescriptor)))

  private val palmaDescriptors = world.getDescriptorsByOption(PalmaOptions.message)

  private val validator = {
    val global = new PalmaGlobalValidator(world)
    val dictionaryValidator = new PalmaDictionaryValidator
    new CompositeValidator(List(global, dictionaryValidator))
  }

  "PalmaValidator" should {
    "validate all samples" in ioTest {
      ZIO.foreach(palmaDescriptors) { d =>
        checkSucceed(validator, d)
      }
    }

    "new dictionary" in ioTest {
      val descriptor = test.NewDictionary.getDescriptor
      val newWorld = createWorld(Seq(descriptor))
      val v = new PalmaGlobalValidator(newWorld)
      checkFail(
        v,
        descriptor,
        List(NewDictionaryValidation(descriptor))
      )
    }
  }
}

package vertis.anubis.api.test.evolution

import vertis.anubis.api.services.validate.errors.PalmaValidationError._
import vertis.anubis.api.services.validate.validators.palma.PalmaOptionsEvolutionValidator
import vertis.anubis.api.test._
import vertis.anubis.api.test.evolution.EvolutionValidatorSpecBase.MadWorld
import vertis.zio.test.ZioSpecBase

/** @author ruslansd
  */
class PalmaOptionsEvolutionValidatorSpec extends ZioSpecBase with EvolutionValidatorSpecBase {

  private val mappings = Map(
    DeleteLinkAnnotation.getDescriptor -> SimpleDictionary.getDescriptor,
    MakeEncrypted.getDescriptor -> SimpleDictionary.getDescriptor,
    AddIndexNewField.getDescriptor -> SimpleDictionary.getDescriptor,
    AddIndexOldField.getDescriptor -> SimpleDictionary.getDescriptor,
    AddTupleIndexOldField.getDescriptor -> SimpleDictionary.getDescriptor,
    ChangeKey.getDescriptor -> SimpleDictionary.getDescriptor,
    AddNewLink.getDescriptor -> SimpleDictionary.getDescriptor
  )

  override protected val validator = new PalmaOptionsEvolutionValidator(new MadWorld(mappings))

  "PalmaOptionsEvolutionValidator" should {
    "fail on delete link annotation" in {
      testEvolution(
        DeleteLinkAnnotation.getDescriptor,
        List(
          ChangedLinkOption(SimpleDictionary.getDescriptor, SimpleDictionary.getDescriptor.findFieldByName("link"))
        )
      )
    }

    "accept new link creation" in {
      testEvolution(
        AddNewLink.getDescriptor,
        List.empty
      )
    }

    "fail on change encrypted annotation" in {
      testEvolution(
        MakeEncrypted.getDescriptor,
        List(
          ChangedEncryption(SimpleDictionary.getDescriptor)
        )
      )
    }
    "not fail on new index on new field" in {
      testEvolution(
        AddIndexNewField.getDescriptor,
        List.empty
      )
    }

    "fail on new index on old field" in {
      testEvolution(
        AddIndexOldField.getDescriptor,
        List(ChangedIndexes(SimpleDictionary.getDescriptor))
      )
    }

    "fail on new tupled index on old field" in {
      testEvolution(
        AddTupleIndexOldField.getDescriptor,
        List(ChangedIndexes(SimpleDictionary.getDescriptor))
      )
    }

    "fail on key field change" in {
      testEvolution(
        ChangeKey.getDescriptor,
        List(
          ChangedKeyOption(SimpleDictionary.getDescriptor, SimpleDictionary.getDescriptor.findFieldByName("code"))
        )
      )
    }
  }
}

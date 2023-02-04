package vertis.anubis.api.test

import vertis.anubis.api.services.validate.World
import vertis.anubis.api.services.validate.errors.PalmaValidationError._
import vertis.anubis.api.services.validate.validators.palma.PalmaReader.{Index, IndexField}
import vertis.anubis.api.services.validate.validators.palma.{PalmaDictionaryValidator, PalmaGlobalValidator}
import vertis.anubis.api.test
import vertis.zio.test.ZioSpecBase
import zio.ZIO

/** @author ruslansd
  */
class PalmaDictionaryValidatorSpec extends ZioSpecBase with ValidationTestSupport {

  private val validator = new PalmaDictionaryValidator

  override val world: World = createWorld(Seq(test.UndefinedKeyField.getDescriptor))

  "PalmaDictionaryValidator" should {
    "fail on undefined key" in ioTest {
      checkFail(
        validator,
        test.UndefinedKeyField.getDescriptor,
        List(UndefinedKeyField(test.UndefinedKeyField.getDescriptor))
      )
    }

    "fail on unexpected key field type" in ioTest {
      checkFail(
        validator,
        test.UnexpectedKeyFieldType.getDescriptor,
        List(UnexpectedKeyFieldType(test.UnexpectedKeyFieldType.getDescriptor))
      )
    }

    "fail on empty dictionary name" in ioTest {
      checkFail(
        validator,
        test.EmptyDictionaryName.getDescriptor,
        List(
          EmptyDictionaryName(test.EmptyDictionaryName.getDescriptor),
          UndefinedKeyField(test.EmptyDictionaryName.getDescriptor)
        )
      )
    }
    "fail on invalid link annotation" in ioTest {
      checkFail(
        validator,
        test.IllegalLinkAnnotation.getDescriptor,
        List(
          IllegalLinkAnnotation(
            test.IllegalLinkAnnotation.getDescriptor,
            test.IllegalLinkAnnotation.getDescriptor.findFieldByName("invalid_link")
          ),
          IllegalLinkAnnotation(
            test.IllegalLinkAnnotation.getDescriptor,
            test.IllegalLinkAnnotation.getDescriptor.findFieldByName("invalid_link_int")
          )
        )
      )
    }

    "fail on invalid link annotation in message container" in {
      checkFail(
        validator,
        test.IllegalLinkContainerAnnotation.getDescriptor,
        List(
          IllegalLinkAnnotation(
            test.InvalidLinkContainer.getDescriptor,
            test.InvalidLinkContainer.getDescriptor.findFieldByName("invalid_link")
          )
        )
      )
    }

    "fail on expected link annotation" in ioTest {
      checkFail(
        validator,
        test.ExpectedLinkAnnotation.getDescriptor,
        List(
          ExpectedLinkAnnotation(
            test.ExpectedLinkAnnotation.getDescriptor,
            test.ExpectedLinkAnnotation.getDescriptor.findFieldByName("dictionary")
          )
        )
      )
    }

    "fail on missing index field" in ioTest {
      checkFail(
        validator,
        test.MissingIndexField.getDescriptor,
        List(
          MissingIndexField(test.MissingIndexField.getDescriptor, "russian_alias")
        )
      )
    }

    "fail on unsupported index field type" in ioTest {
      checkFail(
        validator,
        test.UnsupportedIndexFieldType.getDescriptor,
        List(
          UnsupportedIndexFieldType(
            test.UnsupportedIndexFieldType.getDescriptor,
            test.UnsupportedIndexFieldType.getDescriptor.findFieldByName("index")
          )
        )
      )
    }

    "fail on index tuples unsupported" in ioTest {
      checkFail(
        validator,
        test.IndexTuplesUnsupported.getDescriptor,
        List(
          IndexTuplesUnsupported(
            test.IndexTuplesUnsupported.getDescriptor,
            Index(
              Seq(
                IndexField(
                  "index",
                  Some(test.IndexTuplesUnsupported.getDescriptor.findFieldByName("index"))
                ),
                IndexField(
                  "index1",
                  Some(test.IndexTuplesUnsupported.getDescriptor.findFieldByName("index1"))
                )
              )
            )
          )
        )
      )
    }

    "fail on nested dictionaries" in ioTest {
      checkFail(
        validator,
        test.HasNestedDictionary.NestedDictionary.getDescriptor,
        List(
          NestedDictionaries(
            test.HasNestedDictionary.NestedDictionary.getDescriptor
          )
        )
      )
    }

    "fail on duplicate dictionary name" in ioTest {
      checkFail(
        new PalmaGlobalValidator(world),
        test.DuplicateValidDictionary.getDescriptor,
        List(
          DuplicateDictionary(test.DuplicateValidDictionary.getDescriptor),
          NewDictionaryValidation(test.DuplicateValidDictionary.getDescriptor)
        )
      )
    }

    "success on valid dictionaries" in ioTest {
      val descriptors = Seq(test.HasNestedDictionary.getDescriptor, test.ValidDictionary.getDescriptor)

      ZIO.foreach(descriptors) { d =>
        checkSucceed(validator, d)
      }
    }
  }

}

package vertis.palma.service

import com.google.protobuf.Message
import vertis.palma.dao.model.DictionaryItem.Id
import vertis.palma.dao.model.{Index, Relation}
import vertis.palma.service.DynamicMessageParser._
import vertis.palma.service.model.DictionaryException
import vertis.palma.service.model.DictionaryException._
import vertis.zio.test.ZioSpecBase
import zio.IO

/** Spec on [[DynamicMessageParser]].
  *
  * @author ruslansd
  */
class DynamicMessageParserSpec extends ZioSpecBase with DictionaryBaseSpec {

  private def parse(msg: Message): IO[DictionaryException, ParsedItem] = {
    DynamicMessageParser.parseDictionaryItem(
      toDynamicMessage(msg)
    )
  }

  "DynamicMessageParser" should {
    "correctly parse dictionary message without relation" in {
      ioTest {
        parse(black).map { item =>
          item.relations shouldBe empty
          item.id.key shouldBe black.getCode
          item.id.dictionaryId shouldBe "test/color"
        }
      }
    }

    "correctly parse dictionary message with relation" in {
      ioTest {
        for {
          item <- parse(model)
          _ <- check {
            val expectedRelations = Seq(
              Relation(Id("test/color", black.getCode), "colors"),
              Relation(Id("test/color", red.getCode), "colors"),
              Relation(Id("test/auto/mark", mark.getCode), "mark")
            )
            item.relations should contain theSameElementsAs expectedRelations
            item.id.key shouldBe model.getCode
            item.id.dictionaryId shouldBe "test/auto/mark/model"
            item.payload shouldBe modelCleaned
          }
        } yield ()
      }
    }

    "fail on parse message with empty dictionary key" in {
      intercept[MissingKeyField] {
        ioTest {
          parse(WithEmptyDictionaryKey)
        }
      }
    }

    "fail on parse message with relation on dictionary with empty key" in {
      intercept[MissingKeyField] {
        ioTest {
          parse(RelatedToEmptyDictionaryKey)
        }
      }
    }

    "skip empty relation" in {
      ioTest {
        for {
          item <- parse(WithEmptyLink)
          _ <- check {
            item.relations shouldBe empty
          }
        } yield ()
      }
    }

    "fail on parse message without dictionary name" in {
      intercept[MissingDictionaryOptions] {
        ioTest {
          parse(DictionaryWithoutNameOption)
        }
      }
    }

    "parse nested relation" in {
      ioTest {
        parse(modelWithNestedLink).map { item =>
          val expectedRelations = Seq(
            Relation(Id("test/color", black.getCode), "colors.color"),
            Relation(Id("test/color", red.getCode), "colors.color"),
            Relation(Id("test/auto/mark", mark.getCode), "mark"),
            Relation(Id("test/auto/model/option", Option1.getCode), "package.links.option"),
            Relation(Id("test/auto/model/option", Option2.getCode), "package.links.option"),
            Relation(Id("test/color", black.getCode), "package.color")
          )
          item.relations should contain theSameElementsAs expectedRelations
          item.id.key shouldBe model.getCode
          item.id.dictionaryId shouldBe "test/auto/mark/model_nested_link"
          item.payload shouldBe modelWithNestedLinkCleaned
        }
      }
    }

    "not parse links of nested dictionary" in {
      ioTest {
        parse(foo).map { item =>
          val expectedRelations = Seq(
            Relation(Id("test/bar", Bar2.getCode), "bar_link")
          )
          item.relations should contain theSameElementsAs expectedRelations
          item.id.key shouldBe foo.getCode
          item.id.dictionaryId shouldBe "test/foo"
          item.payload shouldBe fooCleaned
        }
      }
    }

    "fail on parse message with nested link on dictionary with empty key" in {
      intercept[MissingKeyField] {
        ioTest {
          parse(modelMissingOptionCode)
        }
      }
    }

    "fail on parse message with explicitly empty nested link" in {
      intercept[MissingKeyField] {
        ioTest {
          parse(modelWithEmptyOption)
        }
      }
    }

    "skip empty nested link" in {
      ioTest {
        for {
          item <- parse(modelWithUnsetOption)
          _ <- check {
            val expected = Set(
              Relation(Id("test/auto/mark", "BMW"), "mark"),
              Relation(Id("test/auto/model/option", "option_1"), "package.links.option"),
              Relation(Id("test/auto/model/option", "option_2"), "package.links.option"),
              Relation(Id("test/color", "x_black"), "colors.color"),
              Relation(Id("test/color", "x_red"), "colors.color"),
              Relation(Id("test/color", "x_black"), "package.color")
            )
            item.relations should contain theSameElementsAs expected
          }
        } yield ()
      }
    }

    "correctly parse dictionary message without indexes" in {
      ioTest {
        for {
          item <- parse(mark)
          _ <- check("indexes")(item.indexes shouldBe empty)
          _ <- check("key")(item.id.key shouldBe mark.getCode)
          _ <- check("name")(item.id.dictionaryId shouldBe "test/auto/mark")
        } yield ()
      }
    }

    "correctly parse single-field index" in {
      ioTest {
        for {
          item <- parse(black)
          _ <- check("relations")(item.relations shouldBe empty)
          _ <- check("indexes")(item.indexes shouldBe Seq(Index("alias", black.getAlias)))
          _ <- check("key")(item.id.key shouldBe black.getCode)
          _ <- check("name")(item.id.dictionaryId shouldBe "test/color")
        } yield ()
      }
    }

    "fail on missing index field" in {
      intercept[MissingIndexField] {
        ioTest {
          parse(brokenIndex)
        }
      }
    }

    "fail on multi-field index" in {
      intercept[UnsupportedFeature] {
        ioTest {
          parse(multiIndex)
        }
      }
    }

    "generate key if undefined" in {
      ioTest {
        for {
          item <- parse(withUndefinedKey)
          _ = item.id.key should not be empty
          parsedModel = parse(toDynamicMessage(item.payload), withUndefinedKey)
          _ = parsedModel.getCode should not be empty
        } yield ()
      }
    }

    "not generate key if key defined" in {
      ioTest {
        for {
          item <- parse(withDefinedKey)
          _ = item.id.key shouldBe withDefinedKey.getCode
          parsedModel = parse(toDynamicMessage(item.payload), withDefinedKey)
          _ = parsedModel.getCode shouldBe withDefinedKey.getCode
        } yield ()
      }
    }

    "fail on non string key field" in {
      intercept[UnexpectedFieldType] {
        ioTest {
          parse(intKey)
        }
      }
    }
  }
}

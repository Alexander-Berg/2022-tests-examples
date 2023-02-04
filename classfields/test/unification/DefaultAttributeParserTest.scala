package ru.yandex.vertis.general.feed.processor.pipeline.test.unification

import general.feed.transformer.{RawAttribute, RawCategory, RawCondition, RawOffer}
import general.bonsai.attribute_model.{
  AttributeDefinition => BonsaiAttributeDefinition,
  BooleanSettings,
  DictionarySettings,
  NumberSettings,
  StringSettings
}
import ru.yandex.vertis.general.feed.processor.dictionary.BonsaiDictionaryService
import ru.yandex.vertis.general.feed.processor.dictionary.testkit.BonsaiDictionaryTestService
import ru.yandex.vertis.general.feed.processor.model.Category.CategoryAttribute
import ru.yandex.vertis.general.feed.processor.model.{Attribute => ParsedAttribute, _}
import ru.yandex.vertis.general.feed.processor.pipeline.unification.AttributeParser
import zio.{Has, Ref, ULayer}
import zio.test._
import zio.test.Assertion._

object DefaultAttributeParserTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultAttributeParser")(
      testM("Парсит атрибуты по идентификатору") {
        val offer = offerWithAttributes(
          List(
            RawAttribute("attribute-num", "33.33"),
            RawAttribute("attribute-bool", "false"),
            RawAttribute("attribute-string", "testtest test"),
            RawAttribute("attribute-dict", "второе"),
            RawAttribute("attribute-rep-string", "one"),
            RawAttribute("attribute-rep-string", "two"),
            RawAttribute("attribute-rep-string", "three")
          )
        )
        val category = prepareCategory
        AttributeParser.parseAttributes(offer, category).map { attributes =>
          val expected = List(
            ParsedAttribute(numberAttributeDefinition, NumberValue(33.33)),
            ParsedAttribute(booleanAttributeDefinition, BooleanValue(false)),
            ParsedAttribute(stringAttributeDefinition, StringValue("testtest test")),
            ParsedAttribute(repeatedDictionaryAttributeDefinition, RepeatedDictionaryValue(Set("v2"))),
            ParsedAttribute(repeatedStringAttributeDefinition, RepeatedStringValue(Set("one", "two", "three")))
          )
          assert(attributes.field)(hasSameElements(expected)) && assert(attributes.errors)(isEmpty)
        }
      },
      testM("Парсит атрибуты по уникальному имени") {
        val offer = offerWithAttributes(
          List(
            RawAttribute("number attribute", "33.33"),
            RawAttribute("boolean attribute", "false"),
            RawAttribute("string attribute", "testtest test"),
            RawAttribute("dictionary attribute", "второе"),
            RawAttribute("attribute-rep-string", "one"),
            RawAttribute("attribute-rep-string", "two"),
            RawAttribute("attribute-rep-string", "three")
          )
        )
        val category = prepareCategory
        AttributeParser.parseAttributes(offer, category).map { attributes =>
          val expected = List(
            ParsedAttribute(numberAttributeDefinition, NumberValue(33.33)),
            ParsedAttribute(booleanAttributeDefinition, BooleanValue(false)),
            ParsedAttribute(stringAttributeDefinition, StringValue("testtest test")),
            ParsedAttribute(repeatedDictionaryAttributeDefinition, RepeatedDictionaryValue(Set("v2"))),
            ParsedAttribute(repeatedStringAttributeDefinition, RepeatedStringValue(Set("one", "two", "three")))
          )
          assert(attributes.field)(hasSameElements(expected)) && assert(attributes.errors)(isEmpty)
        }
      },
      testM("Парсит атрибуты по синонимам") {
        val offer = offerWithAttributes(
          List(
            RawAttribute("how-many", "33.33"),
            RawAttribute("is-it", "false"),
            RawAttribute("line", "testtest test"),
            RawAttribute("one-of", "второе"),
            RawAttribute("attribute-rep-string", "one"),
            RawAttribute("attribute-rep-string", "two"),
            RawAttribute("attribute-rep-string", "three")
          )
        )
        val category = prepareCategory
        AttributeParser.parseAttributes(offer, category).map { attributes =>
          val expected = List(
            ParsedAttribute(numberAttributeDefinition, NumberValue(33.33)),
            ParsedAttribute(booleanAttributeDefinition, BooleanValue(false)),
            ParsedAttribute(stringAttributeDefinition, StringValue("testtest test")),
            ParsedAttribute(repeatedDictionaryAttributeDefinition, RepeatedDictionaryValue(Set("v2"))),
            ParsedAttribute(repeatedStringAttributeDefinition, RepeatedStringValue(Set("one", "two", "three")))
          )
          assert(attributes.field)(hasSameElements(expected)) && assert(attributes.errors)(isEmpty)
        }
      },
      testM("Парсит атрибуты частично") {
        val offer = offerWithAttributes(
          List(
            RawAttribute("how-many", "DO_NOT_PARSE1"),
            RawAttribute("is-it", "DO_NOT_PARSE2"),
            RawAttribute("line", "testtest test"),
            RawAttribute("line", "another test"),
            RawAttribute("one-of", "первое"),
            RawAttribute("one-of", "DO_NOT_PARSE3"),
            RawAttribute("one-of", "второе"),
            RawAttribute("one-of", "DO_NOT_PARSE4"),
            RawAttribute("attribute-rep-string", "one"),
            RawAttribute("attribute-rep-string", "two"),
            RawAttribute("attribute-rep-string", "three"),
            RawAttribute("DOES_NOT_EXIST", "a")
          )
        )
        val category = prepareCategory
        AttributeParser.parseAttributes(offer, category).map { attributes =>
          val expectedAttributes = List(
            ParsedAttribute(repeatedDictionaryAttributeDefinition, RepeatedDictionaryValue(Set("v1", "v2"))),
            ParsedAttribute(repeatedStringAttributeDefinition, RepeatedStringValue(Set("one", "two", "three")))
          )
          val expectedErrors = List(
            AttributeNotFound("DOES_NOT_EXIST"),
            MultipleValuesForSimpleAttribute(stringAttributeDefinition.name),
            InvalidAttributeValue("how-many", "DO_NOT_PARSE1", "Число"),
            InvalidAttributeValue("is-it", "DO_NOT_PARSE2", "Булевый флаг"),
            InvalidAttributeValue("one-of", "DO_NOT_PARSE3", "Словарь (допустимые значения: один, два)"),
            InvalidAttributeValue("one-of", "DO_NOT_PARSE4", "Словарь (допустимые значения: один, два)")
          )

          assert(attributes.field)(hasSameElements(expectedAttributes)) &&
          assert(attributes.errors)(hasSameElements(expectedErrors))
        }
      },
      testM("Не парсить скрытые атрибуты") {
        val offer = offerWithAttributes(
          List(
            RawAttribute("hidden-attribute-dict", "wrong value!")
          )
        ).copy(title = "спрятано")
        val category = prepareCategory
        AttributeParser.parseAttributes(offer, category).map { parsed =>
          assert(parsed.errors)(isEmpty) &&
          assert(parsed.field)(isEmpty)
        }
      },
      testM("Парсить атрибуты по описанию и заголовку") {
        val offer = offerWithAttributes(Nil).copy(
          title = "УтвеРждеНие",
          description = "ПерВое втоРое третье четвЁртоЕ"
        )
        val category = prepareCategory
        val expected = List(
          ParsedAttribute(booleanAttributeDefinition, BooleanValue(true)),
          ParsedAttribute(repeatedDictionaryAttributeDefinition, RepeatedDictionaryValue(Set("v1", "v2")))
        )
        AttributeParser.parseAttributes(offer, category).map { parsed =>
          assert(parsed.field)(hasSameElements(expected))
        }
      },
      testM("Предпочитать значение из заголовка если нашлось несколько значений") {
        val parsableOffer = offerWithAttributes(Nil).copy(
          title = "десятое",
          description = "шестое восьмое"
        )
        val conflictedOffer = offerWithAttributes(Nil).copy(
          title = "Шестое восьмое",
          description = "десятое"
        )
        val category = prepareCategory
        val expectedSuccess = List(
          ParsedAttribute(regularDictionaryAttributeDefinition, DictionaryValue("v10"))
        )
        for {
          success <- AttributeParser.parseAttributes(parsableOffer, category)
          notFound <- AttributeParser.parseAttributes(conflictedOffer, category)
        } yield assert(success.field)(hasSameElements(expectedSuccess)) &&
          assert(notFound.field)(isEmpty)
      }
    )
  }.provideCustomLayerShared {
    prepareDictionary() >>> AttributeParser.live
  }

  val booleanAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-bool",
    version = 3,
    name = "утверждение",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.BooleanSettings(BooleanSettings())
  )

  val numberAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-num",
    version = 1,
    name = "количество",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.NumberSettings(NumberSettings())
  )

  val stringAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-string",
    version = 1,
    name = "строка",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.StringSettings(StringSettings())
  )

  val repeatedStringAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-rep-string",
    version = 2,
    name = "Строки",
    isRepeated = true,
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.StringSettings(StringSettings())
  )

  val repeatedDictionaryAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-dict",
    version = 1,
    isRepeated = true,
    name = "выберите",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.DictionarySettings(
      DictionarySettings(
        Seq(
          DictionarySettings.DictionaryValue("v1", "один", synonyms = Seq("первое")),
          DictionarySettings.DictionaryValue("v2", "два", synonyms = Seq("второе"))
        )
      )
    )
  )

  val regularDictionaryAttributeDefinition = BonsaiAttributeDefinition(
    id = "attribute-dict-unrepeated",
    version = 1,
    isRepeated = false,
    name = "выберите одно",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.DictionarySettings(
      DictionarySettings(
        Seq(
          DictionarySettings.DictionaryValue("v6", "шесть", synonyms = Seq("шестое")),
          DictionarySettings.DictionaryValue("v8", "восемь", synonyms = Seq("восьмое")),
          DictionarySettings.DictionaryValue("v10", "десять", synonyms = Seq("десятое"))
        )
      )
    )
  )

  val hiddenDictionaryAttributeDefinition = BonsaiAttributeDefinition(
    id = "hidden-attribute-dict",
    version = 4,
    name = "dict",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.DictionarySettings(
      DictionarySettings(
        Seq(
          DictionarySettings.DictionaryValue("key", "name", synonyms = Seq("value"))
        )
      )
    )
  )

  val oneMoreHiddenAttributeDefinition = BonsaiAttributeDefinition(
    id = "one-more-hidden",
    version = 4,
    name = "спрятано",
    attributeSettings = BonsaiAttributeDefinition.AttributeSettings.DictionarySettings(
      DictionarySettings(
        Seq(
          DictionarySettings.DictionaryValue("hidden", "спрятано", synonyms = Seq("скрыто"))
        )
      )
    )
  )

  private def prepareDictionary(): ULayer[Has[Ref[BonsaiDictionaryService.Service]]] = {
    Ref.make {
      val service: BonsaiDictionaryService.Service = new BonsaiDictionaryTestService(
        Map.empty,
        Map.empty,
        Map(
          "how-many" -> Seq(
            BonsaiAttributeDefinition(
              id = "attribute-num-other-category",
              name = "число",
              version = 0,
              attributeSettings = BonsaiAttributeDefinition.AttributeSettings.NumberSettings(NumberSettings())
            ),
            numberAttributeDefinition
          ),
          "is-it" -> Seq(booleanAttributeDefinition),
          "line" -> Seq(stringAttributeDefinition),
          "one-of" -> Seq(repeatedDictionaryAttributeDefinition),
          "записи" -> Seq(repeatedStringAttributeDefinition)
        ),
        Map(
          "attribute-num" -> numberAttributeDefinition,
          "attribute-bool" -> booleanAttributeDefinition,
          "attribute-string" -> stringAttributeDefinition,
          "attribute-dict" -> repeatedDictionaryAttributeDefinition,
          "attribute-dict-unrepeated" -> regularDictionaryAttributeDefinition,
          "attribute-rep-string" -> repeatedStringAttributeDefinition,
          "hidden-attribute-dict" -> hiddenDictionaryAttributeDefinition,
          "one-more-hidden" -> oneMoreHiddenAttributeDefinition
        ),
        Map(
          "number attribute" -> numberAttributeDefinition,
          "boolean attribute" -> booleanAttributeDefinition,
          "string attribute" -> stringAttributeDefinition,
          "dictionary attribute" -> repeatedDictionaryAttributeDefinition,
          "unrepeated dictionary attribute" -> regularDictionaryAttributeDefinition,
          "repeated string attribute" -> repeatedStringAttributeDefinition
        ),
        Map(
          "attribute-dict" -> Map(
            "первое" -> AttributeDictionaryValue("v1", "один"),
            "второе" -> AttributeDictionaryValue("v2", "два")
          ),
          "attribute-dict-unrepeated" -> Map(
            "первое" -> AttributeDictionaryValue("v1", "один"),
            "второе" -> AttributeDictionaryValue("v2", "два"),
            "шестое" -> AttributeDictionaryValue("v6", "шесть"),
            "восьмое" -> AttributeDictionaryValue("v8", "восемь")
          ),
          "hidden-attribute-dict" -> Map(
            "value" -> AttributeDictionaryValue("key", "name")
          ),
          "one-more-hidden" -> Map(
            "скрыто" -> AttributeDictionaryValue("hidden", "спрятано")
          )
        )
      )
      service
    }.toLayer
  }

  private def offerWithAttributes(attributes: List[RawAttribute]): RawOffer =
    RawOffer(
      externalId = "offer-1",
      title = "Test offer",
      description = "продаю мопед (не мой)",
      condition = RawCondition.Condition.USED,
      category = Some(RawCategory("category-1")),
      attributes = attributes
    )

  private def prepareCategory: Category =
    Category(
      id = "category-1",
      "some-category-name",
      version = 0L,
      attributes = Set(
        CategoryAttribute("attribute-bool", false),
        CategoryAttribute("attribute-num", false),
        CategoryAttribute("attribute-string", false),
        CategoryAttribute("attribute-dict", false),
        CategoryAttribute("attribute-rep-string", false),
        CategoryAttribute("hidden-attribute-dict", true),
        CategoryAttribute("one-more-hidden", true),
        CategoryAttribute("attribute-dict-unrepeated", false)
      ).map(attr => attr.id -> attr).toMap,
      false,
      true,
      true
    )
}

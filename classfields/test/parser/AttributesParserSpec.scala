package ru.yandex.vertis.general.wizard.meta.parser

import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings}
import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.model.RequestMatch.AttributeValue
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, RequestMatch}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object AttributesParserSpec extends DefaultRunnableSpec {

  private val soft = DictionaryValue(key = "soft", synonyms = Seq("мягкие"))
  private val materialDict = DictionarySettings(allowedValues = Seq(soft))

  private val material =
    AttributeDefinition(id = "material", synonyms = Seq("материал")).withDictionarySettings(materialDict)

  private val siberian = DictionaryValue(key = "siberian", synonyms = Seq("сибирские"))
  private val breedDict = DictionarySettings(allowedValues = Seq(siberian))
  private val breeds = AttributeDefinition(id = "breed", synonyms = Seq("порода")).withDictionarySettings(breedDict)
  private val breedsDescription = CategoryAttribute("breed")
  private val cats = Category(id = "cat", synonyms = Seq("кошки"), attributes = Seq(breedsDescription))

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(material)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(breeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(cats))
  )

  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)
  private val intentions = Seq.empty

  private val intentionSnapshot = IntentionPragmaticsSnapshot(intentions)
  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val dictionaryService =
    new LiveDictionaryService(
      intentionSnapshot,
      TestUtils.EmptyMetaPragmaticsSnapshot,
      bonsaiSnapshot,
      catalogSynonymsMapping
    )
  private val bonsai = LiveBonsaiService.create(bonsaiSnapshot)

  private val attributesParser = new AttributesParser(dictionaryService)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AttributesParser")(
      testM("parse attributes") {
        for {
          parsed <- attributesParser.parse(
            AttributesParser.Input(
              PartialParsedQuery(
                MetaWizardRequest.empty("купить кошки сибирские мягкие"),
                Set.empty
              ),
              ad => Set("breed", "material")(ad.id)
            )
          )
        } yield assert(parsed.map(_.toSet).toSet)(
          equalTo(
            Set(
              Set(
                RequestMatch.Attribute
                  .userInputIndices(Set(2), "breed", 0L, AttributeValue.Dictionary("siberian")),
                RequestMatch.Attribute.userInputIndices(Set(3), "material", 0L, AttributeValue.Dictionary("soft"))
              )
            )
          )
        )
      },
      testM("parse attributes fit to parsed parts") {
        for {
          parsed <- attributesParser.parse(
            AttributesParser.Input(
              PartialParsedQuery(
                MetaWizardRequest.empty("купить кошки сибирские мягкие"),
                Set(RequestMatch.Category.userInputIndices(Set(2), "other", 0L))
              ),
              ad => Set("breed", "material")(ad.id)
            )
          )
        } yield assert(parsed.map(_.toSet).toSet)(
          equalTo(
            Set(
              Set(
                RequestMatch.Attribute.userInputIndices(Set(3), "material", 0L, AttributeValue.Dictionary("soft"))
              )
            )
          )
        )
      }
    )

}

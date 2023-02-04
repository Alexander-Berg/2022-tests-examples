package ru.yandex.vertis.general.wizard.meta.parser

import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings}
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.RequestMatch.AttributeValue
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, RequestMatch}
import zio.test.Assertion.equalTo
import zio.test._

object AutonomousAttributesParserSpec extends DefaultRunnableSpec {

  private val soft = DictionaryValue(key = "soft", synonyms = Seq("мягкие"))
  private val materialDict = DictionarySettings(allowedValues = Seq(soft))

  private val material =
    AttributeDefinition(id = "material", synonyms = Seq("материал"), isAutonomous = true)
      .withDictionarySettings(materialDict)
  private val materialDescription = CategoryAttribute("material")

  private val siberian = DictionaryValue(key = "siberian", synonyms = Seq("сибирские"))
  private val breedDict = DictionarySettings(allowedValues = Seq(siberian))

  private val breeds =
    AttributeDefinition(id = "breed", synonyms = Seq("порода"), isAutonomous = true).withDictionarySettings(breedDict)
  private val breedsDescription = CategoryAttribute("breed")

  private val hot = DictionaryValue(key = "hot", synonyms = Seq("теплые"))
  private val hotDict = DictionarySettings(allowedValues = Seq(hot))

  private val temperature = AttributeDefinition(id = "temperature", synonyms = Seq("температур"), isAutonomous = false)
    .withDictionarySettings(hotDict)
  private val temperatureDescription = CategoryAttribute("temperature")

  private val cats = Category(
    id = "cat",
    synonyms = Seq("кошки"),
    attributes = Seq(breedsDescription, materialDescription, temperatureDescription)
  )

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(material)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(breeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(temperature)),
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

  private val attributesParser = new AutonomousAttributesParser(dictionaryService)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AutonomousAttributesParser")(
      testM("parse attributes") {
        for {
          parsed <- attributesParser.parse(
            PartialParsedQuery(
              MetaWizardRequest.empty("купить кошки сибирские мягкие теплые"),
              Set.empty
            )
          )
        } yield assert(parsed.toSet)(
          equalTo(
            Set(
              RequestMatch.Attribute
                .userInputIndices(Set(2), "breed", 0L, AttributeValue.Dictionary("siberian")),
              RequestMatch.Attribute.userInputIndices(Set(3), "material", 0L, AttributeValue.Dictionary("soft"))
            )
          )
        )
      }
    )

}

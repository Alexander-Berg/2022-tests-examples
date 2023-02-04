package ru.yandex.vertis.general.wizard.meta.rules

import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings}
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.meta.parser.AutonomousAttributesParser
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.rules.impl.CategoryByAutonomousAttribute
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, ParseState}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object CategoryByAutonomousAttributeSpec extends DefaultRunnableSpec with MockitoSupport {

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

  private val cats =
    Category(id = "cat", synonyms = Seq("кошки"), attributes = Seq(breedsDescription, temperatureDescription))

  private val dogs =
    Category(id = "dog", synonyms = Seq("собаки"), attributes = Seq(materialDescription, temperatureDescription))

  private val pigs = Category(id = "pig", synonyms = Seq("свиньи"), attributes = Seq(temperatureDescription))

  private val parrots = Category(id = "parrot", synonyms = Seq("попугаи"), attributes = Seq.empty)

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(material)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(breeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(temperature)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(cats)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(dogs)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(pigs)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(parrots))
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

  private val bonsaiService = LiveBonsaiService.create(bonsaiSnapshot)

  private val autonomousAttributesParser = new AutonomousAttributesParser(dictionaryService)

  private val categoryEnricherNode = CategoryByAutonomousAttribute(autonomousAttributesParser, bonsaiService)

  private val state = ParseState.empty(MetaWizardRequest.empty("кошки мягкие сибирские теплые"))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryByAutonomousAttribute RuleNode")(
      testM("parse category by autonomous attribute") {
        for {
          parsedStates <- categoryEnricherNode.process(
            state
          )
        } yield assert(parsedStates.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set("cat", "dog")))
      }
    )
}

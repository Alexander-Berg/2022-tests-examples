package ru.yandex.vertis.general.wizard.meta.services

import common.geobase.Region
import common.geobase.model.RegionIds.RegionId
import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings}
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.{CategoryTagsService, RegionService}
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.rules.RulesFactory
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.service.{DictionaryService, MetaWizard}
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.RequestMatch.AttributeValue
import ru.yandex.vertis.general.wizard.model._
import zio.test.Assertion._
import zio.test._
import zio.{Task, ZIO, ZLayer}

object MetaWizardSpec extends DefaultRunnableSpec {

  private val instruments = Category(id = "instr", synonyms = Seq("инструмент"))
  private val musicInstruments = Category(id = "music", synonyms = Seq("музыкальный инструмент"))
  private val spruces = Category(id = "spruce", synonyms = Seq("елка", "елку"))
  private val brand = Pragmatic.Intention(IntentionType.Brand, "brand", Set("яндекс"))
  private val newIntention = Pragmatic.Intention(IntentionType.New, "new", Set("новый"))
  private val neutralIntention = Pragmatic.Intention(IntentionType.Neutral, "time", Set("время"))
  private val buyIntention = Pragmatic.Intention(IntentionType.Commercial, "buy", Set("купить"))
  private val rabotaFindIntention = Pragmatic.Intention(IntentionType.RabotaFind, "rabota-find", Set("вакансия"))
  private val rabotaPostIntention = Pragmatic.Intention(IntentionType.RabotaPost, "rabota-post", Set("резюме"))

  private val realty = Category(id = "nedvizhimost-1_1gVpN8", synonyms = Seq("недвижимость"))
  private val auto = Category(id = "avto_juPdCX", synonyms = Seq("машину"))

  private val vakansii = Category(id = "vakansii_XMAfWw", synonyms = Seq("вакансии"))
  private val mechanicVacancy = Category(id = "mechanic_vakansii", synonyms = Seq("механик"), parentId = vakansii.id)

  private val resume = Category(id = "rezume_EyUGxM", synonyms = Seq("резюме"))
  private val tutorResume = Category(id = "tutor_resume", synonyms = Seq("учитель"), parentId = resume.id)

  private val soft = DictionaryValue(key = "soft", synonyms = Seq("мягкую"))
  private val materialDict = DictionarySettings(allowedValues = Seq(soft))

  private val material =
    AttributeDefinition(id = "material", synonyms = Seq("материал")).withDictionarySettings(materialDict)

  private val siberian = DictionaryValue(key = "siberian", synonyms = Seq("сибирскую"))
  private val breedDict = DictionarySettings(allowedValues = Seq(siberian))
  private val breeds = AttributeDefinition(id = "breed", synonyms = Seq("порода")).withDictionarySettings(breedDict)
  private val breedsDescription = CategoryAttribute("breed")

  private val cats =
    Category(id = "cat", synonyms = Seq("кошку"), attributes = Seq(breedsDescription, CategoryAttribute("material")))

  private val commercialRequireCategoryId = "commercial-require-category"
  private val commercialRequireCategory = Category(id = commercialRequireCategoryId, synonyms = Seq("гусь", "гуся"))

  private val mockRegionService = new RegionService.Service {
    override def getRegion(regionId: RegionId): Task[Option[Region]] = ZIO.none

    override def getPathToRoot(regionId: RegionId): Task[Seq[Region]] = ZIO.succeed(Seq.empty)
  }

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(instruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(musicInstruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(spruces)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(realty)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(auto)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(material)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(breeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(cats)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(commercialRequireCategory)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(vakansii)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(mechanicVacancy)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(resume)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(tutorResume))
  )
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  private val intentions =
    Seq(brand, newIntention, buyIntention, rabotaFindIntention, neutralIntention, rabotaPostIntention)

  private val intentionSnapshot = IntentionPragmaticsSnapshot(intentions)

  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val dictionaryService: DictionaryService.Service =
    new LiveDictionaryService(
      intentionSnapshot,
      TestUtils.EmptyMetaPragmaticsSnapshot,
      bonsaiSnapshot,
      catalogSynonymsMapping
    )

  private val categoryTagsService = new CategoryTagsService.Service {

    override def hasTag(categoryId: CategoryId, categoryTag: CategoryTag): Task[Boolean] =
      Task.succeed(
        if (categoryId == commercialRequireCategoryId && categoryTag == CategoryTag.CommercialRequire)
          true
        else
          false
      )
  }

  private def isValid(wizardMatch: WizardMatch): Boolean =
    wizardMatch.parseStateInfo == ParseStateInfo.Valid

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("MetaWizard")(
      testM("parse category") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("время купить новый инструмент"))
        } yield assert(wizardMatches.filter(isValid))(
          equalTo(
            Seq(
              WizardMatch.Category(
                RequestMatch.Category.userInputIndices(Set(3), "instr", 0L),
                None,
                Set.empty,
                Set(
                  RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Neutral),
                  RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Commercial),
                  RequestMatch.Intention.userInputIndices(Set(2), IntentionType.New)
                ),
                ParseStateInfo.Valid
              )
            )
          )
        )
      },
      testM("parse category despite io usages") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить ёлку"))
        } yield assert(wizardMatches.filter(isValid))(
          equalTo(
            Seq(
              WizardMatch.Category(
                RequestMatch.Category.userInputIndices(Set(1), "spruce", 0L),
                None,
                Set.empty,
                Set(
                  RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Commercial)
                ),
                ParseStateInfo.Valid
              )
            )
          )
        )
      },
      testM("parse all categories") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("время купить новый музыкальный инструмент"))
        } yield assert(wizardMatches.filter(isValid))(
          equalTo(
            Seq(
              WizardMatch.Category(
                RequestMatch.Category.userInputIndices(Set(3, 4), "music", 0L),
                None,
                Set.empty,
                Set(
                  RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Neutral),
                  RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Commercial),
                  RequestMatch.Intention.userInputIndices(Set(2), IntentionType.New)
                ),
                ParseStateInfo.Valid
              )
            )
          )
        )
      },
      testM("parse intentions") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("время купить новый яндекс"))
        } yield assert(wizardMatches.filter(isValid))(
          equalTo(
            Seq(
              WizardMatch.Intentions(
                None,
                Set(
                  RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Neutral),
                  RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Commercial),
                  RequestMatch.Intention.userInputIndices(Set(2), IntentionType.New),
                  RequestMatch.Intention.userInputIndices(Set(3), IntentionType.Brand)
                ),
                ParseStateInfo.Valid
              )
            )
          )
        )
      },
      testM("parse category attributes") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить кошку сибирскую мягкую"))
        } yield assert {
          val filtered = wizardMatches.filter(isValid)
          filtered
        }(
          equalTo(
            Seq(
              WizardMatch.Category(
                RequestMatch.Category.userInputIndices(Set(1), "cat", 0L),
                None,
                Set(
                  RequestMatch.Attribute
                    .userInputIndices(Set(2), "breed", 0L, AttributeValue.Dictionary("siberian")),
                  RequestMatch.Attribute
                    .userInputIndices(Set(3), "material", 0L, AttributeValue.Dictionary("soft"))
                ),
                Set(RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Commercial)),
                ParseStateInfo.Valid
              )
            )
          )
        )
      },
      testM("don't parse attributes without category") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить сибирскую мягкую"))
        } yield assert(wizardMatches.filter(isValid))(isEmpty)
      },
      testM("Do not parse Realty categories") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить недвижимость"))
        } yield assert(wizardMatches.filter(isValid))(isEmpty)
      },
      testM("Do not parse Auto categories") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить машину"))
        } yield assert(wizardMatches.filter(isValid))(isEmpty)
      },
      testM("Do not parse commercial require category without Commercial intent") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("гусь"))
        } yield assert(wizardMatches.filter(isValid))(isEmpty)
      },
      testM("Parse commercial require category with Commercial intent") {
        for {
          wizardMatches <- MetaWizard.process(MetaWizardRequest.empty("купить гуся"))
        } yield assert(wizardMatches.filter(isValid))(isNonEmpty)
      },
      testM("Correctly parse rabota with key intentional") {
        for {
          vacancyMatches <- MetaWizard.process(MetaWizardRequest.empty("механик вакансия"))
          resumeMatches <- MetaWizard.process(MetaWizardRequest.empty("учитель резюме"))
        } yield assert(vacancyMatches.filter(isValid))(isNonEmpty) && assert(resumeMatches.filter(isValid))(isNonEmpty)
      },
      testM("Don't parse rabota without keys intentions") {
        for {
          vacancyMatches <- MetaWizard.process(MetaWizardRequest.empty("механик"))
          resumeMatches <- MetaWizard.process(MetaWizardRequest.empty("учитель"))
        } yield assert(vacancyMatches.filter(isValid))(isEmpty) && assert(resumeMatches.filter(isValid))(isEmpty)
      }
    )
  }.provideCustomLayer {
    val bonsaiService = ZLayer.succeed(LiveBonsaiService.create(bonsaiSnapshot))
    val rulesFactory = (ZLayer.succeed(mockRegionService) ++
      ZLayer.succeed(dictionaryService) ++
      bonsaiService ++
      ZLayer.succeed(categoryTagsService)) >>>
      RulesFactory.live
    rulesFactory >>> MetaWizard.live
  }

}

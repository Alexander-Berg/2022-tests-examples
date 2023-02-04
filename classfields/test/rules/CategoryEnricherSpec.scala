package ru.yandex.vertis.general.wizard.meta.rules

import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import org.mockito.invocation.InvocationOnMock
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.BonsaiService
import ru.yandex.vertis.general.wizard.meta.parser.CategoryParser
import ru.yandex.vertis.general.wizard.meta.rules.impl.CategoryEnricher
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.Experiment.WithoutCategory
import ru.yandex.vertis.general.wizard.model.{ClassifierResult, MetaWizardRequest, ParseState, RequestMatch}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.Task
import zio.test.Assertion._
import zio.test._

object CategoryEnricherSpec extends DefaultRunnableSpec with MockitoSupport {

  private val cats =
    Category(
      id = "cats",
      state = CategoryState.DEFAULT,
      symlinkToCategoryId = "",
      synonyms = Seq("коты")
    )

  private val pies =
    Category(
      id = "pies",
      state = CategoryState.DEFAULT,
      symlinkToCategoryId = "",
      synonyms = Seq("пирожки")
    )

  private val categories: Seq[Category] = Seq(cats, pies)

  private val bonsaiService: BonsaiService.Service = mock[BonsaiService.Service]

  private val snapshotSource: Seq[ExportedEntity] = Seq(ExportedEntity(ExportedEntity.CatalogEntity.Category(cats)))
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  when(bonsaiService.categoryById(?)).thenAnswer((invocationOnMock: InvocationOnMock) =>
    Task.succeed(categories.find(_.id == invocationOnMock.getArgument[String](0)))
  )

  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val dictionaryService =
    new LiveDictionaryService(
      TestUtils.EmptyIntentionsSnapshot,
      TestUtils.EmptyMetaPragmaticsSnapshot,
      bonsaiSnapshot,
      catalogSynonymsMapping
    )

  private val categoryParser = new CategoryParser(dictionaryService)

  private val categoryEnricherNode = CategoryEnricher(categoryParser, bonsaiService)

  private val badCatsClassifier = ClassifierResult("cats", "cats", 0.5f, 0.0f, 0.6f, true, None)
  private val piesClassifier = ClassifierResult("pies", "pies", 0.6f, 0.0f, 0.5f, true, None)
  private val goodCatsClassifier = ClassifierResult("cats", "cats", 0.6f, 0.0f, 0.5f, true, None)

  private val catsState = ParseState.empty(MetaWizardRequest.empty("коты").copy(experiments = Set(WithoutCategory)))

  private val catsPiesState =
    ParseState.empty(
      MetaWizardRequest
        .empty("собаки")
        .copy(classifiersResults = Set(badCatsClassifier), experiments = Set(WithoutCategory))
    )

  private val piesState =
    ParseState.empty(
      MetaWizardRequest.empty("коты").copy(classifiersResults = Set(piesClassifier), experiments = Set(WithoutCategory))
    )

  private val catsParsedState =
    ParseState.empty(
      MetaWizardRequest
        .empty("коты")
        .copy(classifiersResults = Set(goodCatsClassifier), experiments = Set(WithoutCategory))
    )

  private val piesStateWoExperiment =
    ParseState.empty(
      MetaWizardRequest.empty("коты").copy(classifiersResults = Set(piesClassifier))
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryEnricher RuleNode")(
      testM("parse category") {
        for {
          state <- categoryEnricherNode.process(
            catsState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set("cats")))
      },
      testM("not parse category by classifier without enough threshold") {
        for {
          state <- categoryEnricherNode.process(
            catsPiesState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set.empty[String]))
      },
      testM("parse category by classifier with enough threshold") {
        for {
          state <- categoryEnricherNode.process(
            piesState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set("cats", "pies")))
      },
      testM("not parse category by classifier with enough threshold without experiment") {
        for {
          state <- categoryEnricherNode.process(
            piesStateWoExperiment
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set("cats")))
      },
      testM("prefer by parsing") {
        for {
          state <- categoryEnricherNode.process(
            catsParsedState
          )
        } yield assert(
          state.flatMap(_.categoryMatch.map(_.source.isInstanceOf[RequestMatch.Source.UserRequestTokens])).toSet
        )(
          equalTo(Set(true))
        )
      }
    )
}

package ru.yandex.vertis.general.wizard.api.services

import common.geobase.model.RegionIds
import general.bonsai.category_model.Category
import ru.yandex.vertis.general.wizard.api.services.impl._
import ru.yandex.vertis.general.wizard.core.service.SettingsService
import ru.yandex.vertis.general.wizard.core.service.impl.{LiveBonsaiService, LiveTemplateService}
import ru.yandex.vertis.general.wizard.model.Experiment.CustomExperiment
import ru.yandex.vertis.general.wizard.model.Settings.{TemplateKey, TypeSettings}
import ru.yandex.vertis.general.wizard.model.{Experiment, Platform, Settings, Text, WizardEssentials}
import zio.Task
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object TemplateServiceSpec extends DefaultRunnableSpec {

  private val exp1: Experiment = CustomExperiment("exp1")
  private val exp2: Experiment = CustomExperiment("exp2")
  private val expCommonTitle: Experiment = CustomExperiment("expCommonTitle")

  private val DefaultTitle: String = "titleDef"
  private val DefaultDescription: String = "descrDef"
  private val DefaultGreenUrl: String = "gurlDef"

  private val Title1: String = "title1"
  private val Title2: String = "title2"
  private val CommonTitleForExp: String = "CommonTitleForExp"

  private def defaultTypeSetting = TypeSettings(
    titleTemplate = DefaultTitle,
    descriptionTemplate = DefaultDescription,
    greenUrlSecondPartTemplate = DefaultGreenUrl,
    titleMap = Map(),
    descriptionMap = Map(),
    greenUrlSecondPartMap = Map()
  )

  private def templateKey(category: Category = null, exp: Experiment = null) =
    TemplateKey(Option(exp), Option(category).map(_.id))

  private val settings = Settings(
    get = defaultTypeSetting
      .copy(
        titleMap = Map(
          templateKey(TestCatalog.musicInstruments, exp1) -> Title1,
          templateKey(TestCatalog.guitars, exp2) -> Title2
        )
      ),
    post = defaultTypeSetting
      .copy(
        titleMap = Map(
          templateKey(exp = expCommonTitle) -> CommonTitleForExp,
          templateKey(TestCatalog.musicInstruments, exp1) -> Title1,
          templateKey(TestCatalog.guitars) -> Title2
        )
      ),
    brand = defaultTypeSetting
      .copy(
        titleMap = Map(
          templateKey(exp = exp1) -> Title1
        )
      ),
    common = defaultTypeSetting
      .copy(
        titleMap = Map(
          templateKey(exp = exp1) -> Title1
        )
      )
  )

  private val bonsai = LiveBonsaiService.create(TestCatalog.bonsaiSnapshot)

  private val settingsService = new SettingsService.Service {
    override def get: Task[Settings] = Task.succeed(settings)
  }
  private val templateService = new LiveTemplateService(settingsService, bonsai)

  private def get(category: Category, experiments: Experiment*): WizardEssentials =
    WizardEssentials.Get(category, RegionIds.SaintPetersburg, Seq(), Seq(), experiments.toSet, Platform.Desktop)

  private def post(category: Category, experiment: Experiment*): WizardEssentials =
    WizardEssentials.Post(category, RegionIds.SaintPetersburg, Seq(), Platform.Desktop, experiment.toSet)

  private def brand(experiments: Experiment*): WizardEssentials =
    WizardEssentials.Brand(RegionIds.SaintPetersburg, Seq(), experiments.toSet, Platform.Desktop)

  private val Cases = Seq(
    get(TestCatalog.guitars, exp2) -> Text.Title -> Title2,
    get(TestCatalog.guitars) -> Text.Title -> DefaultTitle,
    get(TestCatalog.guitarString, exp1) -> Text.Title -> Title1,
    get(TestCatalog.guitarString, exp2) -> Text.Title -> Title2,
    get(TestCatalog.guitarString) -> Text.Title -> DefaultTitle,
    post(TestCatalog.guitars) -> Text.Title -> Title2,
    post(TestCatalog.guitars, expCommonTitle) -> Text.Title -> CommonTitleForExp,
    brand() -> Text.Title -> DefaultTitle,
    brand(exp1) -> Text.Title -> Title1
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val cases = Cases.zipWithIndex.map { case (((essential, text), expected), idx) =>
      testM(s"case #$idx") {
        for {
          res <- templateService.getTemplate(essential, text)
        } yield assert(res)(equalTo(expected))
      }
    }

    suite("TemplateService")(cases: _*)
  }
}

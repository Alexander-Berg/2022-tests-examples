package ru.yandex.auto.wizard.result.builders

import java.util.concurrent.ThreadLocalRandom

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.{Mark, Model}
import ru.yandex.auto.eds.wizard.magazine.{MagazineEntriesHolder, MagazineThemes, WizardMagazineEntriesProvider}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.sort.SortType
import ru.yandex.auto.wizard.MagazineEntry
import ru.yandex.auto.wizard.result.builders.thumbs.MagazineThumbBuilder
import ru.yandex.auto.wizard.result.data.construct.Thumb
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport._

import scala.util.Random

/**
  *
  * @author Rustam Guseyn-zade
  */
@RunWith(classOf[JUnitRunner])
class MagazineWizardBuilderSpec extends WordSpecLike with Matchers {

  val magazineProvider = mock[WizardMagazineEntriesProvider]

  val sc = mock[SearchConfiguration]

  val magazineWizardBuilder = new MagazineThumbBuilder(magazineProvider)

  "should provide journal thumb when magazine provider find appropriate MagazineEntry" in {
    val mark = "TESLA"
    val model = "CYBERTRUCK"
    val wantedMagazineEntry = magazineEntry(MagazineThemes.testDrive.toString, Some(mark), Some(model))
    val mockedModel = mock[Model]
    val mockedMark = mock[Mark]
    val foundData = mock[MagazineEntriesHolder]
    when(mockedMark.getMarkName).thenReturn(mark)
    when(mockedModel.getModelName).thenReturn(model)
    when(foundData.getEntries(MockitoSupport.?)).thenReturn(List(wantedMagazineEntry))
    when(magazineProvider.getData).thenReturn(foundData)
    when(sc.isMobileWizard).thenReturn(false)
    when(sc.getUtmCampaign).thenReturn("campaign")
    when(sc.getWizardSubtype).thenReturn("test-drive")

    magazineWizardBuilder.build(mockedMark, mockedModel, sc, WizardIntentionType.BLANK) should contain(
      new Thumb(
        wantedMagazineEntry.imgUrl,
        wantedMagazineEntry.title,
        wantedMagazineEntry.url + s"?from=wizard.test-drive&utm_source=auto_wizard&utm_medium=desktop&utm_campaign=campaign&utm_content=${WizardIntentionType.BLANK.getName}&sort_offers=${SortType.NO_SORTING.getParamSortName}&utm_term=test-drive",
        "",
        "JOURNAL_THUMB",
        2
      )
    )
  }

  def magazineEntry(theme: String, mark: Option[String], model: Option[String]): MagazineEntry =
    MagazineEntry(
      new Random().nextInt(),
      theme,
      "some title",
      "https://music.yandex.ru/home",
      new java.util.Date(ThreadLocalRandom.current().nextLong(1577826000000L, 1580418000000L)),
      "some description",
      "https://ya.ru/",
      mark,
      model
    )
}

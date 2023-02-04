package ru.yandex.auto.eds.wizard.magazine

import java.util.concurrent.ThreadLocalRandom

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.wizard.MagazineEntry

import scala.util.Random
import MagazineThemes._
import org.scalatestplus.junit.JUnitRunner

/**
  *
  * @author Rustam Guseyn-zade
  */
@RunWith(classOf[JUnitRunner])
class WizardMagazineEntriesProviderSpec extends WordSpecLike with Matchers {

  "should parse magazine entries without mark and model appropriately" in {
    val magazineEntryWithoutMarkAndModel = magazineEntry("some theme", None, None)
    val entriesHolder: MagazineEntriesHolder = MagazineEntriesHolder(List(magazineEntryWithoutMarkAndModel))

    holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder,
      Map("some theme" -> Map("None" -> Map("None" -> List(magazineEntryWithoutMarkAndModel))))
    )
  }

  "should put magazine entries with same mark and model in one leaf" in {
    val mark = "HYUNDAI"
    val model = "SOLARIS"
    val magazineEntry1 = magazineEntry("some theme", Some(mark), Some(model))
    val magazineEntry2 = magazineEntry("some theme", Some(mark), Some(model))
    val entriesHolder = MagazineEntriesHolder(List(magazineEntry1, magazineEntry2))

    holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder,
      Map("some theme" -> Map("HYUNDAI" -> Map("SOLARIS" -> List(magazineEntry1, magazineEntry2))))
    )
  }

  "should put magazine entries with different mark or model in different leafs" in {
    val magazineEntry1 = magazineEntry("some theme", Some("HYUNDAI"), Some("SOLARIS"))
    val magazineEntry2 = magazineEntry("some theme", Some("HYUNDAI"), Some("CRETA"))
    val magazineEntry3 = magazineEntry("some theme", Some("DEFINITELY NOT HYUNDAI"), Some("CRETA"))
    val entriesHolder: MagazineEntriesHolder =
      MagazineEntriesHolder(List(magazineEntry1, magazineEntry2, magazineEntry3))

    holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder,
      Map(
        "some theme" ->
          Map(
            "HYUNDAI" -> Map("SOLARIS" -> List(magazineEntry1), "CRETA" -> List(magazineEntry2)),
            "DEFINITELY NOT HYUNDAI" -> Map("CRETA" -> List(magazineEntry3))
          )
      )
    )
  }

  "should put magazine entries with different themes even if marks and models are same in different leafs" in {
    val mark = "HYUNDAI"
    val model = "SOLARIS"
    val magazineEntry1 = magazineEntry("theme1", Some(mark), Some(model))
    val magazineEntry2 = magazineEntry("theme2", Some(mark), Some(model))
    val entriesHolder = MagazineEntriesHolder(List(magazineEntry1, magazineEntry2))

    holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder,
      Map(
        "theme1" -> Map("HYUNDAI" -> Map("SOLARIS" -> List(magazineEntry1))),
        "theme2" -> Map("HYUNDAI" -> Map("SOLARIS" -> List(magazineEntry2)))
      )
    )
  }

  "should not add magazine entry in magazineEntriesHolder, if it contains corrupted url" in {
    val corruptedUrl = "biba"
    val magazineEntryWithCorrectUrls = magazineEntry("any theme", Some("TESLA"), Some("CYBERTRUCK"))
    val magazineEntryWithCorruptedUrl =
      magazineEntry("any theme", Some("TESLA"), Some("CYBERTRUCK")).copy(url = corruptedUrl)
    val magazineEntryWithCorruptedImageUrl =
      magazineEntry("any theme", Some("TESLA"), Some("CYBERTRUCK")).copy(imgUrl = corruptedUrl)

    MagazineEntriesHolder(
      List(magazineEntryWithCorrectUrls, magazineEntryWithCorruptedUrl, magazineEntryWithCorruptedImageUrl)
    ).getEntries() should contain theSameElementsAs List(magazineEntryWithCorrectUrls)
  }

  "should provide first met test-drive magazine entry on request" in {
    val mark = "TESLA"
    val model = "CYBERTRUCK"
    val magazineEntry1 = magazineEntry(testDrive.toString, Some(mark), Some(model))
    val magazineEntry2 = magazineEntry(testDrive.toString, Some(mark), Some(model))
    val magazineEntry3 = magazineEntry(testDrive.toString, Some("HYUNDAI"), Some("SOLARIS"))
    val magazineEntry4 = magazineEntry(testDrive.toString, Some("HYUNDAI"), None)
    val magazineEntry5 = magazineEntry(lists.toString, None, Some("SOLARIS"))
    val magazineEntry6 = magazineEntry(lists.toString, Some(mark), Some(model))
    val entriesHolder = MagazineEntriesHolder(
      List(magazineEntry1, magazineEntry2, magazineEntry3, magazineEntry4, magazineEntry5, magazineEntry6)
    )

    holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder,
      Map(
        testDrive.toString -> Map(
          mark -> Map(model -> List(magazineEntry1, magazineEntry2)),
          "HYUNDAI" -> Map("SOLARIS" -> List(magazineEntry3), GroupByModel.NoModel -> List(magazineEntry4))
        ),
        lists.toString -> Map(
          "None" -> Map("SOLARIS" -> List(magazineEntry5)),
          "TESLA" -> Map("CYBERTRUCK" -> List(magazineEntry6))
        )
      )
    )
    entriesHolder.getEntries(MagazineFilter(Some(testDrive), Some(mark.toUpperCase), Some(model.toUpperCase))) should contain(
      magazineEntry1
    )

  }

  def holderFromEntriesShouldBeEqualEntriesTree(
      entriesHolder: MagazineEntriesHolder,
      entriesTree: Map[String, Map[String, Map[String, List[MagazineEntry]]]]
  ): Unit = {
    entriesTree shouldBe getMagazineEntriesTree(
      new GroupByTheme(entriesHolder.getEntries())
    )
  }

  def getMagazineEntriesTree(themeGrouped: GroupByTheme): Map[String, Map[String, Map[String, List[MagazineEntry]]]] = {
    themeGrouped.members.map {
      case (theme, markGrouped) =>
        (theme, markGrouped.members.map { case (mark, modelGrouped) => (mark, modelGrouped.members) })
    }
  }

  def magazineEntry(theme: String, mark: Option[String], model: Option[String]): MagazineEntry =
    MagazineEntry(
      new Random().nextInt(),
      theme,
      "some title",
      "https://ya.ru/",
      new java.util.Date(ThreadLocalRandom.current().nextLong(1577826000000L, 1580418000000L)),
      "some description",
      "https://ya.ru/",
      mark,
      model
    )
}

package ru.yandex.realty.handlers.developer

import ru.yandex.realty.provider.stub.CompaniesStorageTestComponents
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.proto.developer.Statistic
import ru.yandex.realty.services.callback.CallbackService
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.storage.SiteSpecialProjectsBunkerStorage
import ru.yandex.realty.storage.pinned.PinnedSpecialProjectsStorage

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import ru.yandex.realty.model.sites._
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DeveloperManagerSpec extends SpecBase with CompaniesStorageTestComponents {
  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  val developerManager = new DeveloperManager(
    mock[Provider[CompaniesStorage]],
    mock[DeveloperStatisticBuilder],
    mock[DeveloperOfficesBuilder],
    mock[FinancialPerformanceBuilder],
    mock[DeveloperCustomTabsBuilder],
    mock[DeveloperSlidersBuilder],
    mock[DeveloperGeoStatisticBuilder],
    mock[SitesGroupingService],
    mock[Provider[PinnedSpecialProjectsStorage]],
    mock[Provider[SiteSpecialProjectsBunkerStorage]],
    mock[CallbackService]
  )

  val site1 = new Site(1)
  site1.setDecorationInfo(
    Seq(
      new DecorationInfo(Decoration.CLEAN, 1, "1")
    ).asJava
  )

  val site2 = new Site(2)
  site2.setDecorationInfo(
    Seq(
      new DecorationInfo(Decoration.CLEAN, 2, "2"),
      new DecorationInfo(Decoration.TURNKEY, 2, "2")
    ).asJava
  )

  val site3 = new Site(3)
  site3.setDecorationInfo(
    Seq(
      new DecorationInfo(Decoration.WHITE_BOX, 3, "3"),
      new DecorationInfo(Decoration.TURNKEY, 3, "3")
    ).asJava
  )

  "DeveloperManagerSpec " should {
    val developerSearchResult = Seq(
      DeveloperSearchResultHolder(
        0,
        companiesProvider.get().get(0),
        Seq(site1, site2),
        Set.empty,
        Some(5),
        Statistic.getDefaultInstance
      ),
      DeveloperSearchResultHolder(
        1,
        companiesProvider.get().get(1),
        Seq(site2, site3),
        Set.empty,
        Some(2),
        Statistic.getDefaultInstance
      ),
      DeveloperSearchResultHolder(
        2,
        companiesProvider.get().get(2),
        Seq.empty,
        Set.empty,
        Some(12),
        Statistic.getDefaultInstance
      )
    )
    "filter from years " in {
      val userInput = DeveloperUserInput(numberYearsFrom = Some(10), numberYearsTo = None, page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered.size shouldBe 1
      filtered.head.developer shouldBe companiesProvider.get().get(2)
    }
    "filter from and to years " in {
      val userInput = DeveloperUserInput(numberYearsFrom = Some(4), numberYearsTo = Some(6), page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered.size shouldBe 1
      filtered.head.developer shouldBe companiesProvider.get().get(0)
    }
    "filter only to years " in {
      val userInput = DeveloperUserInput(numberYearsFrom = None, numberYearsTo = Some(6), page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered.size shouldBe 2
    }

    "filter by id " in {
      val userInput = DeveloperUserInput(developerId = Set(2), page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered.size shouldBe 1
      filtered.head.developer shouldBe companiesProvider.get().get(2)
    }
    "filter by id with multiple ids " in {
      val userInput = DeveloperUserInput(developerId = Set(1, 2), page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered.size shouldBe 2
    }

    "don't change result anything if filter not specified" in {
      val userInput = DeveloperUserInput(numberYearsFrom = None, numberYearsTo = None, page = 1, pageSize = 10)
      val filtered = developerManager.filterDevelopers(developerSearchResult, userInput)
      filtered shouldBe developerSearchResult
    }

    "filter by decoration " in {
      val userInput1 = DeveloperUserInput(decoration = Set(Decoration.CLEAN), page = 1, pageSize = 10)
      val filtered1 = developerManager.filterDevelopers(developerSearchResult, userInput1)
      filtered1.size shouldBe 2
      filtered1.map(_.developer).toSet shouldBe Set(companiesProvider.get().get(0), companiesProvider.get().get(1))

      val userInput2 = DeveloperUserInput(decoration = Set(Decoration.TURNKEY), page = 1, pageSize = 10)
      val filtered2 = developerManager.filterDevelopers(developerSearchResult, userInput2)
      filtered2.size shouldBe 2
      filtered2.map(_.developer).toSet shouldBe Set(companiesProvider.get().get(0), companiesProvider.get().get(1))

      val userInput3 = DeveloperUserInput(decoration = Set(Decoration.WHITE_BOX), page = 1, pageSize = 10)
      val filtered3 = developerManager.filterDevelopers(developerSearchResult, userInput3)
      filtered3.size shouldBe 1
      filtered3.map(_.developer).toSet shouldBe Set(companiesProvider.get().get(1))

      val userInput4 = DeveloperUserInput(decoration = Set(Decoration.ROUGH), page = 1, pageSize = 10)
      val filtered4 = developerManager.filterDevelopers(developerSearchResult, userInput4)
      filtered4.isEmpty shouldBe true
    }
  }

}

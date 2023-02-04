package ru.yandex.realty.storage.pinned

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.storage.pinned.PinnedSpecialProjectsTestComponents.{
  AlreadyEnded,
  MskSamoletProject,
  NotYetStarted,
  VertoletProject
}

@RunWith(classOf[JUnitRunner])
class PinnedSpecialProjectsStorageSpec extends AsyncSpecBase {

  "PinnedSpecialProjectsStorage" should {
    "work without projects" in new PinnedSpecialProjectsProviderFixture {
      val storage = new PinnedSpecialProjectsStorage(Seq.empty)

      storage.pinnedSpecialProjects shouldBe Seq.empty
      storage.findPinnedSiteIds(node, regionGraph) shouldBe Seq.empty
    }

    "work with all project dates" in new PinnedSpecialProjectsProviderFixture {
      val storage = new PinnedSpecialProjectsStorage(Seq(AlreadyEnded))

      storage.pinnedSpecialProjects.size shouldBe 1
      storage.findPinnedSiteIds(node, regionGraph) shouldBe Seq.empty
    }

    "work with future projects" in new PinnedSpecialProjectsProviderFixture {
      val storage = new PinnedSpecialProjectsStorage(Seq(NotYetStarted))

      storage.pinnedSpecialProjects.size shouldBe 1
      storage.findPinnedSiteIds(node, regionGraph) shouldBe Seq.empty
    }

    "work with real data" in new PinnedSpecialProjectsProviderFixture {

      val storage = new PinnedSpecialProjectsStorage(Seq(MskSamoletProject, VertoletProject))

      storage.pinnedSpecialProjects.size shouldBe 2
      storage.findPinnedSiteIds(node, regionGraph) shouldBe Seq(2890148)
    }
  }

  trait PinnedSpecialProjectsProviderFixture {
    val regionGraph: RegionGraph = mock[RegionGraph]

    val node = new Node()
    node.setId(1234341L)
    node.setGeoId(1)

  }
}

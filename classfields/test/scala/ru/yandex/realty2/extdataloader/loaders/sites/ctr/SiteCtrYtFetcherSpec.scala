package ru.yandex.realty2.extdataloader.loaders.sites.ctr

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.scalatest.OneInstancePerTest
import ru.yandex.extdata.core.Data.StreamingData
import ru.yandex.extdata.core.ProduceResult.Produced
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.v2.FakeController
import ru.yandex.realty.mocks.yt.MockYtTable

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class SiteCtrYtFetcherSpec extends SpecBase with OneInstancePerTest {

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "empty nodes",
      nodes = Seq.empty,
      expectedData = StringUtils.EMPTY
    ),
    TestCase(
      description = "one node",
      nodes = Seq(
        YTree
          .mapBuilder()
          .key("id")
          .value(1813379)
          .key("calls")
          .value(2)
          .key("type")
          .value("newbuild")
          .key("money")
          .value(3000.0)
          .buildMap()
      ),
      expectedData = "1813379\t2"
    ),
    TestCase(
      description = "several mixed nodes",
      nodes = Seq(
        YTree
          .mapBuilder()
          .key("id")
          .value(1813379)
          .key("calls")
          .value(2)
          .key("type")
          .value("newbuild")
          .key("money")
          .value(3000.0)
          .buildMap(),
        YTree
          .mapBuilder()
          .key("id")
          .value(1773561)
          .key("calls")
          .value(1)
          .key("type")
          .value("village")
          .key("money")
          .value(1050.0)
          .buildMap(),
        YTree
          .mapBuilder()
          .key("id")
          .value(1778495)
          .key("calls")
          .value(4)
          .key("type")
          .value("village")
          .key("money")
          .value(12600.0)
          .buildMap(),
        YTree
          .mapBuilder()
          .key("id")
          .value(1761020)
          .key("calls")
          .value(3)
          .key("type")
          .value("newbuild")
          .key("money")
          .value(9000.0)
          .buildMap()
      ),
      expectedData =
        "1813379\t2\n" +
          "1773561\t1\n" +
          "1778495\t4\n" +
          "1761020\t3"
    )
  )

  "SiteCtrYtFetcher" should {
    testCases.foreach {
      case TestCase(description, nodes, expectedData) =>
        description in {
          val yt: Yt = mockYt(nodes)
          val fetcher = new SiteCtrYtFetcher(
            controller = new FakeController(),
            period = 1.hour,
            keepVersions = 3,
            yt = yt,
            ytCallsPath = YPath.simple("//home/verticals/realty/calls")
          )

          val result = fetcher.fetch(Option.empty)
          result match {
            case Success(value) =>
              value match {
                case Produced(StreamingData(is), _) =>
                  IOUtils.toString(is) shouldEqual expectedData
                case _ =>
                  fail(s"Unexpected produced result")
              }
            case Failure(exception) =>
              fail(exception)
          }
        }
    }
  }

  private def mockYt(nodes: Seq[YTreeMapNode]) = {
    val yt = mock[Yt]
    val ytTable = new MockYtTable(nodes)
    (yt.tables _).expects().returning(ytTable)
    yt
  }

  case class TestCase(
    description: String,
    nodes: Seq[YTreeMapNode],
    expectedData: String
  )

}

package ru.yandex.auto.extdata.jobs

import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.common.JobIndexDependencies
import ru.yandex.auto.common.util.yt.RichYT._
import ru.yandex.auto.core.dealer.{DealerInfo, DealerPhone}
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.extdata.AutoDataTypes.dealers
import ru.yandex.auto.log.Logging
import ru.yandex.extdata.core.DataType
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.vertis.application.environment.Configuration
import utils.DefaultEdsClient

import scala.collection.convert.decorateAll._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Success

class DealersFetcherSpec extends WordSpec with Matchers with DealersFixtures with Logging {
  private[jobs] val dataTypeListToFetch: List[DataType] = List(
    AutoDataTypes.rawVerba,
    AutoDataTypes.regions,
    AutoDataTypes.office7Clients,
    AutoDataTypes.dealersYaMapsRegions,
    AutoDataTypes.currency
  )

  implicit val cfg = Configuration.current().resolve()
  implicit val actorSystem: ActorSystem = ActorSystem(name = "autoru-job-service", config = cfg)
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit val operational: OperationalSupport = new OperationalSupport

  private val dependencies = new JobIndexDependencies(DefaultEdsClient(dataTypeListToFetch))
  private lazy val fetcher: YaMapsDealersProducer = new YaMapsDealersProducer(dealers, dependencies)

  // one-off smoke test to debug `fetch` internals
  "test dealersFetcher" in {
    pending
    Thread.sleep(15.seconds.toMillis)
    fetcher.produce(DealersFilter(Set()))
    Thread.sleep(15.seconds.toMillis)
  }

  "test enrichDealersWithTelepony" in {
    val results = fetcher.enrichDealersWithTelepony(Iterable(genDealer))

    results.size should be > 0
    results.foreach { di =>
      di.getPhones.size should be > 0
      di.getPhones.get(0).getRawPhone shouldEqual genDealer.getPhones.get(0).getRawPhone

      di.getTeleponyPhones should not be null
      di.getTeleponyPhones.size should be > 0
      di.getTeleponyPhones.get(0) should not be empty
      di.getTeleponyPhones.get(0) should not be genDealer.getPhones.get(0).getRawPhone
    }
  }

  "buildYaMapForYT should produce nothing if dealer has no offers" in {
    val dealer = genDealer
    dealer.setBackaId("123")
    dealer.setId(1)
    val results = fetcher.buildYaMapForYT(Iterable(dealer))
    results.size shouldEqual 0
  }

  "buildYaMapForYT should not produce CALL row if no telepony phone is provided" in {
    val dealer = genDealer
    dealer.setBackaId("123")
    val results = fetcher.buildYaMapForYT(Iterable(dealer))
    results.size shouldEqual 1
  }

  "buildYaMapForYT should produce 2 rows for a dealer (if telepony phones production is on)" in {
    val dealer = genDealer
    dealer.setBackaId("123")
    dealer.setTeleponyPhones(List("+79581003675").asJava)
    val results = fetcher.buildYaMapForYT(Iterable(dealer))
    println()
    println(results)

    results.size shouldEqual 2
  }

  "trackingPhoneNode should produce valid Map with parsed phone details" in {
    val parsePhone = fetcher.trackingPhoneNode("+79581003675")
    val expected: YTreeMapNode = Map(
      "details" -> Map(
        "country" -> "7",
        "number" -> "1003675",
        "region" -> "958"
      ),
      "formatted" -> "+7 (958) 100-36-75",
      "number" -> "79581003675"
    )

    parsePhone shouldEqual expected
  }

  "should parse input ENV params" in {
    val headToExclude = "132930745345"
    val env = """[%s,%s]""".format(headToExclude, headToExclude)
    val jsonFilter = AutoruYaMapsDealersMain.provideDealersFilter(Success(env))

    jsonFilter shouldBe DealersFilter(Set(headToExclude))
  }

  "should filter given dealer out by permalink" in {
    def fetchDealers(exclude: Set[String]): Stream[String] = {
      fetcher.processDealers(DealersFilter(exclude)).map(_.map(_.getBackaId)).get
    }

    val fullList = fetchDealers(Set())
    val headToExclude = fullList.head
    fullList.toSet.contains(headToExclude) shouldBe true

    fetchDealers(Set(headToExclude)).toSet
      .contains(headToExclude) shouldBe false
  }

}

trait DealersFixtures {

  def genDealer: DealerInfo = {
    val dealer = new DealerInfo
    val dealerPhone = new DealerPhone()

    dealerPhone.setRawPhone("+79086861374")
    dealer.setPhones(List(dealerPhone).asJava)
    dealer.setId(123L)
    dealer.setRegionId(2)
    dealer
  }
}

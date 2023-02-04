package ru.yandex.vertis.moderation.saas

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.{Globals, SpecBase}
import ru.yandex.vertis.moderation.concurrent.newForkJoinPoolExecutionContext
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Instance, RealtyEssentials}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.searcher.core.model.Sort.UpdateDate
import ru.yandex.vertis.moderation.searcher.core.saas.client.{HttpSearchClient, SaasOptions, SaasPrefixes}
import ru.yandex.vertis.moderation.searcher.core.saas.document.DocumentBuilder
import ru.yandex.vertis.moderation.searcher.core.saas.search.{RealtySearchQuery, SaasSearcher, SearchRequest}
import ru.yandex.vertis.moderation.service.impl.StubTvmTicketProvider
import ru.yandex.vertis.moderation.settings.HttpClientConfig
import ru.yandex.vertis.moderation.util.{DateTimeUtil, Page}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * Manually executive Specs for SaaS
  *
  * @author sunlight
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class SaasSpec extends SpecBase {

  val DefaultSort = UpdateDate(asc = false)

  implicit val executionContext: ExecutionContext = newForkJoinPoolExecutionContext(1, "saas-%d")

  private val saasOptions =
    new SaasOptions {
      def saasPrefixes: SaasPrefixes = SaasPrefixes(100500, Seq(100500), Seq(100500))
      def usePruning = true
      def realtime = true
    }

  implicit private val featureRegistry: FeatureRegistry = EmptyFeatureRegistry

  val saasClient =
    new HttpSearchClient(
      "http://saas-indexerproxy-prestable.yandex.net:80/service/4ae5b455c145cd41b8f69ca5c2028594",
      HttpClientConfig(
        requestTimeout = Some(10.seconds),
        readTimeout = Some(10.seconds),
        maxConnections = Some(128)
      ),
      "http://saas-searchproxy-prestable.yandex.net:17000",
      HttpClientConfig(
        requestTimeout = Some(10.seconds),
        readTimeout = Some(10.seconds),
        maxConnections = Some(128)
      ),
      saasOptions,
      ttl = Some(5.minutes),
      new StubTvmTicketProvider
    )

  val searcher =
    new SaasSearcher(
      client = saasClient,
      service = "vs_moderation_realty",
      options = saasOptions
    )

  val documentBuilder = new DocumentBuilder(Globals.opinionCalculator(Service.REALTY))

  private def create2Instances(): (Instance, Instance) = {
    val generated = CoreGenerators.instanceGen(CoreGenerators.RealtyEssentialsGen).next
    val re =
      generated.essentials
        .asInstanceOf[RealtyEssentials]
        .copy(
          clusterId = Some(Gen.choose(1L, 100000000L).next),
          clusterHead = Some(Gen.oneOf(true, false).next)
        )
    val instance = generated.copy(essentials = re)

    val instance2 =
      CoreGenerators
        .instanceGen(instance.externalId, instance.essentials)
        .next
        .copy(essentialsUpdateTime = instance.essentialsUpdateTime.plusDays(1))
        .copy(createTime = instance.createTime.plusDays(1))

    (instance, instance2)
  }

  "save and get" should {
    "by one id" in {
      val generated = CoreGenerators.instanceGen(CoreGenerators.RealtyEssentialsGen).next

      val re =
        generated.essentials
          .asInstanceOf[RealtyEssentials]
          .copy(clusterId = Some(Gen.choose(1L, 100000000L).next))
          .copy(clusterHead = Some(Gen.oneOf(true, false).next))
      val instance = generated.copy(essentials = re)

      val doc = documentBuilder.build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        saasClient.update(doc, DateTimeUtil.now(), kps = kps).futureValue
        saasClient.flush().futureValue

        val query = RealtySearchQuery(clusterId = instance.essentials.asInstanceOf[RealtyEssentials].clusterId)
        val request = SearchRequest(query, DefaultSort, Page(0, 10))
        val searchResponse = Await.result(searcher.search(request), 10.seconds)
        searchResponse.request should be(request)
        searchResponse.result.totalItems should be(1)
        searchResponse.result.items.nonEmpty should be(true)
        searchResponse.result.items.head should be(instance.id)
      }
    }

    "freshest by update date for 2 instances with same id" in {
      val (instance, instance2) = create2Instances()
      val doc = documentBuilder.build(instance)
      val doc2 = documentBuilder.build(instance2)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        saasClient.update(doc, DateTimeUtil.now(), kps = kps).futureValue
        saasClient.update(doc2, DateTimeUtil.now(), kps = kps).futureValue
        saasClient.flush().futureValue

        val query = RealtySearchQuery(clusterId = instance.essentials.asInstanceOf[RealtyEssentials].clusterId)
        val request = SearchRequest(query, DefaultSort, Page(0, 10))
        val searchResponse = Await.result(searcher.search(request), 10.seconds)
        searchResponse.request should be(request)
        searchResponse.result.totalItems should be(1)
        searchResponse.result.items.nonEmpty should be(true)
        searchResponse.result.items.head should be(instance2.id)
      }
    }

    /*    "search without grouping: 2 by update date for 2 instances with same id" in {
      val (instance, instance2) = create2Instances()
      val doc = documentBuilder.build(instance)
      val doc2 = documentBuilder.build(instance2)
      saasClient.update(doc).futureValue
      saasClient.update(doc2).futureValue
      saasClient.flush().futureValue

      val query = RealtySearchQuery(
        clusterId = instance.essentials.asInstanceOf[RealtyEssentials].clusterId)
      val request = SearchRequest(query, DefaultSort, Page(0, 10))
      val searchResponse = Await.result(searcher.search(request), 10.seconds)
      searchResponse.request should be(request)
      searchResponse.result.totalItems should be(2)
      searchResponse.result.items.nonEmpty should be(true)
      searchResponse.result.items.contains(instance.id) should be(true)
      searchResponse.result.items.contains(instance2.id) should be(true)
    }*/

    /*"search with sorts" in {
      val (instance11, instance12) = create2Instances()
      val doc11 = DocumentBuilder.build(instance11)
      val doc12 = DocumentBuilder.build(instance12)
      Await.result(saasClient.update(doc11), 10.seconds)
      Await.result(saasClient.update(doc12), 10.seconds)

      val (instance21, instance22) = create2Instances()
      val doc21 = DocumentBuilder.build(instance21)
      val doc22 = DocumentBuilder.build(instance22)
      Await.result(saasClient.update(doc21), 10.seconds)
      Await.result(saasClient.update(doc22), 10.seconds)

      val query = new RealtySearchQuery(clusterHead = Some(true))
      val request = new SearchRequest(query, Sort.UpdateDate, Page(0, 10), false)
      val searchResponse = Await.result(searcher.search(request), 10.seconds)
      searchResponse.request should be (request)
      //searchResponse.result.totalItems should be (2)
      searchResponse.result.items.nonEmpty should be (true)
      searchResponse.result.items.contains(instance12.id) should be (true)
      searchResponse.result.items.contains(instance22.id) should be (true)
      searchResponse.result.items.contains(instance11.id) should be (false)
      searchResponse.result.items.contains(instance21.id) should be (false)
    }*/
  }

  "delete" should {

    "executes correctly" in {
      val externalId = CoreGenerators.ExternalIdGen.next
      saasClient.delete(externalId, saasOptions.saasPrefixes.searchKps).futureValue
    }
  }
}

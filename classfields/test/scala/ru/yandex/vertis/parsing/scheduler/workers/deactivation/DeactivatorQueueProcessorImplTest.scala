package ru.yandex.vertis.parsing.scheduler.workers.deactivation

import java.io.{File, InputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.concurrent.atomic.AtomicInteger
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.components.bunkerconfig.BunkerConfig
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.ParsedOffersDao
import ru.yandex.vertis.parsing.auto.features.ParsingFeatures
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.cmexpert.cars.avito.CmExpertAvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.haraba.cars.avito.HarabaAvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito.ScrapingHubAvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.drom.ScrapingHubDromCarsParser
import ru.yandex.vertis.parsing.clients.downloader.{ContentEncoding, DownloadedFile, UrlDownloader}
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.components.time.DefaultTimeService
import ru.yandex.vertis.parsing.components.workersfactory.workers.DidNotWork
import ru.yandex.vertis.parsing.components.zookeeper.node.ZookeeperNode
import ru.yandex.vertis.parsing.dao.deactivator
import ru.yandex.vertis.parsing.dao.deactivator.DeactivatorQueueDao
import ru.yandex.vertis.parsing.features.SimpleFeatures
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.workmoments.WorkMoments
import ru.yandex.vertis.parsing.util.{IO, Threads}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DeactivatorQueueProcessorImplTest extends FunSuite with MockitoSupport {
  private val io = new IO(new File("."))

  implicit private val timeService: DefaultTimeService = new DefaultTimeService

  private val workMoments = mock[WorkMoments]

  private val rateLimitNode: ZookeeperNode[Int] = mock[ZookeeperNode[Int]]

  private val timeoutNode: ZookeeperNode[Int] = mock[ZookeeperNode[Int]]

  private val batchSizeNode: ZookeeperNode[Int] = mock[ZookeeperNode[Int]]

  private val anonymousUrlDownloader: UrlDownloader = mock[UrlDownloader]
  private val zoraUrlDownloader: UrlDownloader = mock[UrlDownloader]

  private val deactivatorQueueDao: DeactivatorQueueDao[Category] = mock[DeactivatorQueueDao[Category]]

  private val parsedOffersDao: ParsedOffersDao = mock[ParsedOffersDao]

  private val bunkerConfig = BunkerConfig(
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    Seq(),
    Seq()
  )

  private val features: ParsingFeatures = new SimpleFeatures with ParsingFeatures {
    override def webminingParsers: List[CommonAutoParser] = Nil

    override def av100parsers: List[CommonAutoParser] = Nil

    override def auto24parsers: List[CommonAutoParser] = Nil

    override def scrapingHubParsers: List[CommonAutoParser] =
      List(ScrapingHubAvitoCarsParser, ScrapingHubDromCarsParser)

    override def bunkerConfig: BunkerConfig = DeactivatorQueueProcessorImplTest.this.bunkerConfig

    override def harabaParsers: List[CommonAutoParser] = List(HarabaAvitoCarsParser)

    override def cmExpertParsers: List[CommonAutoParser] = List(CmExpertAvitoCarsParser)
  }

  implicit private val trace: Traced = TracedUtils.empty

  implicit private val ec: ExecutionContextExecutor = Threads.sameThreadEc

  private val deactivatorQueueProcessorImpl = new DeactivatorQueueProcessorImpl(
    workMoments,
    100,
    rateLimitNode,
    timeoutNode,
    batchSizeNode,
    zoraUrlDownloader,
    deactivatorQueueDao,
    parsedOffersDao,
    features,
    Site.Avito,
    Category.CARS
  ) {
    override def shouldWork: Boolean = true

    override def start(): Unit = {}

    override def stop(): Unit = {}
  }

  test("target site and category are disabled") {
    features.Deactivator.setEnabled(true)
    features.DeactivatorQueueProcessCategoryFromSite(Site.Avito -> Category.CARS).setEnabled(false)
    features.DeactivatorQueueProcessCategoryFromSite(Site.Drom -> Category.CARS).setEnabled(true)
    when(workMoments.isWorkMoment).thenReturn(true)
    when(workMoments.didNotWork).thenReturn(DidNotWork(1.second))
    assert(!deactivatorQueueProcessorImpl.doWork().actuallyWorked)
    verify(workMoments).didNotWork
  }

  test("stackoverflow check") {
    val count = new AtomicInteger(0)
    stub(deactivatorQueueDao.getUnprocessed(_: Site, _: Option[Category], _: Int)(_: Traced)) {
      case (_, _, _, _) =>
        val newCount = count.incrementAndGet()
        if (newCount > 2000) Seq.empty
        else Seq(deactivator.DeactivatorQueueRow(0, Category.CARS, Site.Avito, "hash", "url"))
    }
    features.Deactivator.setEnabled(true)
    features.DeactivatorQueueProcessCategoryFromSite(Site.Avito -> Category.CARS).setEnabled(true)
    when(rateLimitNode.get(?)).thenReturn(1000000)
    when(timeoutNode.get(?)).thenReturn(60)
    when(batchSizeNode.get(?)).thenReturn(100)
    val tmpFile = io.newTempFile("DeactivatorQueueProcessorImplTest", "")
    val in: InputStream = this.getClass.getResourceAsStream("/avito_inactive.html")
    Files.copy(in, Paths.get(tmpFile.getAbsolutePath), StandardCopyOption.REPLACE_EXISTING)
    when(zoraUrlDownloader.downloadFromUrl(?, ?, ?)(?))
      .thenReturn(Future.successful(DownloadedFile(tmpFile, ContentEncoding.None)))
    when(parsedOffersDao.setDeactivated(?, ?, ?, ?)(?)).thenReturn(Map("hash" -> true))
    when(deactivatorQueueDao.setProcessed(?)(?)).thenReturn(Map("hash" -> true))
    deactivatorQueueProcessorImpl.processNextBucketQueue(Site.Avito, Category.CARS)
  }
}

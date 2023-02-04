package vertis.broker.yops.tasks.publishing

import common.clients.reactor.ReactorClient
import common.clients.reactor.model._
import common.clients.reactor.testkit.InMemoryReactorClient
import common.yt.Yt.Attribute._
import common.yt.Yt.Attributes
import common.zio.logging.Logging
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods.byDay
import vertis.broker.yops.model.PartitionTable
import vertis.broker.yops.tasks.ReactorPublishingTask.YtReactorAttribute
import vertis.broker.yops.tasks.publish.PublishConfig.UpdatesCheck
import vertis.broker.yops.tasks.publish.{PublishConfig, ReactorPublisher}
import vertis.broker.yops.tasks.publishing.ReactorPublisherIntSpec._
import vertis.broker.yops.testkit.YtTasksTestSupport
import vertis.broker.yops.testkit.YtTasksTestSupport.getAllAttributesMap
import vertis.core.time.DateTimeUtils
import vertis.yt.model.attributes.YtAttribute
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.reactor.TestReactorSynchronizerSource
import vertis.yt.zio.reactor.model.YtPartitionInstance.YtPartitionReactorInstance
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.{BTask, BaseEnv}
import zio.clock.Clock
import zio.duration.{Duration => ZioDuration}
import zio.{IO, UIO, URIO, ZIO, ZManaged}
import java.time.LocalDate
import scala.concurrent.duration._

/** @author kusaeva
  */
class ReactorPublisherIntSpec extends YtZioTest with YtTasksTestSupport {

  override def basePath: YPath = testBasePath

  "ReactorPublisher" should {
    "publish to reactor if silence has elapsed" in
      publishTest(
        config =
          defaultConfig.copy(checkForUpdates = defaultConfig.checkForUpdates.map(c => c.copy(silence = 5.seconds))),
        day = LocalDate.now().minusDays(3)
      ) { test =>
        import test._

        for {
          _ <- publish
          instances <- listReactorInstances
          _ <- check("silence hasn't elapsed:") {
            val instanceOpt = instances.find(_.metadata.exists(_.path == table.ytPath.toString))
            instanceOpt shouldBe empty
          }
          _ <- sleepSilence
          _ <- publish
          newInstances <- listReactorInstances
          _ <- check("publish to reactor:") {
            val instanceOpt = newInstances.find(_.metadata.exists(_.path == table.ytPath.toString))
            instanceOpt shouldBe defined
            instanceOpt.get.userTimestamp.contains(DateTimeUtils.toInstant(day)) shouldBe true
          }
        } yield ()
      }

    "not republish if attributes changed" in
      publishTest(
        config =
          defaultConfig.copy(checkForUpdates = defaultConfig.checkForUpdates.map(c => c.copy(silence = 500.millis))),
        day = LocalDate.now().minusDays(3)
      ) { test =>
        import test._
        import resources.yt
        import table.attributes

        for {
          _ <- sleepSilence
          _ <- publish
          newAttrs <- getAllAttributesMap(yt, table.ytPath)
          newInstances <- listReactorInstances
          _ <- check("publish to reactor:") {
            val instanceOpt = newInstances.find(_.metadata.exists(_.path == table.ytPath.toString))
            instanceOpt shouldBe defined
          }
          _ <- check("check attributes:") {
            attributes.get(YtReactorAttribute) shouldBe empty
            newAttrs.get(YtReactorAttribute) shouldBe defined
            attributes(ContentRevision) shouldBe newAttrs(ContentRevision)
            newAttrs(ModificationTime).isAfter(attributes(ModificationTime)) shouldBe true
          }
          _ <- sleepSilence
          _ <- publish
          newerAttrs <- getAllAttributesMap(yt, table.ytPath)
          _ <- check("not republished:") {
            newerAttrs(ModificationTime) shouldBe newAttrs(ModificationTime)
          }
        } yield ()
      }

    "not publish if date is outside publish window" in
      publishTest(
        config = defaultConfig.copy(
          publishWindow = defaultConfig.checkForUpdates.map(_.partitioning.periodOf(2)),
          checkForUpdates = defaultConfig.checkForUpdates.map(c => c.copy(silence = 500.millis))
        ),
        day = LocalDate.now().minusDays(3)
      ) { test =>
        import test._

        for {
          _ <- sleepSilence
          _ <- publish
          instances <- listReactorInstances
          _ <- check("not publish to reactor:") {
            val instanceOpt = instances.find(_.metadata.exists(_.path == table.ytPath.toString))
            instanceOpt shouldBe empty
          }
        } yield ()
      }
  }

  /** create table for day at a random path and run test */
  private def publishTest(
      config: PublishConfig,
      day: LocalDate,
      attrs: Seq[YtAttribute] = Nil
    )(testIo: PublishTest => TestBody): Unit =
    ioTest {
      makeResources.use { resources =>
        import resources._
        for {
          base <- randomName
          ytTable = createDayTable(base, day, attrs)
          _ <- yt.cypressNoTx.createTable(ytTable)
          tableAttrs <- getAllAttributesMap(yt, ytTable.path)
          regTable = PartitionTable(
            day,
            ytTable.path,
            config.reactorPath,
            tableAttrs
          )
          _ <- testIo(PublishTest(resources, regTable, day, config))
        } yield ()
      }
    }

  private def makeResources: ZManaged[BaseEnv, Throwable, TestResources] =
    for {
      yt <- ytResources.map(_.yt)
      reactorClient = new InMemoryReactorClient
      log <- ZManaged.service[Logging.Service]
      synchro = TestReactorSynchronizerSource.createYt(
        reactorClient,
        yPath => new YtPartitionReactorInstance(yPath.toString),
        log
      )
      reactorPublisher = new ReactorPublisher(yt, synchro)
    } yield TestResources(yt, reactorClient, reactorPublisher)
}

object ReactorPublisherIntSpec {

  private val defaultConfig: PublishConfig =
    PublishConfig(
      ytPath = "subscriptions/notification_event/1d",
      reactorPath = "subscriptions/notification_event/daily",
      publishWindow = None,
      checkForUpdates = Some(UpdatesCheck(partitioning = byDay, silence = 3.hours))
    )

  case class TestResources(
      yt: YtZio,
      reactorClient: ReactorClient.Service,
      reactorPublisher: ReactorPublisher)

  case class PublishTest(resources: TestResources, table: PartitionTable, day: LocalDate, config: PublishConfig) {

    import resources._

    val allAttributes: YtTask[Attributes] = getAllAttributesMap(yt, table.ytPath)

    val publish: BTask[Boolean] = {
      config.checkForUpdates
        .map(updatesCheck =>
          allAttributes >>= { freshAttrs =>
            reactorPublisher.publishIfChanged(
              table.copy(attributes = freshAttrs),
              updatesCheck.partitioning,
              updatesCheck.silence,
              config.publishWindow,
              flushToReactor = true
            )
          }
        )
        .getOrElse(UIO(false))
    }

    val sleepSilence: URIO[Clock, Unit] =
      ZIO.sleep(ZioDuration.fromScala(config.checkForUpdates.map(_.silence.plus(200.millis)).getOrElse(0.millis)))

    val listReactorInstances: IO[ReactorError, Seq[ArtifactInstance[YPathArtifactMeta]]] =
      reactorClient.listInstances[YPathArtifactMeta](
        table.reactorPath,
        userTimestampFilter = Some(TimestampFilter.Range.wholeDay(day))
      )
  }
}

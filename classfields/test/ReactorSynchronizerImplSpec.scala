package vertis.yt.zio.reactor

import common.clients.reactor.ReactorClient
import common.clients.reactor.model._
import common.clients.reactor.testkit.InMemoryReactorClient
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import vertis.core.time.DateTimeUtils
import vertis.yt.storage.{AtomicStorage, AtomicStorageTestImpl}
import vertis.yt.zio.reactor.TestReactorSynchronizer.testConfig
import vertis.yt.zio.reactor.instances.ReactorInstance
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import zio.{IO, ZIO}

import java.time.LocalDate

/**
  */
class ReactorSynchronizerImplSpec extends ZioSpecBase {

  private case class Instance(day: LocalDate, ytPath: String = "//yt/test", version: Int = 1)

  implicit private object TestReactorInstance extends ReactorInstance[Instance] {

    override def makeInstance(in: Instance): ArtifactInstance[YPathArtifactMeta] =
      ArtifactInstance(
        userTimestamp = Some(DateTimeUtils.toInstant(in.day)),
        metadata = Some(YPathArtifactMeta(in.ytPath)),
        attributes = Map(versionAttribute -> in.version.toString)
      )
  }

  private val path = "/test"

  private val versionAttribute = "version"

  "ReactorSynchronizerImpl" should {
    "sync instance with reactor" in ioTest {
      for {
        storage <- AtomicStorageTestImpl.create[Seq[Instance]](Nil)
        client <- InMemoryReactorClient.create
        log <- ZIO.service[Logging.Service]
        synchro <- makeSynchro(storage, client, log)
        day = LocalDate.now()
        instance = Instance(day)
        _ <- synchro.publishOne(instance)
        published <- client
          .listInstances[YPathArtifactMeta](path, userTimestampFilter = Some(TimestampFilter.Range.wholeDay(day)))
      } yield {
        published.size shouldBe 1
        published.head.metadata.map(_.path) shouldBe Some(instance.ytPath)
      }
    }

    "replace instance in reactor if it is already there" in ioTest {
      for {
        storage <- AtomicStorageTestImpl.create[Seq[Instance]](Nil)
        client <- InMemoryReactorClient.create
        log <- ZIO.service[Logging.Service]
        synchro <- makeSynchro(storage, client, log)
        day = LocalDate.now()
        instance1 = Instance(day)
        res1 <- synchro.publishOne(instance1)
        instance2 = Instance(day, version = 2)
        res2 <- synchro.publishOne(instance2)
        published <- client
          .listInstances[YPathArtifactMeta](path, userTimestampFilter = Some(TimestampFilter.Range.wholeDay(day)))
      } yield {
        published.size shouldBe 1
        published.head.metadata.map(_.path) shouldBe Some(instance2.ytPath)
        published.head.attributes(versionAttribute) shouldBe "2"
      }
    }

    "keep instance in storage and don't fail on reactor failure" in ioTest {
      for {
        storage <- AtomicStorageTestImpl.create[Seq[Instance]](Nil)
        failingClient = new ReactorClient.Service {
          override def createInstance[T: ArtifactMetadata](
              path: String,
              instance: ArtifactInstance[T],
              artifactDescription: Option[String],
              createIfNotExist: Boolean,
              ttlDays: Int,
              projectId: Long): IO[ReactorError, ArtifactInstance[T]] =
            ZIO.fail(ReactorFailure(new RuntimeException("test"), false))

          override def deprecateInstance[T: ArtifactMetadata](
              path: String,
              instance: ArtifactInstance[T],
              prevInstance: ArtifactInstanceId,
              artifactDescription: Option[String],
              ttlDays: Int,
              projectId: Long): IO[ReactorError, ArtifactInstance[T]] =
            ZIO.fail(ReactorFailure(new RuntimeException("test"), false))

          override def listInstances[T: ArtifactMetadata](
              path: String,
              creationTimestampFilter: Option[TimestampFilter],
              userTimestampFilter: Option[TimestampFilter],
              sortBy: Option[SortBy],
              limit: Int): IO[ReactorError, Seq[ArtifactInstance[T]]] =
            ZIO.fail(ReactorFailure(new RuntimeException("test"), false))
        }
        log <- ZIO.service[Logging.Service]
        synchroFailing <- makeSynchro(storage, failingClient, log)
        day = LocalDate.now()
        instance = Instance(day)
        _ <- synchroFailing.publishOne(instance)
        deferred <- storage.get
      } yield {
        deferred.size shouldBe 1
        deferred.head shouldBe instance
      }
    }

    "sync deferred instance with reactor" in ioTest {
      for {
        client <- InMemoryReactorClient.create
        log <- ZIO.service[Logging.Service]
        day = LocalDate.now()
        instance = Instance(day)
        storage <- AtomicStorageTestImpl.create[Seq[Instance]](Seq(instance))
        synchro <- makeSynchro(storage, client, log)
        _ <- synchro.publishDeferred()
        published <- client
          .listInstances[YPathArtifactMeta](path, userTimestampFilter = Some(TimestampFilter.Range.wholeDay(day)))
      } yield {
        published.size shouldBe 1
        published.head.metadata.map(_.path) shouldBe Some(instance.ytPath)
      }
    }
  }

  private def makeSynchro(
      storage: AtomicStorage[BaseEnv, Seq[Instance]],
      reactorClient: ReactorClient.Service,
      log: Logging.Service) = {
    Prometheus.registry >>= { prometheus =>
      ReactorSynchronizerImpl.create[BaseEnv, Instance](
        storage,
        reactorClient,
        path,
        config = testConfig,
        prometheus = prometheus,
        log = log
      )
    }
  }
}

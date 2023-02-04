package common.clients.reactor.testkit

import common.clients.reactor.ReactorClient
import common.clients.reactor.model._
import zio._

import java.time.Instant
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

class InMemoryReactorClient extends ReactorClient.Service {

  import InMemoryReactorClient._

  private val idGen = new AtomicLong(0)
  private val storage = new AtomicReference[State](Map.empty.withDefaultValue(Seq.empty))

  override def createInstance[T: ArtifactMetadata](
      path: String,
      instance: ArtifactInstance[T],
      artifactDescription: Option[String],
      createIfNotExist: Boolean,
      ttlDays: Int,
      projectId: Long): IO[ReactorError, ArtifactInstance[T]] = UIO {
    val preparedInstance = instance.copy(id = idGen.incrementAndGet(), creationTimestamp = Instant.now())
    storage.updateAndGet { state =>
      val artifacts = state(path)
      state.updated(
        path,
        artifacts :+ preparedInstance
      )
    }
    preparedInstance
  }

  override def deprecateInstance[T: ArtifactMetadata](
      path: String,
      instance: ArtifactInstance[T],
      prevInstance: ArtifactInstanceId,
      artifactDescription: Option[String],
      ttlDays: Int,
      projectId: Long): IO[ReactorError, ArtifactInstance[T]] = UIO {
    val preparedInstance = instance.copy(id = idGen.incrementAndGet(), creationTimestamp = Instant.now())
    storage.updateAndGet { state =>
      val artifacts = state(path)
      val filtered = artifacts.filterNot(_.id == prevInstance)
      state.updated(
        path,
        filtered :+ preparedInstance
      )
    }
    preparedInstance
  }

  private def filterTimestamp(timestamp: Instant, filter: TimestampFilter) = {
    filter match {
      case r: TimestampFilter.Range => r.includes(timestamp)
    }
  }

  override def listInstances[T: ArtifactMetadata](
      path: String,
      creationTimestampFilter: Option[TimestampFilter],
      userTimestampFilter: Option[TimestampFilter],
      sortBy: Option[SortBy],
      limit: Int): IO[ReactorError, Seq[ArtifactInstance[T]]] = UIO {
    val all = storage
      .get()(path)
      .filter { v =>
        userTimestampFilter.forall(f => v.userTimestamp.exists(t => filterTimestamp(t, f))) &&
        creationTimestampFilter.forall(f => filterTimestamp(v.creationTimestamp, f))
      }

    val sorted = sortBy match {
      case Some(sort) =>
        val instances = sort.field match {
          case SortByField.Id => all.sortBy(_.id)
          case SortByField.CreationTimestamp => all.sortBy(_.creationTimestamp)
          case SortByField.UserTimestamp => all.sortBy(_.userTimestamp)
        }
        if (sort.ascending) instances else instances.reverse
      case None => all
    }

    sorted.take(limit).asInstanceOf[Seq[ArtifactInstance[T]]]
  }

  // for tests
  def createInstanceSimple(
      path: String,
      ytPath: String,
      userTimestamp: Instant): IO[ReactorError, ArtifactInstance[YPathArtifactMeta]] = {
    val instance = ArtifactInstance(
      userTimestamp = Some(userTimestamp),
      metadata = Some(YPathArtifactMeta(ytPath))
    )
    createInstance(path, instance, None, ttlDays = ttlDays, projectId = projectId)
  }
}

object InMemoryReactorClient {
  private type State = Map[String, Seq[ArtifactInstance[_]]]

  val ttlDays: Int = 30
  val projectId: Long = 1L

  def create: UIO[InMemoryReactorClient] = UIO(new InMemoryReactorClient)

}

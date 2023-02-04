package vertis.sraas.dao

import java.util.concurrent.ConcurrentHashMap

import com.google.protobuf.DescriptorProtos

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IterableHasAsScala

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class TestSchemaRegistryDao[V] extends SchemaRegistryDao[V] {
  private val storage = new ConcurrentHashMap[V, DescriptorProtos.FileDescriptorSet]()

  override def get(version: V): Future[Option[DescriptorProtos.FileDescriptorSet]] =
    Future.successful(Option(storage.get(version)))

  override def set(version: V, descriptorSet: DescriptorProtos.FileDescriptorSet): Future[Unit] =
    Future.successful(storage.put(version, descriptorSet): Unit)

  override def allVersions(): Future[Seq[V]] =
    Future.successful(storage.keySet().asScala.toSeq)

  def remove(version: V): Future[DescriptorProtos.FileDescriptorSet] =
    Future.successful(storage.remove(version))
}

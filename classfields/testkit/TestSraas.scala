package common.sraas

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import common.clients.sraas.Version
import common.sraas.Sraas.{Sraas, SraasDescriptor, SraasFileDescriptorSet, SraasMessageNames}
import zio.{Has, RIO, Ref, Task, UIO, ULayer, ZIO, ZRef}

object TestSraas {

  type TestSraas = Has[Service]

  trait Service {
    def setJavaDescriptor(response: DescriptorKey => Task[SraasDescriptor]): UIO[Unit]
  }

  def setJavaDescriptor(response: DescriptorKey => Task[SraasDescriptor]): RIO[TestSraas, Unit] = {
    ZIO.accessM(_.get.setJavaDescriptor(response))

  }

  case class DescriptorKey(protoMessageName: String, version: Version)

  private class ServiceImpl(effectRef: Ref[DescriptorKey => Task[SraasDescriptor]]) extends Service {

    override def setJavaDescriptor(response: DescriptorKey => Task[SraasDescriptor]): UIO[Unit] =
      effectRef.set(response)
  }

  private class SraasImpl(
      effectRef: Ref[DescriptorKey => Task[SraasDescriptor]])
    extends Sraas.Service {

    override def getDescriptor(
        protoMessageName: String,
        version: Version = Version.Last): Task[SraasDescriptor] =
      effectRef.get.flatMap(_(DescriptorKey(protoMessageName, version)))

    override def getMessageNamesWithOption(option: String, version: Version): Task[SraasMessageNames] =
      Task(SraasMessageNames(protoMessageNames = Nil, version = version.toString))

    override def getFull(version: Version): Task[Sraas.SraasFileDescriptorSet] =
      Task(SraasFileDescriptorSet(fds = FileDescriptorSet.getDefaultInstance, version = version.toString))
  }

  val layer: ULayer[Sraas with TestSraas] = {
    val effect =
      for {
        effectRef <- ZRef.make[DescriptorKey => Task[SraasDescriptor]](dk =>
          Task.fail(new IllegalArgumentException(s"Unstubbed descriptor key $dk"))
        )
        responseSetter: Service = new ServiceImpl(effectRef)
        sraas: Sraas.Service = new SraasImpl(effectRef)
      } yield Has.allOf(sraas, responseSetter)
    effect.toLayerMany
  }

}

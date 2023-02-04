package vertis.palma.service.descriptors

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import common.clients.sraas.Version
import common.sraas.Sraas.{Sraas, SraasDescriptor, SraasFileDescriptorSet, SraasMessageNames}
import common.sraas.{DescriptorNotFound, Sraas}
import vertis.zio.ServerEnv
import vertis.zio.test.ZioSpecBase
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent
import vertis.palma.test.Mark
import zio.{ZIO, ZLayer}
import zio._

/**
  */
class SraasDescriptorRepositoryIntSpec extends ZioSpecBase {

  val sraasLayer: ULayer[Sraas] = ZLayer.succeed(
    new Sraas.Service {

      override def getDescriptor(protoMessageName: String, version: Version): Task[SraasDescriptor] = Task {
        protoMessageName match {
          case "vertis.palma.test.Mark" =>
            SraasDescriptor(Mark.getDescriptor, protoMessageName, version.toString)
          case "palma.changelog.MutationEvent" =>
            SraasDescriptor(MutationEvent.javaDescriptor, protoMessageName, version.toString)
          case _ =>
            throw new DescriptorNotFound(protoMessageName, version)
        }
      }

      override def getMessageNamesWithOption(option: String, version: Version): Task[SraasMessageNames] = Task {
        SraasMessageNames(protoMessageNames = Seq("vertis.palma.test.Mark"), version = "v0.0.1")
      }

      override def getFull(version: Version): Task[Sraas.SraasFileDescriptorSet] = Task {
        SraasFileDescriptorSet(fds = FileDescriptorSet.getDefaultInstance, version = version.toString)
      }
    }
  )

  "SraasDescriptorRepository" should {
    "get some schemas" in ioTest {
      SraasDescriptorRepository
        .make()
        .provideSomeLayer[ServerEnv](sraasLayer)
        .use { repository =>
          for {
            state <- repository.repositoryState
            _ <- check {
              state.descriptorsByName should not be empty
            }
          } yield ()
        }
    }
  }
}

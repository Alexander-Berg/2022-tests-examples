package vertis.palma

import com.google.protobuf.Descriptors
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent
import vertis.zio.{BaseEnv, RTask}
import vertis.palma.service.descriptors.DescriptorRepository.{RepositoryState, SchemaCallback}
import vertis.palma.service.descriptors.{DescriptorRepository, SraasDescriptorRepository}
import vertis.sraas.model.SchemaVersion
import zio.{IO, URIO, ZIO}

/** @author tolmach
  */
case class StaticDescriptorRepository(
    version: SchemaVersion,
    descriptors: Seq[Descriptors.Descriptor],
    mutationEventDescriptor: Descriptors.Descriptor = MutationEvent.javaDescriptor)
  extends DescriptorRepository {

  override val repositoryState: RTask[DescriptorRepository.RepositoryState] = {
    SraasDescriptorRepository.toDescriptorsByDictionaryId(descriptors).map { descriptorsByDictionaryId =>
      val descriptorsByName = descriptors.map { d =>
        d.getFullName -> d
      }.toMap

      RepositoryState(
        version,
        descriptorsByName,
        descriptorsByDictionaryId,
        mutationEventDescriptor
      )
    }
  }

  private def getDescriptorFromState(protoMessageName: String): RTask[Descriptors.Descriptor] = {
    repositoryState.flatMap { state =>
      state.descriptorsByName.get(protoMessageName) match {
        case Some(descriptor) =>
          ZIO.succeed(descriptor)
        case None =>
          ZIO.fail(new NoSuchElementException(s"Message $protoMessageName [$version] was not found"))
      }
    }
  }

  override def addCallback(name: String)(cb: SchemaCallback): URIO[BaseEnv, Unit] = {
    IO.fail(throw new UnsupportedOperationException)
  }

  override def removeCallback(name: String): URIO[BaseEnv, Unit] = {
    IO.fail(throw new UnsupportedOperationException)
  }

}

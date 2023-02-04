package vertis.palma.service.descriptors

import vertis.palma.service.descriptors.DescriptorRepository.SchemaCallback
import vertis.zio.{BaseEnv, RTask}
import zio.URIO

/** @author kusaeva
  */
class TestDescriptorRepository extends DescriptorRepository {
  override def repositoryState: RTask[DescriptorRepository.RepositoryState] = ???

  override def addCallback(name: String)(cb: SchemaCallback): URIO[BaseEnv, Unit] = ???

  override def removeCallback(name: String): URIO[BaseEnv, Unit] = ???
}

package vasgen.core.saas.mock

import vasgen.core.VasgenStatus
import vasgen.core.zk.ZkSemaphore
import zio._

object ZkSemaphoreStub extends ZkSemaphore.Service[TestSetup] {

  override def doLeased[R, T](
    io: => ZIO[R, VasgenStatus, T],
  ): ZIO[R, VasgenStatus, T] = io

}

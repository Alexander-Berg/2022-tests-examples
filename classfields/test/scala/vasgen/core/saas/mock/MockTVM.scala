package vasgen.core.saas.mock

object MockTVM extends TVM.Service {

  override def getServiceTicket(destinationId: Int): Task[ServiceTicketBody] =
    ZIO.die(new IllegalStateException("Mock"))

  override def checkServiceTicket(
    ticket: ServiceTicketBody,
  ): UIO[CheckedServiceTicket] = ZIO.die(new IllegalStateException("Mock"))

  override def unsafeTicketProvider: UIO[UnsafeTicketProvider] =
    ZIO.die(new IllegalStateException("Mock"))

  def empty: ZLayer[Any, Nothing, TVM.Service] = ZIO.succeed(MockTVM).toLayer

}

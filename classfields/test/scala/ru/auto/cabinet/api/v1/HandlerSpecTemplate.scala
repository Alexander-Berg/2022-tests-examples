package ru.auto.cabinet.api.v1

import akka.http.scaladsl.server.{Directive, Directive0}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.ArgumentMatchers
import org.scalatest.{BeforeAndAfter, Suite}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.api.{
  DomainDirectives,
  DomainExceptionHandler,
  RequestObserver
}
import ru.auto.cabinet.json.DomainMarshaller
import ru.auto.cabinet.trace.CabinetRequestContext

trait HandlerSpecTemplate
    extends ScalatestRouteTest
    with DomainExceptionHandler
    with DomainMarshaller
    with DomainDirectives
    with Matchers
    with BeforeAndAfter {
  this: Suite =>

  implicit val requestObserver: RequestObserver =
    new RequestObserver.CompositeObserver()

  /*
   * CabinetRequestContext прокидывается из Handler`a на несколько уровней выше,
   * потому нужна обертка
   */
  def wrapRequestMock: Directive0 = Directive { inner => ctx =>
    val newCtx = CabinetRequestContext.wrap(ctx)
    inner.apply(())(newCtx)
  }

  def any[T](): T = ArgumentMatchers.any()

  def ?[T]: T = any()

  object eq {
    def apply[T](t: T): T = ArgumentMatchers.eq(t)

    def apply(t: Long): Long = ArgumentMatchers.eq(t)

    def apply(t: Boolean): Boolean = ArgumentMatchers.eq(t)
  }

  //  override def testConfig: Config = ConfigFactory.parseResources("test.conf")
  //  def actorRefFactory: ActorRefFactory = system

}

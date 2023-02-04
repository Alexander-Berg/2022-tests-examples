package ru.yandex.realty.grpc.server

import io.grpc.{Context, Metadata, ServerCall, ServerCallHandler, Status}
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.passport.tvmauth.{CheckedServiceTicket, CheckedUserTicket, TicketStatus}
import ru.yandex.realty.auth.Application
import ru.yandex.realty.grpc.ContextKeys.RequestImplContextKey
import ru.yandex.realty.grpc.MetadataKeys.ServiceTicketHeader
import ru.yandex.realty.request.RequestImpl
import ru.yandex.realty.service.DefaultApplicationService
import ru.yandex.realty.service.DefaultApplicationService.ApplicationMapping
import ru.yandex.realty.tvm.TvmLibraryApi
import ru.yandex.vertis.application.runtime.RuntimeConfig

@RunWith(classOf[JUnitRunner])
class ApplicationAuthInterceptorSpec
  extends FlatSpec
  with TableDrivenPropertyChecks
  with Matchers
  with OneInstancePerTest {

  private val KnownAppSrc = 1
  private val KnownApp = Application.Nginx
  private val SandboxApp = Application.Swagger
  private val UnknownAppSrc = 2
  private val SandboxAppSrc = 3

  private val OkTicket = "ok"
  private val UnknownTicket = "unknown"
  private val SandboxTicket = "sandbox"
  private val BadTicket = "bad"

  def test(
    prod: Boolean,
    tvmOn: Boolean,
    restricted: Boolean,
    ticket: Option[String],
    expectedErrorOrApp: Either[Status, Application]
  ): Unit = {
    new MockFactory {
      val runtime = mock[RuntimeConfig]
      (runtime.isEnvironmentStable _).expects().returns(prod)

      val tvm = new TvmLibraryApi {
        override def getTicket(service: String): String = ???
        override def checkServiceTicket(ticket: String, allowedClients: Set[Int]): Unit = ???
        override def checkUserTicket(ticket: String): CheckedUserTicket = ???
        override def getUidFromUserTicket(userTicket: String): Long = ???

        override def checkServiceTicket(ticket: String): CheckedServiceTicket = {
          (tvmOn, ticket) match {
            case (false, _) => new CheckedServiceTicket(TicketStatus.OK, "", 0, 0)
            case (true, OkTicket) => new CheckedServiceTicket(TicketStatus.OK, "", KnownAppSrc, 0)
            case (true, UnknownTicket) => new CheckedServiceTicket(TicketStatus.OK, "", UnknownAppSrc, 0)
            case (true, SandboxTicket) => new CheckedServiceTicket(TicketStatus.OK, "", SandboxAppSrc, 0)
            case (true, BadTicket) => new CheckedServiceTicket(TicketStatus.MALFORMED, "", 0, 0)
          }
        }
      }

      val apps = new DefaultApplicationService(
        Seq(
          ApplicationMapping(KnownApp.name, token = None, src = Some(KnownAppSrc)),
          ApplicationMapping(SandboxApp.name, token = None, src = Some(SandboxAppSrc))
        )
      )

      val call = mock[ServerCall[String, String]]
      val next = mock[ServerCallHandler[String, String]]
      expectedErrorOrApp match {
        case Left(s) => (call.close _).expects(where((status, _) => s.getCode == status.getCode)).once()
        case Right(a) => (next.startCall _).expects(*, *)
      }

      val interceptor = new ApplicationAuthInterceptor(tvm, apps, runtime, !restricted)

      val context = Context.ROOT.withValue(RequestImplContextKey, new RequestImpl())
      val prevContext = context.attach()
      try {
        interceptor.interceptCall(
          call, {
            val m = new Metadata()
            ticket.foreach(m.put(ServiceTicketHeader, _))
            m
          },
          next
        )

        expectedErrorOrApp.right.foreach { app =>
          RequestImplContextKey.get().application shouldBe app
        }
      } finally {
        context.detach(prevContext)
      }
    }
  }

  val cases = Table(
    ("prod", "tvm", "restricted", "ticket", "expectedErrorOrApp"),
    // unsecured
    (false, false, false, None, Right(Application.Regular)),
    (false, false, false, Some(OkTicket), Right(Application.Regular)),
    (false, false, false, Some(UnknownTicket), Right(Application.Regular)),
    (false, false, false, Some(SandboxTicket), Right(Application.Regular)),
    (false, false, false, Some(BadTicket), Right(Application.Regular)),
    (true, false, false, None, Right(Application.Regular)),
    (true, false, false, Some(OkTicket), Right(Application.Regular)),
    (true, false, false, Some(UnknownTicket), Right(Application.Regular)),
    (true, false, false, Some(SandboxTicket), Right(Application.Regular)),
    (true, false, false, Some(BadTicket), Right(Application.Regular)),
    // restricted no tvm
    (false, false, true, None, Left(Status.UNAUTHENTICATED)),
    (false, false, true, Some(OkTicket), Left(Status.PERMISSION_DENIED)),
    (false, false, true, Some(UnknownTicket), Left(Status.PERMISSION_DENIED)),
    (false, false, true, Some(SandboxTicket), Left(Status.PERMISSION_DENIED)),
    (false, false, true, Some(BadTicket), Left(Status.PERMISSION_DENIED)),
    (true, false, true, None, Left(Status.UNAUTHENTICATED)),
    (true, false, true, Some(OkTicket), Left(Status.PERMISSION_DENIED)),
    (true, false, true, Some(UnknownTicket), Left(Status.PERMISSION_DENIED)),
    (true, false, true, Some(SandboxTicket), Left(Status.PERMISSION_DENIED)),
    (true, false, true, Some(BadTicket), Left(Status.PERMISSION_DENIED)),
    // permissive tvm
    (false, true, false, None, Right(Application.Regular)),
    (false, true, false, Some(OkTicket), Right(KnownApp)),
    (false, true, false, Some(UnknownTicket), Right(Application.Regular)),
    (false, true, false, Some(SandboxTicket), Right(SandboxApp)),
    (false, true, false, Some(BadTicket), Left(Status.UNAUTHENTICATED)),
    (true, true, false, None, Right(Application.Regular)),
    (true, true, false, Some(OkTicket), Right(KnownApp)),
    (true, true, false, Some(UnknownTicket), Right(Application.Regular)),
    (true, true, false, Some(SandboxTicket), Left(Status.PERMISSION_DENIED)),
    (true, true, false, Some(BadTicket), Left(Status.UNAUTHENTICATED)),
    // restrictive tvm
    (false, true, true, None, Left(Status.UNAUTHENTICATED)),
    (false, true, true, Some(OkTicket), Right(KnownApp)),
    (false, true, true, Some(UnknownTicket), Left(Status.PERMISSION_DENIED)),
    (false, true, true, Some(SandboxTicket), Right(SandboxApp)),
    (false, true, true, Some(BadTicket), Left(Status.UNAUTHENTICATED)),
    (true, true, true, None, Left(Status.UNAUTHENTICATED)),
    (true, true, true, Some(OkTicket), Right(KnownApp)),
    (true, true, true, Some(UnknownTicket), Left(Status.PERMISSION_DENIED)),
    (true, true, true, Some(SandboxTicket), Left(Status.PERMISSION_DENIED)),
    (true, true, true, Some(BadTicket), Left(Status.UNAUTHENTICATED))
  )

  behavior of classOf[ApplicationAuthInterceptor].getName

  it should "work correctly" in {
    forAll(cases)(test)
  }

}

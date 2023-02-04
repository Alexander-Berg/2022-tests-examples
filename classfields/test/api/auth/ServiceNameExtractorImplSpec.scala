package vertis.palma.api.auth

import common.zio.app.Environments
import common.zio.tvm.ServiceTickets.Ticket
import common.zio.tvm.{InvalidTicket, ServiceTickets}
import org.scalatest.Assertion
import ru.yandex.passport.tvmauth.TicketStatus
import ru.yandex.vertis.shiva.api.service_map.api.{ListRequest, ResolveTvmIDRequest, ResolveTvmIDResponse}
import ru.yandex.vertis.shiva.service_map.schema.ServiceMap
import ru.yandex.vertis.shiva.types.layer.layer.Layer
import vertis.palma.api.auth.ServiceNameExtractorImpl.{UnexpectedTvmLayer, UnknownService}
import vertis.shiva.client.ShivaPublicApiClient
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase
import zio.{IO, Task, UIO, ZIO, ZManaged}

class ServiceNameExtractorImplSpec extends ZioSpecBase {

  private class MockedServiceTickets(knownValidTickets: Map[String, Int]) extends ServiceTickets.Service {

    override def verifyServiceTicket(ticket: String): IO[InvalidTicket, Ticket] = {
      ZIO
        .fromOption(knownValidTickets.get(ticket))
        .orElseFail(InvalidTicket(TicketStatus.MALFORMED, ""))
        .map(id => ServiceTickets.Ticket(TicketStatus.OK, id))
    }
  }

  private class MockedShivaClient(knownServices: Map[Int, ResolveTvmIDResponse]) extends ShivaPublicApiClient {

    override def resolveTvmID(request: ResolveTvmIDRequest): Task[ResolveTvmIDResponse] = Task {
      knownServices(request.tvmID.toInt)
    }

    override def listServices(request: ListRequest = ListRequest()): Task[Seq[ServiceMap]] =
      UIO(Seq.empty)
  }

  private def make(
      ticketToTvmId: Map[String, Int],
      serviceToShivaResponse: Map[Int, ResolveTvmIDResponse],
      fallbackServiceNameToId: Map[String, Int],
      env: Environments.Value): ZManaged[Any, Throwable, ServiceNameExtractorImpl] =
    Task {
      val tvmService = new MockedServiceTickets(ticketToTvmId)
      val shivaClient = new MockedShivaClient(serviceToShivaResponse)
      val authConfig = AuthConfig(couldAuthByServiceName = true, knownServices = fallbackServiceNameToId)
      new ServiceNameExtractorImpl(tvmService, shivaClient, authConfig, env)
    }.toManaged_

  private val OnlyShivaId: Int = 500;
  private val OnlyFallbackId: Int = 777;
  private val ShivaAndFallbackId: Int = 100;

  private val UnknownId: Int = 666;
  private val UnknownEnvShivaId: Int = 333;
  private val InvalidEnvShivaId: Int = 100000;

  private val ShivaServices: Map[Int, ResolveTvmIDResponse] = Map(
    OnlyShivaId -> ResolveTvmIDResponse("shiva1", Layer.TEST),
    ShivaAndFallbackId -> ResolveTvmIDResponse("shiva2", Layer.TEST),
    InvalidEnvShivaId -> ResolveTvmIDResponse("shiva3", Layer.PROD),
    UnknownEnvShivaId -> ResolveTvmIDResponse("shiva4")
  )

  private val FallbackServices: Map[String, Int] = Map(
    "fallback1" -> OnlyFallbackId,
    "fallback2" -> ShivaAndFallbackId
  )

  private def withServiceNameResolver(
      ticketToTvmId: Map[String, Int] = Map.empty,
      knownServices: Map[Int, ResolveTvmIDResponse] = ShivaServices,
      fallbackServices: Map[String, Int] = FallbackServices,
      env: Environments.Value = Environments.Testing
    )(action: ServiceNameExtractorImpl => BTask[Assertion]): Unit = ioTest {
    (for {
      serviceNameExtractor <- make(ticketToTvmId, knownServices, fallbackServices, env)
      res <- action(serviceNameExtractor).toManaged_
    } yield res).useNow
  }

  "TvmSupport.resolveService" should {

    "resolve name by shiva" in withServiceNameResolver() { serviceNameExtractor =>
      for {
        res <- serviceNameExtractor.resolveService(OnlyShivaId)
        a <- check("shiva")(res shouldBe "shiva1")
      } yield a
    }

    "fallback to config mapping" in withServiceNameResolver() { serviceNameExtractor =>
      for {
        res <- serviceNameExtractor.resolveService(OnlyFallbackId)
        a <- check("fallback")(res shouldBe "fallback1")
      } yield a
    }

    "prefer value from config against resolved by shiva" in withServiceNameResolver() { serviceNameExtractor =>
      for {
        res <- serviceNameExtractor.resolveService(ShivaAndFallbackId)
        a <- check("both")(res shouldBe "fallback2")
      } yield a
    }

    "use resolved name if got unknown layer" in withServiceNameResolver() { serviceNameExtractor =>
      for {
        res <- serviceNameExtractor.resolveService(UnknownEnvShivaId)
        a <- check("without layer")(res shouldBe "shiva4")
      } yield a
    }

    "failed with UnexpectedTvmLayer if got unexpected tvm_id layer from shiva" in withServiceNameResolver() {
      serviceNameExtractor =>
        for {
          res <- serviceNameExtractor.resolveService(InvalidEnvShivaId).either
          assertion <- check("unexpected layer") {
            res match {
              case Left(ex) => ex shouldBe UnexpectedTvmLayer(InvalidEnvShivaId, "shiva3", Layer.PROD)
              case r => fail(s"Expected UnexpectedTvmLayer but got $r")
            }
          }
        } yield assertion
    }

    "failed with UnknownService if no one services found" in withServiceNameResolver() { serviceNameExtractor =>
      for {
        res <- serviceNameExtractor.resolveService(UnknownId).either
        assertion <- check("unknown service") {
          res match {
            case Left(ex) => ex shouldBe UnknownService(UnknownId)
            case r => fail(s"Expected UnknownService but got $r")
          }
        }
      } yield assertion
    }
  }
}

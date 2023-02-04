package infra.profiler_collector.api.test

import akka.http.scaladsl.model.{ContentTypes, StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import common.akka.http.OpsDirectives
import common.profiler.Profiler
import common.zio.ops.tracing.testkit.TestTracing
import infra.profiler_collector.api.{Reader, Routes, Writer}
import infra.profiler_collector.model.{Query, StackTrace, TraceSample}
import io.opentracing.noop.NoopTracerFactory
import ru.yandex.vertis.ops.prometheus.CompositeCollector
import zio._
import zio.magic._
import zio.test._

import java.nio.file.{Files, Path}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.mutable

object RoutesSpec extends DefaultRunnableSpec {

  case class WriterInvocation(
      report: Path,
      mode: Profiler.ProfilingMode,
      serviceName: String,
      labels: Map[String, String],
      reportBody: Array[Byte])

  class MockWriter extends Writer {
    var invocations = Seq.empty[WriterInvocation]

    override def writeJfrReport(
        report: Path,
        mode: Profiler.ProfilingMode,
        serviceName: String,
        labels: Map[String, String]): ZIO[Any, Throwable, Unit] =
      ZIO.effectTotal(
        invocations :+= WriterInvocation(
          report,
          mode,
          serviceName,
          labels,
          Files.readAllBytes(report)
        )
      )
  }

  val TestQuery =
    Query(
      Instant.EPOCH.minus(10, ChronoUnit.MINUTES),
      Instant.EPOCH,
      "cpu",
      "test-service",
      Map("dc" -> "sas")
    )

  val TestSamples = Seq(
    TraceSample(
      StackTrace(
        Seq(
          StackTrace.Frame("a", 1, StackTrace.FrameType.Interpreted),
          StackTrace.Frame("b", 1, StackTrace.FrameType.Interpreted)
        )
      ),
      1
    ),
    TraceSample(
      StackTrace(
        Seq(
          StackTrace.Frame("a", 1, StackTrace.FrameType.Interpreted),
          StackTrace.Frame("c", 1, StackTrace.FrameType.Interpreted)
        )
      ),
      4
    )
  )

  class MockReader extends Reader {
    var mockData = mutable.HashMap.empty[Query, Seq[TraceSample]]

    override def getSamples(query: Query): Task[Seq[TraceSample]] =
      Task.effectTotal(mockData.getOrElse(query, Seq.empty))
  }

  val routeTestkit = ZManaged.makeEffect(new RouteTest with TestFrameworkInterface {
    override def failTest(msg: String): Nothing = sys.error(msg)
    override def testExceptionHandler: ExceptionHandler = ExceptionHandler { case e: Throwable => throw e }
  })(_.cleanUp())

  val testReportBytes = Array.tabulate[Byte](35000)(_.toByte)

  def seal(route: Route): Route = {
    val wrap = OpsDirectives.makeDefault(NoopTracerFactory.create(), new CompositeCollector)
    Route.seal(wrap(route))
  }

  def spec =
    suite("Routes")(
      testM("save") {
        routeTestkit.use { testkit =>
          import testkit._
          for {
            routes <- ZIO.service[Routes]

            uri = Uri("/upload/jfr")
              .withQuery(
                Uri.Query(
                  "service" -> "test",
                  "labels" -> "{\"a\": \"b\"}",
                  "mode" -> "cpu,alloc"
                )
              )

            statusResult = Post(uri, testReportBytes) ~> seal(routes.routes) ~> check {
              status
            }
            invocations <- ZIO.service[Writer].map(_.asInstanceOf[MockWriter].invocations)
          } yield {
            val tempFileDeleted = !Files.exists(invocations.head.report)
            assertTrue(statusResult == StatusCodes.OK) &&
            assertTrue(invocations.size == 1) &&
            assertTrue(invocations.head.serviceName == "test") &&
            assertTrue(invocations.head.labels == Map("a" -> "b")) &&
            assertTrue(
              invocations.head.mode == Profiler.ProfilingMode(
                Profiler.CpuProfilingMode.Cpu,
                alloc = true,
                locks = false
              )
            ) &&
            assertTrue(invocations.head.reportBody.sameElements(testReportBytes)) &&
            assertTrue(tempFileDeleted)
          }
        }
      },
      testM("get flame-graph") {
        routeTestkit.use { testkit =>
          import testkit._
          for {
            routes <- ZIO.service[Routes]

            query = Uri.Query(
              "service" -> "test-service",
              "mode" -> "cpu",
              "from" -> "-10m",
              "to" -> "now",
              "dc" -> "sas"
            )
            uri = Uri("/flame-graph").withQuery(query)
            unknownServiceUri = uri.withQuery(("service", "unknown") +: query.filter(_._1 != "service"))

            _ <- Reader(r =>
              ZIO.effectTotal {
                r.asInstanceOf[MockReader]
                  .mockData
                  .update(TestQuery, TestSamples)
              }
            )

            (resStatus, resBody, resContentType) = Get(uri) ~> seal(routes.routes) ~> check {
              (status, entityAs[String], contentType)
            }

            (unknownStatus, unknownBody, unknownContentType) = Get(unknownServiceUri) ~> seal(routes.routes) ~> check {
              (status, entityAs[String], contentType)
            }

          } yield {
            assertTrue(
              resStatus == StatusCodes.OK && resContentType == ContentTypes.`text/html(UTF-8)` &&
                resBody.contains("""f(0,0,5,3,'all')
                                   |f(1,0,5,0,'a')
                                   |f(2,0,1,0,'b')
                                   |f(2,1,4,0,'c')""".stripMargin)
            ) &&
            assertTrue(
              unknownStatus == StatusCodes.OK && unknownContentType == ContentTypes.`text/html(UTF-8)` &&
                unknownBody.contains("f(0,0,0,2,'all')")
            )
          }
        }
      }
    ).injectCustom(
      Routes.layer,
      ZLayer.succeed[Writer](new MockWriter),
      ZLayer.succeed[Reader](new MockReader),
      TestTracing.noOp
    )

}

package infra.profiler_collector.model.test

import infra.profiler_collector.model.Query
import zio.ZIO
import zio.test.AssertionResult.TraceResult
import zio.test._

import java.time.Instant
import java.time.temporal.ChronoUnit.{HOURS, MINUTES}

object QuerySpec extends DefaultRunnableSpec {
  val DefaultMap = Map("service" -> "test-service")

  val Now = Instant.EPOCH
  val DefaultQuery = Query(Now.minus(10, MINUTES), Now, "cpu", "test-service", Map.empty)

  def testCase(name: String, map: Map[String, String], expected: Query => Query) = {
    testM(name) {
      for {
        q <- ZIO.effect(Query.fromMap(map, Now))
        expectedQuery = expected(DefaultQuery)
      } yield {
        assertTrue(q == expectedQuery)
      }
    }
  }

  def testError(name: String, map: Map[String, String], checkError: Throwable => Assert) = {
    testM(name) {
      for {
        error <- ZIO.effect(Query.fromMap(map, Now)).either
      } yield {
        error match {
          case Left(error) => checkError(error)
          case Right(value) => BoolAlgebra.failure(TraceResult(Trace.fail(s"Expected Throwable, got $value")))
        }
      }
    }
  }

  def spec =
    suiteM("Query parsing")(zio.clock.instant.map { now =>
      Seq(
        testError("service is required", Map.empty, err => assertTrue(err.getMessage.contains("is required"))),
        testCase("parse service name", DefaultMap + ("service" -> "other"), _.copy(service = "other")),
        testCase("parse mode", DefaultMap + ("mode" -> "itimer"), _.copy(mode = "itimer")),
        testCase(
          "parse labels",
          DefaultMap ++ Map("dc" -> "sas", "layer" -> "prod"),
          _.copy(labels = Map("dc" -> "sas", "layer" -> "prod"))
        ),
        testCase(
          "parse relative instants",
          DefaultMap ++ Map("from" -> "-1h", "to" -> "-1m"),
          _.copy(from = Now.minus(1, HOURS), to = Now.minus(1, MINUTES))
        ),
        testCase(
          "parse absolute instants",
          DefaultMap ++ Map("from" -> "2021-07-15T14:35:57.128351Z", "to" -> "2022-07-15T14:35:57.128351Z"),
          _.copy(from = Instant.parse("2021-07-15T14:35:57.128351Z"), to = Instant.parse("2022-07-15T14:35:57.128351Z"))
        ),
        testError(
          "fail if `from` before `to`",
          DefaultMap ++ Map("from" -> "-1h", "to" -> "-2h"),
          err => assertTrue(err.getMessage.contains("should be before"))
        )
      )
    })
}

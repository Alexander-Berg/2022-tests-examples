package ru.yandex.realty.traffic.service.urls.generator

import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.traffic.model.RequestWithText
import ru.yandex.realty.traffic.service.urls.generator.RequestWithTextGenerator.RequestWithTextGenerator
import zio.RIO
import zio.test.Assertion._
import zio.test._

object GeneratorSpecCommon {

  def testGeneratorGenerateExpected(
    request: BySourceUrlsRequest
  )(expected: RequestWithText*): RIO[RequestWithTextGenerator, TestResult] =
    RequestWithTextGenerator
      .generate(request)
      .runCollect
      .map { actual =>
        if (expected.isEmpty) {
          assert(actual)(isEmpty)
        } else {
          expected
            .map { e =>
              assert(actual)(containsRequest(e))
            }
            .reduce(_ && _)
        }
      }

  def testNoGenerated(request: BySourceUrlsRequest): RIO[RequestWithTextGenerator, TestResult] =
    testGeneratorGenerateExpected(request)()

  private def containsRequest(expected: RequestWithText): Assertion[Iterable[RequestWithText]] =
    Assertion.assertion("containsRequest")(Render.param(expected)) { actual =>
      actual
        .find(_.routerRequest.key == expected.routerRequest.key)
        .exists(_.text == expected.text)
    }
}

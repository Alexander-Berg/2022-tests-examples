package ru.auto.comeback.model.test

import ru.auto.comeback.model.testkit.ComebackGen
import ru.auto.comeback.model.Converters.{comebackFromJsonString, comebackToJsonString}
import zio.test._

object JsonConverterTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("JsonConverterTest")(
    testM("converter does not change comeback") {
      check(
        ComebackGen.existingComebacksFromOneClient
      ) { case (_, comebacks) =>
        comebacks.foldLeft(assertCompletes)((accum, expected) => {
          val json = comebackToJsonString(expected)
          val actual = comebackFromJsonString(json)
          accum && assertTrue(expected == actual)
        })
      }
    }
  )
}

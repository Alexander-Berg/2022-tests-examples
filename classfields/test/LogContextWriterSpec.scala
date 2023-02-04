package common.zio.logging.test

import com.fasterxml.jackson.core.JsonFactory
import common.zio.logging.LogContext
import zio.ZIO
import zio.test._

import java.io.StringWriter
import java.time.Instant

object LogContextWriterSpec extends DefaultRunnableSpec {

  private val context = LogContext.of(
    "string" -> "string",
    "int" -> 42,
    "long" -> 42L,
    "float" -> 31.1f,
    "double" -> 31.1d,
    "instant" -> Instant.EPOCH,
    "optional" -> None,
    "optional2" -> Some(2),
    "iterable" -> List(1, 2, 3),
    "object" -> LogContext.of(
      "inner" -> "2",
      "inner-list" -> Vector("1", "2", "3")
    )
  )

  private val expected =
    """{"float":31.1,
      |"optional":null,
      |"string":"string",
      |"object":{"inner":"2","inner-list":["1","2","3"]},
      |"double":31.1,
      |"long":42,
      |"instant":"1970-01-01T00:00:00Z",
      |"int":42,
      |"optional2":2,
      |"iterable":[1,2,3]
      |}""".stripMargin.replace("\n", "")

  def spec = suite("LogContextWriter")(
    testM("write json")(
      for {
        stringWriter <- ZIO.effectTotal(new StringWriter())
        jsonGenerator <- ZIO.effectTotal(new JsonFactory().createGenerator(stringWriter))
        _ = context.writeTo(jsonGenerator)
        _ = jsonGenerator.close()
      } yield assertTrue(stringWriter.toString == expected)
    )
  )

}

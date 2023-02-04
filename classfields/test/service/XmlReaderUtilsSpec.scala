package ru.vertistraf.common.service

import com.lucidchart.open.xtract._
import zio.{Task, ZIO}
import zio.test._
import zio.test.Assertion._

import scala.xml.XML

object XmlReaderUtilsSpec extends DefaultRunnableSpec {

  import ru.vertistraf.common.util.xml.XmlReaderUtils._

  private def liftError[A](f: => Task[ParseResult[A]]): Task[A] =
    f >>= {
      case PartialParseSuccess(get, errors) =>
        if (errors.isEmpty) Task.succeed(get) else ZIO.fail(new RuntimeException(errors.mkString(", ")))
      case ParseFailure(errors) => ZIO.fail(new RuntimeException(errors.mkString(", ")))
      case ParseSuccess(get) => ZIO.succeed(get)
    }

  private def correctlyReadOneSpec() = {
    val xml =
      XML.loadString(
        """<root>
          |<empty></empty>
          |<empty1></empty1>
          |<empty1>e1</empty1>
          |<nonEmpty>str</nonEmpty>
          |</root>""".stripMargin
      )

    def readOne(name: String) =
      liftError {
        Task.effect {
          (__ \ name).readExactlyOne[String].read(xml)
        }
      }

    def shouldFailOn(name: String) =
      testM(s"should fail on `$name` tag") {
        assertM {
          readOne(name).run
        }(fails(anything))
      }

    def shouldRead(name: String, value: String) =
      testM(s"should read `$value` from $name") {
        readOne(name).map(actual => assertTrue(actual == value))
      }

    suite("should correctly read exactly one")(
      shouldFailOn("empty1"),
      shouldFailOn("empty2"),
      shouldRead("empty", ""),
      shouldRead("nonEmpty", "str")
    )
  }

  private def returnChildValuesSpec() = {
    val xml = XML.loadString(
      """<root>
        |<empty></empty>
        |<a>1</a>
        |<a>2</a>
        |<b>3</b>
        |</root>""".stripMargin
    )

    def testReader[A](reader: String => XmlReader[A], tag: String, expected: A) =
      testM(s"correctly return `$expected` for $tag") {
        liftError {
          Task.effect {
            reader(tag).read(xml)
          }
        }.map(actual => assertTrue(actual == expected))
      }

    def shouldFail[A](reader: String => XmlReader[A], tag: String) =
      testM(s"should fail for $tag") {
        assertM {
          liftError {
            Task.effect {
              reader(tag).read(xml)
            }
          }.run
        }(fails(anything))
      }

    suite("correctly return child values")(
      testReader(childTagValue[String], "empty", ""),
      testReader(childTagValueOpt[String], "empty", Some("")),
      testReader(childTagValueOpt[String], "missing", None),
      testReader(childNonEmptySeq[String], "empty", Seq("")),
      testReader(childNonEmptySeq[String], "a", Seq("1", "2")),
      testReader(childNonEmptySeq[String], "b", Seq("3")),
      testReader(childSeq[String], "missed", Seq.empty),
      shouldFail(childTagValue[String], "missing"),
      shouldFail(childNonEmptySeq[String], "missing2")
    )

  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("XmlReaderUtils")(
      correctlyReadOneSpec(),
      returnChildValuesSpec()
    )
}

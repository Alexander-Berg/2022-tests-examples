package ru.yandex.vertis.general.feed.parser.testkit

import common.zio.files.ZFiles
import general.feed.transformer.{RawAddress, RawAttribute, RawGeoPoint, RawImage, RawOffer, RawSeller}
import ru.yandex.vertis.general.feed.parser.core.XmlParser.Parsed
import ru.yandex.vertis.general.feed.parser.core._
import zio.blocking.Blocking
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZManaged}
import com.google.devtools.build.runfiles.Runfiles

import java.io.File
import scala.reflect.{classTag, ClassTag}
import scala.sys.process._

object XmlParserTestkit {

  def isParsingError(assertion: Assertion[ParsingError]): Assertion[Parsed[_]] =
    isCase("ParsingError", _.left.toOption, assertion)

  def isParsingSuccess[T](assertion: Assertion[T]): Assertion[Parsed[T]] =
    isCase("ParsingSuccess", _.toOption, assertion)

  def withLocation(line: Int, position: Int): Assertion[ParsingError] =
    hasField("location", _.location, isSome(equalTo(FileLocation(line, position))))

  private def isParsingErrorOfType[T <: ParsingError: ClassTag](assertion: Assertion[T]): Assertion[ParsingError] =
    isCase(classTag[T].runtimeClass.getSimpleName, e => classTag[T].unapply(e.unwrap), assertion)

  def isInvalidXml(assertion: Assertion[String]): Assertion[ParsingError] =
    isParsingErrorOfType[InvalidXml](hasField("message", _.message, assertion))

  def isInvalidXml(message: String): Assertion[ParsingError] = isInvalidXml(equalTo(message): Assertion[String])

  def isMissingTag(tag: String): Assertion[ParsingError] =
    isParsingErrorOfType[MissingTag](hasField("tag", _.tag, equalTo(tag)))

  def hasImages(assertion: Assertion[Seq[RawImage]]): Assertion[RawOffer] = {
    hasField("images", _.images, assertion)
  }

  def hasGeoPoint(assertion: Assertion[Option[RawGeoPoint]]): Assertion[RawOffer] = {
    hasField("geopoint", _.seller.flatMap(_.locations.head.asMessage.sealedValue.geopoint), assertion)
  }

  def hasAddress(assertion: Assertion[Option[RawAddress]]): Assertion[RawOffer] = {
    hasField("address", _.seller.flatMap(_.locations.head.asMessage.sealedValue.address), assertion)
  }

  def hasSeller(assertion: Assertion[Option[RawSeller]]): Assertion[RawOffer] = {
    hasField("seller", _.seller, assertion)
  }

  def hasAttribute(assertion: Assertion[Seq[RawAttribute]]): Assertion[RawOffer] = {
    hasField("attributes", _.attributes, assertion)
  }

  def hasUrl(assertion: Assertion[String]): Assertion[RawImage] = {
    hasField("url", _.url, assertion)
  }

  def hasElements[T](assertions: Assertion[T]*): Assertion[Seq[T]] = {
    assertionDirect("hasElements")() { list =>
      BoolAlgebra.all(
        hasSize(equalTo(assertions.size)).run(list),
        assertions.zip(list).map { case (as, el) =>
          as.run(el)
        }: _*
      )
    }
  }

  def assertList[T](list: List[T])(assertions: Assertion[T]*): TestResult = {
    BoolAlgebra.all(
      assert(list)(hasSize(equalTo(assertions.size))),
      list.zip(assertions).zipWithIndex.map { case ((result, assertion), idx) =>
        assert(result)(assertion).map(_.label(s"at index $idx"))
      }: _*
    )
  }

  implicit class RichParserTestkit[T](parser: XmlParser[T]) {

    def assertParsing(xml: String)(assertions: Assertion[T]*): TestResult = {
      val list = parser.parse(xml).toList
      assertList(list)(assertions: _*)
    }

    def checkSampleFromRunfiles(
        name: String
      )(implicit ev: T <:< Parsed[RawOffer]): ZIO[Blocking, Throwable, TestResult] = {
      (for {
        r <- ZManaged.effectTotal(Runfiles.create())
        workspace = sys.env("TEST_WORKSPACE")
        xml = new File(r.rlocation(workspace + "/" + name))
        expected = new File(r.rlocation(workspace + "/" + name + ".expected"))

        list = parser.parse(xml).toList
        text = list
          .map(t =>
            ev(t) match {
              case Left(WithMeta(error, location, id)) =>
                Seq(
                  error.toString,
                  location.fold("")(l => s"at ${l.line}:${l.position}"),
                  id.fold("")(id => "external_id = " + id)
                ).filter(_.nonEmpty).mkString("\n")
              case Left(error) => error.toString
              case Right(offer) => offer.toProtoString
            }
          )
          .mkString("\n---------------\n")

        producedFile <- ZFiles.makeTempFile("produced", ".txt")
        _ <- ZFiles.writeStreamTo(producedFile, ZStream.fromIterable(text.getBytes("UTF-8"))).toManaged_

        out <- zio.blocking
          .effectBlocking(s"diff --text ${producedFile.getAbsolutePath} ${expected.getAbsolutePath}".lazyLines_!)
          .toManaged_
        diff = out.mkString("\n")

        target = sys.env("TEST_TARGET")

        isUnderRun = sys.env.contains("BUILD_WORKSPACE_DIRECTORY")
        _ <- ZManaged.when(isUnderRun) {
          ZFiles.writeStreamTo(expected, ZStream.fromIterable(text.getBytes("UTF-8"))).toManaged_
        }

      } yield assert(diff)(isEmptyString).map(
        _.label(s"\nYou can refresh expected data with command:\n  bazel run $target\n")
      )).useNow
    }

    def checkSamples(names: String*)(implicit ev: T <:< Parsed[RawOffer]): ZIO[Blocking, Throwable, TestResult] = {
      names
        .map { path =>
          checkSampleFromRunfiles(path).map(_.map(_.label(path)))
        }
        .reduce { (a, b) =>
          a.zipWith(b)(_.&&(_))
        }
    }
  }
}

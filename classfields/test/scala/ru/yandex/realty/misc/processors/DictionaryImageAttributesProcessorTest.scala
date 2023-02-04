package ru.yandex.realty.misc.processors

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalactic.Equality
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.verba2.model.attribute.{Attribute, ImageAttribute, StringAttribute}
import ru.yandex.verba2.model.{Dictionary, Term}

import java.util
import java.util.Collections
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DictionaryImageAttributesProcessorTest extends SpecBase with PropertyChecks {

  implicit val imageEq: Equality[ImageAttribute] =
    (a: ImageAttribute, b: Any) =>
      b match {
        case p: ImageAttribute =>
          a.getOrigUrl == p.getOrigUrl &&
            a.getThumbnailUrl == p.getThumbnailUrl &&
            a.getUrl == p.getUrl &&
            a.getImageId == p.getImageId &&
            a.getName == p.getName
        case _ => false
      }

  "PreprocessedImagesVerbaDictionaryGetter" should {
    val data =
      Table(
        ("desc", "given", "expected"),
        (
          "case with http and 90",
          new ImageAttribute("image", 1L, "http://x:90/img", "http://x:90/thumb", "http://x:90/pic"),
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic")
        ),
        (
          "case with task example",
          new ImageAttribute(
            "image",
            1L,
            "http://avatars.mds.yandex.net:80/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/optimize",
            "http://avatars.mds.yandex.net:80/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/thumbnail",
            "http://avatars.mds.yandex.net:80/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/orig"
          ),
          new ImageAttribute(
            "image",
            1L,
            "//avatars.mds.yandex.net/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/optimize",
            "//avatars.mds.yandex.net/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/thumbnail",
            "//avatars.mds.yandex.net/get-verba/1030388/2a0000016f56e0f961189547f392976eaf12/orig"
          )
        ),
        (
          "case with different protocols and ports",
          new ImageAttribute("image", 1L, "https://x:8080/img", "ftp://x:32432/thumb", "xyz://x:1025/pic"),
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic")
        ),
        (
          "case with no protocol",
          new ImageAttribute("image", 1L, "//x:8090/img", "//x:8090/thumb", "//x:8090/pic"),
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic")
        ),
        (
          "case with no port",
          new ImageAttribute("image", 1L, "http://x/img", "http://x/thumb", "http://x/pic"),
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic")
        ),
        (
          "case with no protocol and port",
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic"),
          new ImageAttribute("image", 1L, "//x/img", "//x/thumb", "//x/pic")
        ),
        (
          "case with null image urls",
          new ImageAttribute("image", 1L, null, null, null),
          new ImageAttribute("image", 1L, null, null, null)
        )
      )

    forAll(data) { (desc: String, given: Attribute, expected: ImageAttribute) =>
      "return image attributes without protocol and port for " + desc in {

        val imgAttribute = new ImageAttribute("image2", 2L, null, "//sss.com/img", "http://ddf:80/pic")
        val attribute = new StringAttribute("str", List("x", "y", "z").asJava)
        var term = new Term(1L, "term", "name", 1L, "path", new DateTime(), new DateTime())
        term = new Term(term, List(given, attribute, imgAttribute).asJava, Collections.emptyList())
        val terms: util.List[Term] = List(term).asJava
        val d = new Dictionary(1L, 1L, "correct code", "name", "path", terms)

        val imageAttribute = DictionaryImageAttributesProcessor
          .updateImageUrls(d)
          .getTerms
          .get(0)
          .getAttributes
          .asScala
          .find(a => a.getName == "image")
          .map(a => a.asInstanceOf[ImageAttribute])
          .orNull

        imageAttribute should equal(expected)
      }
    }
  }
}

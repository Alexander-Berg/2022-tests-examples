package ru.yandex.realty.cadastr.parser

import com.google.protobuf.Timestamp
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.cadastr.proto.model.excerpt.Excerpt
import ru.yandex.vertis.protobuf.BasicProtoFormats

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class FlatRightMovementExcerptParserSpec extends SpecBase with BasicProtoFormats {

  def parseExcerptFromXml(filepath: String): Excerpt = {
    val is = getClass.getResourceAsStream(filepath)
    val xmlBytes = Stream.continually(is.read).takeWhile(_ > 0).map(_.toByte).toArray
    FlatRightMovementExcerptParser.parseExcerpt(xmlBytes)
  }

  def format(timestamp: Timestamp, format: String = "dd.MM.YYYY"): String =
    DateTimeFormat.read(timestamp).toString(format)

  "FlatRightMovementExcerptParser" should {

    "parse simple excerpt" in {
      val excerpt = parseExcerptFromXml("/excerpts/right-movement/right-movement-simple.xml")

      excerpt.getNumber shouldBe "99/2019/295523632"
      format(excerpt.getDate) shouldBe "15.11.2019"

      assertEquals(excerpt.getFlatRightMovementExcerpt.getObjectDesc.getIdObject, "7157766465")
      assertEquals(excerpt.getFlatRightMovementExcerpt.getObjectDesc.getCadastralNumber, "77:05:0007006:7315")
      assertEquals(excerpt.getFlatRightMovementExcerpt.getObjectDesc.getArea.getArea, 53.5, 0.1)

      assertEquals(excerpt.getFlatRightMovementExcerpt.getOwnersCount, 5)
      assertEquals(excerpt.getFlatRightMovementExcerpt.getOwnersList.asScala.exists(_.getOwnerNumber == 1), true)
      assertEquals(
        excerpt.getFlatRightMovementExcerpt.getOwnersList.asScala.filter((_.getOwnerNumber == 1)).head.getIdSubject,
        "20837441111"
      )
      assertEquals(
        excerpt.getFlatRightMovementExcerpt.getOwnersList.asScala
          .filter((_.getOwnerNumber == 1))
          .head
          .getGovernance
          .getContent,
        "город Москва"
      )

      assertEquals(excerpt.getFlatRightMovementExcerpt.getRegistrationsCount, 4)
      assertEquals(
        excerpt.getFlatRightMovementExcerpt.getRegistrationsList.asScala.exists(_.getRegistrNumber == 1),
        true
      )
      assertEquals(
        excerpt.getFlatRightMovementExcerpt.getRegistrationsList.asScala
          .filter(_.getRegistrNumber == 1)
          .head
          .getIdRecord,
        "71277579222"
      )
      assertEquals(
        excerpt.getFlatRightMovementExcerpt.getRegistrationsList.asScala
          .filter(_.getRegistrNumber == 1)
          .head
          .getRegNumber,
        "77-77-09/222/2009-66"
      )
    }

    "parse empty excerpt" in {
      val excerpt = parseExcerptFromXml("/excerpts/right-movement/right-movement-empty.xml")

      excerpt.getNumber shouldBe "99/2019/305373834"
      format(excerpt.getDate) shouldBe "27.12.2019"

      assertEquals(excerpt.getFlatRightMovementExcerpt.getObjectDesc.getCadastralNumber, "")
    }
  }

}

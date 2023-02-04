package ru.yandex.realty.cadastr.parser

import com.google.protobuf.Timestamp
import org.junit.Assert.{assertFalse, assertTrue}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.cadastr.proto.model.excerpt.Excerpt
import ru.yandex.vertis.protobuf.BasicProtoFormats

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class LandLotExcerptParserSpec extends SpecBase with BasicProtoFormats {

  def parseExcerptFromXml(filepath: String): Excerpt = {
    val is = getClass.getResourceAsStream(filepath)
    val xmlBytes = Stream.continually(is.read).takeWhile(_ > 0).map(_.toByte).toArray
    LandLotExcerptParser.parseExcerpt(xmlBytes)
  }

  def format(timestamp: Timestamp, format: String = "dd.MM.YYYY"): String =
    DateTimeFormat.read(timestamp).toString(format)

  "LandLotExcerptParser" should {
    "parse simple excerpt" in {
      val excerpt = parseExcerptFromXml("/excerpts/landlot/garden-quarter.xml")
      val shareOwners = excerpt.getLandLotRightsExcerpt.getShareOwnersRight.getEncumbrancesList.asScala

      excerpt.getNumber shouldBe "99/2020/314674240"
      format(excerpt.getDate) shouldBe "20.02.2020"

      shareOwners.foreach { shareOwner =>
        if (shareOwner.hasMortgageEndDate && shareOwner.getEncumbrance.hasRegDate) {
          assertTrue(shareOwner.getMortgageEndDate.getSeconds > shareOwner.getEncumbrance.getRegDate.getSeconds)
        }

        assertFalse(ParserUtils.WrongLandLotTypes.contains(shareOwner.getType))
        assertTrue(shareOwner.getEncumbrance.getIdRecord.nonEmpty)
        assertTrue(shareOwner.hasEncumbrance)
        assertTrue(shareOwner.getIsMortgage)
      }
    }
  }
}

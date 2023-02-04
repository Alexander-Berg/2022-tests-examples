package ru.yandex.realty.cadastr.parser

import com.google.protobuf.Timestamp
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.cadastr.proto.model.excerpt.Excerpt
import ru.yandex.realty.cadastr.proto.registry.EncumbranceType
import ru.yandex.realty.cadastr.proto.registry.Owner.OwnerCase
import ru.yandex.vertis.protobuf.BasicProtoFormats

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class FlatRightsExcerptParserSpec extends SpecBase with BasicProtoFormats {

  def parseExcerptFromXml(filepath: String): Excerpt = {
    val is = getClass.getResourceAsStream(filepath)
    val xmlBytes = Stream.continually(is.read).takeWhile(_ > 0).map(_.toByte).toArray
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def format(timestamp: Timestamp, format: String = "dd.MM.YYYY"): String =
    DateTimeFormat.read(timestamp).toString(format)

  "FlatRightsExcerptParser" should {

    "parse simple excerpt" in {
      val excerpt = parseExcerptFromXml("/excerpts/rights/rights-simple.xml")
      val flat = excerpt.getFlatRightsExcerpt.getFlat

      excerpt.getNumber shouldBe "99/2019/295647154"
      format(excerpt.getDate) shouldBe "15.11.2019"

      assertEquals(2, flat.getRights(0).getOwnersCount)
      assertEquals("001003000000", flat.getRights(0).getType)
      assertEquals(
        "Иванович",
        flat
          .getRights(0)
          .getOwnersList
          .asScala
          .filter(_.getPerson.getFamilyName == "Иванов")
          .head
          .getPerson
          .getPatronymic
      )
      assertEquals("Чертановская", flat.getAddress.getStreet.getName)
      assertEquals("2", flat.getLevelsList.asScala.head.getNumberString)
    }

    "parse empty excerpt" in {
      val excerpt = parseExcerptFromXml("/excerpts/rights/rights-empty.xml")
      excerpt.getNumber shouldBe "99/2019/305378592"
      format(excerpt.getDate) shouldBe "27.12.2019"
      assertEquals(excerpt.getFlatRightsExcerpt.getFlat.getCadastralNumber, "77:05:0007006:7243")
      assertEquals(excerpt.getFlatRightsExcerpt.getExtractObject.getRightsCount, 0)
    }

    "parse excerpt with mortgage encumbrance" in {
      val excerpt = parseExcerptFromXml("/excerpts/rights/rights-mortgage.xml")
      val extractionObject = excerpt.getFlatRightsExcerpt.getExtractObject
      val rights = extractionObject.getRightsList.asScala
      val encumbrances = extractionObject.getEncumbrancesList.asScala

      excerpt.getNumber shouldBe "99/2019/300859413"
      format(excerpt.getDate) shouldBe "10.12.2019"
      assertEquals(1, rights.size)
      assertEquals(1, rights.head.getOwnersList.asScala.size)
      assertEquals("Василий", rights.head.getOwnersList.asScala.head.getPerson.getFirstName)

      assertEquals(1, encumbrances.size)
      assertEquals(EncumbranceType.ENCUMBRANCE_TYPE_MORTGAGE_BY_FORCE_OF_LAW, encumbrances.head.getType)
      assertEquals(1, encumbrances.head.getOwnersList.size)
      assertEquals(OwnerCase.ORGANIZATION, encumbrances.head.getOwnersList.asScala.head.getOwnerCase)
      assertEquals("7705454547", encumbrances.head.getOwnersList.asScala.head.getOrganization.getInn)
    }

    "parse excerpt with string in floor number" in {
      val excerpt = parseExcerptFromXml("/excerpts/rights/rights-string-floor.xml")
      excerpt.getNumber shouldBe "99/2019/295647154"
      format(excerpt.getDate) shouldBe "15.11.2019"
      assertEquals("чердак", excerpt.getFlatRightsExcerpt.getFlat.getLevelsList.asScala.head.getNumberString)
    }

    "parse excerpt with no registration and no owner" in {
      val excerpt = parseExcerptFromXml("/excerpts/rights/rights-no-registration.xml")

      excerpt.getNumber shouldBe "99/2019/305620231"
      format(excerpt.getDate) shouldBe "29.12.2019"
      // In xml we have 2 rights, but second one has NoOwner and NoRegistration fields,
      // this right shouldn't be passed into the report
      assertEquals(1, excerpt.getFlatRightsExcerpt.getExtractObject.getRightsCount)

      // The right with NoRegistration field has encumbrance and this encumbrance should be passed into the report,
      // despite the fact the right is not passed
      assertEquals(1, excerpt.getFlatRightsExcerpt.getExtractObject.getEncumbrancesCount)
    }
  }
}

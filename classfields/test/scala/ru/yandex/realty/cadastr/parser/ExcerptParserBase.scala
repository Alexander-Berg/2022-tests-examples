package ru.yandex.realty.cadastr.parser

import org.scalatest.WordSpecLike
import ru.yandex.realty.cadastr.proto.model.excerpt.Excerpt

trait ExcerptParserBase extends WordSpecLike {

  private def getExcerptBytes(filepath: String): Array[Byte] = {
    val is = getClass.getResourceAsStream(filepath)
    Stream.continually(is.read).takeWhile(_ > 0).map(_.toByte).toArray
  }

  def getSimpleRightsExcerpt: Excerpt = {
    val filepath = "/excerpts/rights/rights-simple.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def getSimpleRightMovementExcerpt: Excerpt = {
    val filepath = "/excerpts/right-movement/right-movement-simple.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightMovementExcerptParser.parseExcerpt(xmlBytes)
  }

  def getEmptyRightsExcerpt: Excerpt = {
    val filepath = "/excerpts/rights/rights-empty.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def getEmptyRightMovementExcerpt: Excerpt = {
    val filepath = "/excerpts/right-movement/right-movement-empty.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightMovementExcerptParser.parseExcerpt(xmlBytes)
  }

  def getMortgageRightsExcerpt: Excerpt = {
    val filepath = "/excerpts/rights/rights-mortgage.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def getStringFloorRightsExcerpt: Excerpt = {
    val filepath = "/excerpts/rights/rights-string-floor.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def getNoRegistrationRightsExcerpt: Excerpt = {
    val filepath = "/excerpts/rights/rights-no-registration.xml"
    val xmlBytes = getExcerptBytes(filepath)
    FlatRightsExcerptParser.parseExcerpt(xmlBytes)
  }

  def getSimpleLandLotExcerpt: Excerpt = {
    val filepath = "/excerpts/landlot/garden-quarter.xml"
    val xmlBytes = getExcerptBytes(filepath)
    LandLotExcerptParser.parseExcerpt(xmlBytes)
  }
}

package ru.yandex.vertis.telepony.util.meta

import com.google.i18n.phonenumbers.Phonemetadata
import ru.yandex.vertis.telepony.util.BooleanUtil.onlyIf
import ru.yandex.vertis.util.phone.PhoneNumberUtils.Util

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import io.circe.syntax._
import io.circe.generic.auto._

import scala.jdk.CollectionConverters._

// Use ONLY for updating regexps for regions
object RegionMetaInfoUpdater extends App {

  private def toJson(regions: Set[String]): String = {
    val metas = regions.map(parse)
    metas.toSeq.sortBy(_.region).asJson.toString
  }

  private def parse(region: String): RegionMetaInfo = {
    val meta = getMetadataForRegion(region)
    val patterns = extractRegexPatterns(meta)
    RegionMetaInfo(region, meta.getCountryCode, patterns)
  }

  private val NoAnswerPatterns: Set[String] = Set("N/A", "NA")

  private def extractRegexPatterns(meta: Phonemetadata.PhoneMetadata): Set[String] = {

    def extractNationalPattern(desc: Phonemetadata.PhoneNumberDesc): Option[String] = {
      onlyIf(desc.hasNationalNumberPattern)(desc.getNationalNumberPattern)
        .filterNot(NoAnswerPatterns.contains)
        .map(_.replace("(?:", "("))
    }

    Seq(
      onlyIf(meta.hasPremiumRate)(meta.getPremiumRate),
      onlyIf(meta.hasTollFree)(meta.getTollFree),
      onlyIf(meta.hasSharedCost)(meta.getSharedCost),
      onlyIf(meta.hasVoip)(meta.getVoip),
      onlyIf(meta.hasPersonalNumber)(meta.getPersonalNumber),
      onlyIf(meta.hasPager)(meta.getPager),
      onlyIf(meta.hasUan)(meta.getUan),
      onlyIf(meta.hasFixedLine)(meta.getFixedLine),
      onlyIf(meta.hasMobile)(meta.getMobile)
    ).flatMap(_.flatMap(extractNationalPattern)).toSet
  }

  private def getMetadataForRegion(region: String): Phonemetadata.PhoneMetadata = {
    callPrivate[Phonemetadata.PhoneMetadata](Util, "getMetadataForRegion", region)
  }

  private def callPrivate[T](obj: AnyRef, methodName: String, parameters: AnyRef*): T = {
    val parameterTypes = parameters.map(_.getClass())
    val method = obj.getClass.getDeclaredMethod(methodName, parameterTypes: _*)
    method.setAccessible(true)
    method.invoke(obj, parameters: _*).asInstanceOf[T]
  }

  // numbers generated is not parsed correctly by regexps for this regions
  private val BadRegions = Set("BY", "DE", "AT")
  private val PossibleForGenRegions = Util.getSupportedRegions.asScala.diff(BadRegions)

  val fileBody = toJson(PossibleForGenRegions.toSet)

  val pathToFile = "/Users/tolmach/Work/telepony/telepony-dao/src/main/resources/meta/region_meta_info.json"
  val filePath = Paths.get(pathToFile)
  Files.write(filePath, fileBody.getBytes(StandardCharsets.UTF_8))

}

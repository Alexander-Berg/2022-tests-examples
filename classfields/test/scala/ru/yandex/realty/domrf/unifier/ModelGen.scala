package ru.yandex.realty.domrf.unifier

import org.scalacheck.Gen
import ru.yandex.realty.domrf.client.model.{
  DomrfAddress,
  DomrfBuildPermitInfoList,
  DomrfCodeName,
  DomrfComplex,
  DomrfComplexAddress,
  DomrfConstructionObjectBase,
  DomrfConstructionObjectBaseList,
  DomrfConstructionObjectExtended,
  DomrfCoordinates,
  DomrfFileInfo,
  DomrfObjectPhoto,
  DomrfPremises,
  DomrfProjectDeclaration,
  DomrfRegion
}
import ru.yandex.realty.domrf.model.{RawComplex, RawConstructionObject}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.util.geometry.{BoundingBoxUtil, BoundingBoxUtils}
import ru.yandex.vertis.generators.BasicGenerators

import java.time.{Duration, Instant}
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

object ModelGen extends BasicGenerators {

  private val uniqId = new AtomicLong(42)

  private def uniqIdFunction(arg: AnyVal): Long = {
    uniqId.getAndIncrement()
  }

  def UniqIdGen: Gen[Long] = Gen.resultOf(uniqIdFunction _)

  lazy val RawComplexGen: Gen[RawComplex] = {
    for {
      complex <- DomrfComplexGen
      objectsCount <- Gen.chooseNum[Int](1, 20)
      coordinates = (1 to objectsCount).map(shiftCoordinates(_, complex.coordinates.get))
      objects = coordinates.map(
        c => rawConstructionObjectGen(complex.complexId, complex.developerId, complex.groupId, c).next
      )
    } yield RawComplex(complex, objects)
  }

  private def shiftCoordinates(shift: Int, p: DomrfCoordinates): DomrfCoordinates = {
    DomrfCoordinates(p.latitude + (shift * 1000 * BoundingBoxUtil.METER_BY_LATITUDE), p.longitude)
  }

  def rawConstructionObjectGen(
    complexId: Long,
    developerId: Long,
    groupId: Option[Long],
    geoPoint: DomrfCoordinates
  ): Gen[RawConstructionObject] = {
    for {
      objectExt <- domrfConstructionObjectExtendedGen(complexId, developerId, groupId, geoPoint)
      photos <- Gen.listOf(DomrfObjectPhotoGen)
    } yield RawConstructionObject(objectExt, photos)
  }

  lazy val DomrfComplexGen: Gen[DomrfComplex] = {
    for {
      complexId <- UniqIdGen
      shortName <- Gen.option(Gen.alphaStr)
      fullName <- Gen.alphaStr
      coordinates <- DomrfCoordinatesGen
      developerId <- Gen.posNum[Long]
      groupId <- Gen.option(Gen.posNum[Long])
      constructionObjects <- Gen.listOf(DomrfConstructionObjectBaseGen)
    } yield DomrfComplex(
      complexId,
      shortName,
      fullName,
      Some(coordinates),
      developerId,
      groupId,
      if (constructionObjects.nonEmpty) Some(DomrfConstructionObjectBaseList(constructionObjects)) else None
    )
  }

  lazy val DomrfCoordinatesGen: Gen[DomrfCoordinates] = {
    for {
      lat <- Gen.chooseNum[Double](50, 60)
      lon <- Gen.chooseNum[Double](30, 40)
    } yield DomrfCoordinates(lat, lon)
  }

  lazy val DomrfConstructionObjectBaseGen: Gen[DomrfConstructionObjectBase] = {
    for {
      objectId <- UniqIdGen
      titleName <- Gen.option(Gen.alphaStr)
      address <- DomrfComplexAddressGen
    } yield DomrfConstructionObjectBase(objectId, titleName, address)

  }

  lazy val DomrfComplexAddressGen: Gen[DomrfComplexAddress] = {
    for {
      addressString <- Gen.option(Gen.alphaStr)
      address <- DomrfAddressGen
    } yield DomrfComplexAddress(addressString, address)

  }

  lazy val DomrfAddressGen: Gen[DomrfAddress] = {
    for {
      region <- DomrfRegionGen
      district <- Gen.option(Gen.alphaStr)
      localityType <- Gen.option(Gen.alphaStr)
      localityName <- Gen.option(Gen.alphaStr)
      streetType <- Gen.option(Gen.alphaStr)
      streetName <- Gen.option(Gen.alphaStr)
      house <- Gen.option(Gen.alphaStr)
      letter <- Gen.option(Gen.alphaStr)
      unit <- Gen.option(Gen.alphaStr)
      building <- Gen.option(Gen.alphaStr)
      domain <- Gen.option(Gen.alphaStr)
      info <- Gen.option(Gen.alphaStr)
    } yield DomrfAddress(
      region,
      district,
      localityType,
      localityName,
      streetType,
      streetName,
      house,
      letter,
      unit,
      building,
      domain,
      info
    )

  }

  lazy val DomrfRegionGen: Gen[DomrfRegion] = {
    for {
      code <- Gen.alphaStr
      name <- Gen.alphaStr
    } yield DomrfRegion(code, name)
  }

  lazy val DomrfCodeNameGen: Gen[DomrfCodeName] = {
    for {
      code <- Gen.alphaStr
      name <- Gen.alphaStr
    } yield DomrfCodeName(code, name)
  }

  def domrfConstructionObjectExtendedGen(
    complexId: Long,
    developerId: Long,
    groupId: Option[Long],
    coordinates: DomrfCoordinates
  ): Gen[DomrfConstructionObjectExtended] = {
    for {
      objectId <- UniqIdGen
      name <- Gen.option(Gen.alphaStr)
      complexAddress <- DomrfComplexAddressGen
      region <- DomrfRegionGen
      objectStatus <- DomrfCodeNameGen
      objectType <- DomrfCodeNameGen
      purpose <- DomrfCodeNameGen
    } yield DomrfConstructionObjectExtended(
      objectId = objectId,
      titleName = name,
      address = complexAddress,
      coordinates = Some(coordinates),
      buildPermitInfo = DomrfBuildPermitInfoList(None),
      projectDeclaration = DomrfProjectDeclaration(None, None),
      buildRegion = region,
      objectStatus = objectStatus,
      storeys = None,
      apartmentsCount = None,
      livingArea = None,
      commissioningPlanDate = None,
      developerId = developerId,
      complexId = Some(complexId),
      groupId = groupId,
      objectType = objectType,
      propertyClass = None,
      wallMaterial = None,
      decoration = None,
      isFreePlan = None,
      carPlaceCount = None,
      objectsRenders = None,
      landPlots = None,
      projectCost = None,
      purpose = purpose,
      floorMaterial = None,
      premises = DomrfPremises(None, None, None, None)
    )
  }

  lazy val DomrfObjectPhotoGen: Gen[DomrfObjectPhoto] = {
    for {
      order <- Gen.posNum[Int]
      date <- DateGen
      fileInfo <- DomrfFileInfoGen
    } yield DomrfObjectPhoto(order, date, fileInfo)
  }

  lazy val DomrfFileInfoGen: Gen[DomrfFileInfo] = {
    for {
      fileId <- Gen.alphaStr
      name <- Gen.alphaStr
      extension <- Gen.alphaStr
      size <- Gen.option(Gen.posNum[Long])
      date <- Gen.option(DateGen)
    } yield DomrfFileInfo(fileId, name, extension, size, date)
  }

  lazy val InstantGen: Gen[Instant] = Gen
    .choose(
      min = Instant.now().minus(Duration.ofDays(365)).toEpochMilli,
      max = Instant.now().toEpochMilli
    )
    .map(Instant.ofEpochMilli)

  lazy val DateGen: Gen[Date] = InstantGen.map(i => Date.from(i))
}

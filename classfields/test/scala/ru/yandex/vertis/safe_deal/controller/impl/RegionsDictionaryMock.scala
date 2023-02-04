package ru.yandex.vertis.safe_deal.controller.impl

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.zio_baker.geobase.{Region, RegionTypes}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.GeobaseId
import ru.yandex.vertis.safe_deal.dictionary.RegionsDictionary
import ru.yandex.vertis.safe_deal.dictionary.RegionsDictionary.NotFound
import zio.{IO, Task, ZIO}

class RegionsDictionaryMock() extends RegionsDictionary.Service {
  import RegionsDictionaryMock._

  override def getParentRegions(geobaseIds: Seq[GeobaseId]): Task[Seq[Region]] =
    Task.die(new UnsupportedOperationException("Calling a stub method"))

  override def getRegions(geobaseIds: Seq[GeobaseId]): IO[NotFound, Seq[Region]] =
    Task.die(new UnsupportedOperationException("Calling a stub method"))

  override def getRegion(geobaseId: GeobaseId): IO[NotFound, Region] =
    regionsMap.get(geobaseId) match {
      case Some(region) => ZIO.succeed(region)
      case None => ZIO.fail(NotFound(geobaseId))
    }

  override def getChildRegions(geobaseId: GeobaseId): Task[Seq[Region]] =
    Task.die(new UnsupportedOperationException("Calling a stub method"))
}

object RegionsDictionaryMock {
  private val emptyZeroId = 0L.taggedWith[zio_baker.Tag.GeobaseId]

  private val emptyZeroRegion = Region(
    id = emptyZeroId,
    ruName = "None",
    `type` = RegionTypes.Other,
    latitude = 0,
    longitude = 0,
    tzOffset = 0,
    chiefRegionId = emptyZeroId,
    parentId = emptyZeroId
  )

  private val spb = emptyZeroRegion.copy(
    id = 2L.taggedWith[zio_baker.Tag.GeobaseId],
    ruName = "Санкт-Петербург",
    `type` = RegionTypes.City
  )

  private val msc = emptyZeroRegion.copy(
    id = 213L.taggedWith[zio_baker.Tag.GeobaseId],
    ruName = "Москва",
    `type` = RegionTypes.City
  )
  private val regionsMap = List(spb, msc).map(r => r.id -> r).toMap
}

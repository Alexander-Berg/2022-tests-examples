package ru.yandex.vertis.general.wizard.core.service

import common.geobase.Region
import common.geobase.model.RegionIds.RegionId
import ru.yandex.vertis.general.wizard.core.service.RegionService.RegionService
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object RegionServiceMock extends Mock[RegionService] {
  object GetRegion extends Effect[RegionId, Throwable, Option[Region]]
  object GetPathToRoot extends Effect[RegionId, Throwable, Seq[Region]]

  override val compose: URLayer[Has[mock.Proxy], RegionService] =
    ZLayer.fromService { proxy =>
      new RegionService.Service {
        override def getRegion(regionId: RegionId): Task[Option[Region]] = proxy(GetRegion, regionId)

        override def getPathToRoot(regionId: RegionId): Task[Seq[Region]] = proxy(GetPathToRoot, regionId)
      }
    }
}

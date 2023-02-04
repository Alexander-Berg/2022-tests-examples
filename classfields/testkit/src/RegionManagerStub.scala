package auto.common.manager.region.testkit

import auto.common.manager.region.{RegionManager, RegionManagerLive}
import common.geobase.{GeobaseParser, Tree}
import zio.{Has, Ref, ZLayer}

object RegionManagerStub {

  val live: ZLayer[Any, Nothing, Has[RegionManager]] = {
    val regions = GeobaseParser.parse(RegionManagerStub.getClass.getResourceAsStream("/regions"))
    Ref.make(new Tree(regions)).map(new RegionManagerLive(_)).toLayer
  }
}

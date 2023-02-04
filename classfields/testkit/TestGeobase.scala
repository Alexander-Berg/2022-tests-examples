package common.geobase.testkit

import common.geobase.{GeobaseParser, Tree}
import zio.{Has, Ref, UIO, ZIO, ZLayer, ZRef}

object TestGeobase {

  def load: UIO[Tree] = ZIO.effectTotal {
    new Tree(
      GeobaseParser.parse(getClass.getResourceAsStream("/test-regions.xml"))
    )
  }

  val live: ZLayer[Any, Nothing, Has[Tree]] = load.toLayer

  val liveRef: ZLayer[Any, Nothing, Has[Ref[Tree]]] = load.flatMap(ZRef.make).toLayer
}

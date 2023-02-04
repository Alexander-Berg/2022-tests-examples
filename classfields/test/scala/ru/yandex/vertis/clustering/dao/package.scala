package ru.yandex.vertis.clustering

import java.io.File

import ru.yandex.vertis.clustering.model.{Domains, Neo4jGraphGeneration, PackedGraphGeneration}
import ru.yandex.vertis.clustering.services.GraphInstance
import ru.yandex.vertis.clustering.services.impl.GraphInstanceImpl
import ru.yandex.vertis.clustering.utils.DateTimeUtils

import scala.util.Try

/**
  * Basic graph DAO utils
  *
  * @author alesavin
  */
package object dao {

  def asGraphInstance(resource: String): GraphInstanceImpl = {
    val tarGzFile = new File(ClassLoader.getSystemResource(resource).toURI)
    val toUnpack = new File(tarGzFile.getParentFile, "unpack")
    toUnpack.mkdirs()
    val neo4jGeneration = PackedGraphGeneration(DateTimeUtils.now, tarGzFile)
      .unpack(toUnpack)(Neo4jGraphGeneration.apply)
    new GraphInstanceImpl(neo4jGeneration, Domains.Autoru)
  }

}

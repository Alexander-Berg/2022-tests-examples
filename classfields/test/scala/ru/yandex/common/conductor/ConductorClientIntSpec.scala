package ru.yandex.common.conductor

import java.io.File

import org.scalatest.{Ignore, Matchers, WordSpec}
import ru.yandex.common

import scala.concurrent.duration._
import scala.util.Success

/**
 * Specs on [[FileCache]]
 */
trait ConductorClientIntSpec
  extends WordSpec
  with Matchers {

  def client: ConductorClient

  "ConductorClient" should {

    "provide data center of exits host" in {
      client.getDataCenter("csback2ft.yandex.ru") should be (Success(Some("myt")))
    }
    "not provide data center of non-exits host" in {
      client.getDataCenter("csback.yandex.ru") should be (Success(None))
    }

    "list hosts in exist group" in {
      val result = client.getHostsInGroup(common.ZkTestGroup)
      result.isSuccess should be(true)
      result.get should be(defined)
      result.get.get should not be empty
    }

    "not list hosts in non-exists group" in {
      client.getHostsInGroup("vs_zookeeper-foo") should be (Success(None))
    }
  }

}

@Ignore
class PlainConductorClientIntSPec
  extends ConductorClientIntSpec {

  val client = new ConductorClient
}

@Ignore
class CachedConductorClientIntSPec
  extends ConductorClientIntSpec {

  val client = new ConductorClient
    with FileCachedConductorClient {
    val ttl = 5.seconds

    val cacheDir = new File("/tmp")
  }
}

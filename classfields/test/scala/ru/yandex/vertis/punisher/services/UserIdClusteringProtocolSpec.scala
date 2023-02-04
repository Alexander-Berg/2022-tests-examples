package ru.yandex.vertis.punisher.services

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.UserId
import ru.yandex.vertis.punisher.services.impl.UserClusteringProtocol
import spray.json._

/**
  * Specs for [[UserClusteringProtocol]]
  *
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class UserIdClusteringProtocolSpec extends BaseSpec {

  import UserClusteringProtocol._

  "UserClusteringProtocol" should {
    "get Cluster" in {
      resourceString("/json/cluster.json").parseJson.convertTo[Set[UserId]].size > 1
    }
    "get empty seq" in {
      "[]".parseJson.convertTo[Set[UserId]].isEmpty
    }
  }
}

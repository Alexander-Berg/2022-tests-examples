package ru.yandex.vertis.vsquality.techsupport.clients

import cats.instances.try_._
import sttp.client3._
import com.softwaremill.tagging._
import org.scalatest.Ignore
import ru.yandex.vertis.vsquality.techsupport.clients.BunkerClient.BunkerNodeTag
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpBunkerClient
import ru.yandex.vertis.vsquality.techsupport.model.{Tags, Url}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

import scala.util.Try

/**
  * @author potseluev
  */
@Ignore
class HttpBunkerClientSpec extends SpecBase {

  private val baseBunkerUrl: Url =
    "http://bunker-api-dot.yandex.net/v1".taggedWith[Tags.Url]
  implicit private val sttpSyncBackend: TryBackend[Any] = new TryBackend[Any](quick.backend)
  private val client: BunkerClient[Try] = new HttpBunkerClient(baseBunkerUrl)

  "BunkerClient.getContent" should {
    "get content of existent json node correctly" in {
      val nodeId = "/vertis-moderation/moderation-admin".taggedWith[BunkerNodeTag]
      client.getContent(nodeId).get shouldBe defined
    }

    "get none if node doesn't exist" in {
      val nodeId = "/vertis-moderation/nonexistent-node".taggedWith[BunkerNodeTag]
      client.getContent(nodeId).get shouldBe None
    }

    "get none if node doesn't have any json content" in {
      val nodeId = "/vertis-moderation".taggedWith[BunkerNodeTag]
      client.getContent(nodeId).get shouldBe None
    }
  }

  "BunkerClient.list" should {
    "get children of the node correctly" in {
      val nodeId = "/vertis-moderation".taggedWith[BunkerNodeTag]
      val nodes = client.list(nodeId, recursively = false).get
      nodes should not be empty
      nodes.map(_.id) should contain("/vertis-moderation/autoru".taggedWith[BunkerNodeTag])
      nodes.map(_.id) should not contain "/vertis-moderation/autoru/reasons".taggedWith[BunkerNodeTag]
    }

    "get children of the node recursively correctly" in {
      val nodeId = "/vertis-moderation".taggedWith[BunkerNodeTag]
      val nodes = client.list(nodeId, recursively = true).get
      nodes should not be empty
      nodes.map(_.id) should contain("/vertis-moderation/autoru".taggedWith[BunkerNodeTag])
      nodes.map(_.id) should contain("/vertis-moderation/autoru/reasons/another".taggedWith[BunkerNodeTag])
    }

    "get empty list if no such node" in {
      val nodeId = "/vertis-moderation/nonexistent-node".taggedWith[BunkerNodeTag]
      val nodes = client.list(nodeId, recursively = false).get
      nodes shouldBe empty
    }
  }
}

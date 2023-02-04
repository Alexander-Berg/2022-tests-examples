package ru.yandex.auto.searchline.yt

import YtClient.NodeType.NodeType
import YtClient._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 10.11.16
  */
trait YtClient {

  def create(nodeType: NodeType,
             path: String,
             optAttributes: Option[Seq[Attribute]],
             optParams: Option[CreateParams]): Unit

  def writeTable(path: String,
                 fileName: String,
                 optParams: Option[WriteParams]
                ): Unit

  def link(targetPath: String,
           linkPath: String,
           optParams: Option[LinkParams]
          ): Unit

  def remove(path: String,
             optParams: Option[RemovedParams]
            ): Unit

  def exists(path: String): Boolean

  def get(path: String): String
}

object YtClient {

  type Attribute = (String, String)

  case class CreateParams(recursive: Boolean = true,
                          ignoreExisting: Boolean = false,
                          force: Boolean = false)

  case class WriteParams(append: Boolean = false,
                         encode_utf8: Boolean = false,
                         tableWriter: Option[String] = None)

  case class LinkParams(recursive: Boolean = true,
                        ignoreExisting: Boolean = false,
                        force: Boolean = false)

  case class RemovedParams(recursive: Boolean = true,
                           force: Boolean = true)

  object NodeType {

    sealed abstract class NodeType(name: String) {
      override def toString: String = name
    }

    case object Table extends NodeType("table")

    case object MapNode extends NodeType("map_node")

  }

}
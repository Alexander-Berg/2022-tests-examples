package ru.yandex.vertis.scalatest.matcher

import scala.language.implicitConversions

/**
  * Path of the field.
  * This path is similar to path to file in the FS.
  *
  * @see [[Diff]]
  *
  * @author semkagtn
  */
case class FieldPath private(nodes: Vector[String]) {

  override lazy val toString: String =
    nodes.mkString("/", "/", "")

  def :+(nextNode: String): FieldPath =
    FieldPath(nodes :+ nextNode)

  def isAncestorOf(path: FieldPath): Boolean =
    if (nodes.size < path.nodes.size)
      path.nodes.take(nodes.size) == nodes
    else
      false
}

object FieldPath {

  val Empty: FieldPath = new FieldPath(Vector.empty)

  implicit val Ordering: Ordering[FieldPath] = new Ordering[FieldPath] {
    override def compare(x: FieldPath, y: FieldPath): Int =
      x.toString compareTo y.toString
  }

  implicit def stringToFieldPath(string: String): FieldPath =
    fromString(string)

  def fromString(string: String): FieldPath =
    if (string.startsWith("/")) {
      val withoutRoot = string.tail
      if (withoutRoot.nonEmpty)
        FieldPath(string.tail.split('/').toVector)
      else
        FieldPath.Empty
    } else {
      throw new IllegalArgumentException("Field path must start with '/' symbol")
    }
}

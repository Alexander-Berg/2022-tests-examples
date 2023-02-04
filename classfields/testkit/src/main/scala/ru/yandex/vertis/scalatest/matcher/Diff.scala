package ru.yandex.vertis.scalatest.matcher

/**
  * Describes difference between two fields.
  *
  * @see [[Differ]]
  *
  * @author semkagtn
  */
sealed trait Diff {

  /**
    * Path of the field. It like path to file in the FS.
    */
  def fieldPath: FieldPath
}

object Diff {

  /**
    * Actual object contains field, but expected object doesn't contain.
    *
    * @param actual field value in the actual object.
    */
  case class Added(fieldPath: FieldPath, actual: Any) extends Diff

  /**
    * Expected object contains field, but actual object doesn't contain.
    *
    * @param expected field value in the expected object.
    */
  case class Removed(fieldPath: FieldPath, expected: Any) extends Diff

  /**
    * Actual object and expected object contain field but field values are different.
    *
    * @param actual field value in the actual object.
    * @param expected field value in the expected object.
    */
  case class Changed(fieldPath: FieldPath, actual: Any, expected: Any) extends Diff {
    require(actual != expected, "actual must be not equal to expected")
  }
}

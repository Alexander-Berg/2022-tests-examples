package scandex.model.gen

import scandex.model.*

import scandex.model.meta.FieldDataTypeMeta.{Compression, IndexType}
import strict.*
import zio.test.Gen.*
import zio.test.*

import scala.collection.SortedSet

sealed trait FieldGenSchema[F] {
  def name: String
  def indexType: IndexType

  def apply(): Gen[Sized, (String, Field[F])] = generator.map(v => name -> v)

  protected def generator: Gen[Sized, Field[F]]

}

case class StorageFieldGen[F : FieldType : Ordering : TypeShow, R](
    name: String,
    gen: Gen[Sized, F],
) extends FieldGenSchema[F] {

  override protected def generator: Gen[Sized, Field[F]] =
    gen.map(v => Field(SortedSet(v), indexType, Compression.NONE))

  override def indexType: IndexType = IndexType.STORAGE

}

case class SingleValueFieldGen[F : FieldType : Ordering : TypeShow, R](
    name: String,
    indexType: IndexType,
    gen: Gen[Sized, F],
) extends FieldGenSchema[F] {

  override protected def generator: Gen[Sized, Field[F]] =
    gen.map(v => Field(SortedSet(v), indexType, Compression.NONE))

}

case class MultiValuedFieldGen[F : FieldType : Ordering : TypeShow](
    name: String,
    indexType: IndexType,
    gen: Gen[Sized, F],
) extends FieldGenSchema[F] {

  override protected def generator: Gen[Sized, Field[F]] =
    setOf(gen).map(v => Field(SortedSet.from(v), indexType, Compression.NONE))

}

case class DocumentGenSchema[PK : PrimaryKey](
    primaryGen: Gen[Sized, PK],
    fields: FieldGenSchema[?]*,
) {

  def apply(): Gen[Sized, Document[PK]] = {
    elements(fields.map(_())).flatMap { fieldsGen =>
      primaryGen
        .zip(collectAll(fieldsGen))
        .map { case (pk, fields) =>
          Document(pk, fields.toMap)
        }
    }
  }

}

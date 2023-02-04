package scandex.model

import scandex.model.meta.FieldDataTypeMeta.IndexType
import zio.test.*
import zio.test.Gen.*
import strict.*
import tests.strict.types.*
import zio.Chunk

import scala.collection.SortedSet
import zio.Random
import zio.test.Sized
import zio.test.Gen.string

package object gen {

  val itype = IndexType.STORAGE

  val anyFieldGen
    : Gen[Random & Sized, (String, Gen[Random & Sized, Field[_]])] =
    alphaNumericString
      .filter(_.nonEmpty)
      .zip(
        elements(
          bytesValues(anyBytes),
          stringValues(string),
          int8Values(anyInt8),
          int16Values(anyInt16),
          int32Values(anyInt32),
          int64Values(anyInt64),
          uint8Values(anyUint8),
          uint16Values(anyUint16),
          uint32Values(anyUint32),
          uint64Values(anyUint64),
          float32Values(anyFloat32),
          float64Values(anyFloat64),
          timestampValues(anyTimestamp),
        ),
      )

  def heteroDocsGen[PK : PrimaryKey](
    primaryKeyGen: Gen[Random & Sized, PK],
  ): Gen[Random & Sized, Gen[Random & Sized, Document[PK]]] =
    chunkOf(anyFieldGen).map {
      fieldGens: Chunk[(String, Gen[Random & Sized, Field[?]])] =>
        primaryKeyGen
          .zip(
            collectAll(
              fieldGens.map { case (name, gen) =>
                gen.map(name -> _).filter(_._2.values.nonEmpty)
              },
            ).map(_.toMap),
          )
          .map { case (pk, fields) =>
            Document(pk, fields)
          }
    }

  def bytesValues[R <: Random](
    gen: Gen[R, Bytes],
  ): Gen[R & Sized, Field[Bytes]] =
    setOf(gen).map(set => Field[Bytes](SortedSet.from(set), itype))

  def stringValues[R <: Random](
    gen: Gen[R, String],
  ): Gen[R & Sized, Field[Utf8]] =
    setOf(gen).map(set => Field(SortedSet.from(set.map(Utf8(_))), itype))

  def int8Values[R <: Random](gen: Gen[R, Int8]): Gen[R & Sized, Field[Int8]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def int16Values[R <: Random](
    gen: Gen[R, Int16],
  ): Gen[R & Sized, Field[Int16]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def int32Values[R <: Random](
    gen: Gen[R, Int32],
  ): Gen[R & Sized, Field[Int32]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def int64Values[R <: Random](
    gen: Gen[R, Int64],
  ): Gen[R & Sized, Field[Int64]] =
    setOf(gen).map(set => Field[Int64](SortedSet.from(set), itype))

  def uint8Values[R <: Random](
    gen: Gen[R, Uint8],
  ): Gen[R & Sized, Field[Uint8]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def uint16Values[R <: Random](
    gen: Gen[R, Uint16],
  ): Gen[R & Sized, Field[Uint16]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def uint32Values[R <: Random](
    gen: Gen[R, Uint32],
  ): Gen[R & Sized, Field[Uint32]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def uint64Values[R <: Random](
    gen: Gen[R, Uint64],
  ): Gen[R & Sized, Field[Uint64]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def float32Values[R <: Random](
    gen: Gen[R, Float32],
  ): Gen[R & Sized, Field[Float32]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def float64Values[R <: Random](
    gen: Gen[R, Float64],
  ): Gen[R & Sized, Field[Float64]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

  def timestampValues[R <: Random](
    gen: Gen[R, Timestamp],
  ): Gen[R & Sized, Field[Timestamp]] =
    setOf(gen).map(set => Field(SortedSet.from(set), itype))

}

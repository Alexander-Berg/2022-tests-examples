package ru.yandex.vertis.ydb.skypper.fragment

import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import com.fortysevendeg.scalacheck.datetime.instances.jdk8._
import com.yandex.ydb.table.values.Value
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.jdk.CollectionConverters._

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import com.yandex.ydb.table.values.TupleValue
import com.yandex.ydb.table.values.VariantValue

@RunWith(classOf[JUnitRunner])
class PutTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {
  import Put._

  private val genTimestamp = genDateTimeWithinRange(
    LocalDate.of(0, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant,
    Duration.ofDays(3000 * 365)
  )

  private val genDate = genDateTimeWithinRange(
    LocalDate.of(1970, 1, 1).atStartOfDay(),
    Duration.ofDays(136 * 365)
  ).map(_.toLocalDate)

  check("bool", bool, _.asData.getBool, arbitrary[Boolean])
  check("int8", int8, _.asData.getInt8, arbitrary[Byte])
  check("uint8", uint8, _.asData.getUint8.toByte, arbitrary[Byte])
  check("int16", int16, _.asData.getInt16, arbitrary[Short])
  check("uint16", uint16, _.asData.getUint16.toShort, arbitrary[Short])
  check("int32", int32, _.asData.getInt32, arbitrary[Int])
  check("uint32", uint32, _.asData.getUint32.toInt, arbitrary[Int])
  check("int64", int64, _.asData.getInt64, arbitrary[Long])
  check("uint64", uint64, _.asData.getUint64, arbitrary[Long])
  check("float32", float32, _.asData.getFloat32, arbitrary[Float])
  check("float64", float64, _.asData.getFloat64, arbitrary[Double])
  check("uuid", uuid, _.asData.getUuidJdk, arbitrary[UUID])
  check("utf8", utf8, _.asData.getUtf8, arbitrary[String])
  check("date", date, _.asData.getDate, genDate)
  check("timestamp", timestamp, _.asData.getTimestamp, genTimestamp)
  check("optional<int32>", optional(int32), asOption(_).map(_.asData.getInt32), arbitrary[Option[Int]])
  check("optional<utf8>", optional(utf8), asOption(_).map(_.asData.getUtf8), arbitrary[Option[String]])
  check("list<int32>", list(int32), asList(_).map(_.asData.getInt32), arbitrary[Seq[Int]])
  check("list<utf8>", list(utf8), asList(_).map(_.asData.getUtf8), arbitrary[Seq[String]])
  check(
    "dict<int32, utf8>",
    dict(int32, utf8),
    asDict(_).map { case (k, v) => (k.asData.getInt32, v.asData.getUtf8) }.toMap,
    arbitrary[Map[Int, String]]
  )
  check(
    "dict<utf8, int32>",
    dict(utf8, int32),
    asDict(_).map { case (k, v) => (k.asData.getUtf8, v.asData.getInt32) }.toMap,
    arbitrary[Map[String, Int]]
  )
  check("tuple<int32>", tuple1(int32), _.asInstanceOf[TupleValue].get(0).asData.getInt32, arbitrary[Int])
  check("tuple<utf8>", tuple1(utf8), _.asInstanceOf[TupleValue].get(0).asData.getUtf8, arbitrary[String])
  check(
    "tuple<int8, uint8>",
    tuple2(int8, uint8),
    v => {
      val t = v.asInstanceOf[TupleValue]
      (t.get(0).asData.getInt8, t.get(1).asData.getUint8.toByte)
    },
    arbitrary[(Byte, Byte)]
  )
  check(
    "tuple<int8, uint8, int16>",
    tuple3(int8, uint8, int16),
    v => {
      val t = v.asInstanceOf[TupleValue]
      (t.get(0).asData.getInt8, t.get(1).asData.getUint8.toByte, t.get(2).asData.getInt16)
    },
    arbitrary[(Byte, Byte, Short)]
  )
  check(
    "tuple<int8, uint8, int16, uint16>",
    tuple4(int8, uint8, int16, uint16),
    v => {
      val t = v.asInstanceOf[TupleValue]
      (
        t.get(0).asData.getInt8,
        t.get(1).asData.getUint8.toByte,
        t.get(2).asData.getInt16,
        t.get(3).asData.getUint16.toShort
      )
    },
    arbitrary[(Byte, Byte, Short, Short)]
  )
  check(
    "tuple<int8, uint8, int16, uint16, int32>",
    tuple5(int8, uint8, int16, uint16, int32),
    v => {
      val t = v.asInstanceOf[TupleValue]
      (
        t.get(0).asData.getInt8,
        t.get(1).asData.getUint8.toByte,
        t.get(2).asData.getInt16,
        t.get(3).asData.getUint16.toShort,
        t.get(4).asData.getInt32
      )
    },
    arbitrary[(Byte, Byte, Short, Short, Int)]
  )
  check("variant<int32>", variant1(int32), _.asInstanceOf[VariantValue].getItem.asData.getInt32, arbitrary[Int])
  check("variant<utf8>", variant1(utf8), _.asInstanceOf[VariantValue].getItem.asData.getUtf8, arbitrary[String])
  check(
    "variant<int8, uint8>",
    variant2(int8, uint8),
    v => {
      val t = v.asInstanceOf[VariantValue]
      t.getTypeIndex match {
        case 0 => Left(t.getItem.asData.getInt8)
        case 1 => Right(t.getItem.asData.getUint8.toByte)
      }
    },
    arbitrary[Either[Byte, Byte]]
  )
  check("void", void, _ => (), arbitrary[Unit])

  private def check[A](name: String, put: Put.Typed[A], extract: Value[_] => A, gen: Gen[A]): Unit =
    test(s"$name") {
      forAll(gen) { a =>
        val value = put.toSql(a)
        assert(value.getType == put.sqlType)
        val extracted = extract(value)
        assert(extracted == a)
      }
    }

  private def asOption(v: Value[_]): Option[Value[_]] = {
    val vOpt = v.asOptional
    if (vOpt.isPresent) Some(vOpt.get) else None
  }

  private def asList(v: Value[_]): Seq[Value[_]] = {
    val vList = v.asList
    0.until(vList.size).map(vList.get)
  }

  private def asDict(v: Value[_]): Map[Value[_], Value[_]] = {
    val vDict = v.asDict
    vDict.keySet.asScala.map(k => (k, vDict.get(k))).toMap
  }
}

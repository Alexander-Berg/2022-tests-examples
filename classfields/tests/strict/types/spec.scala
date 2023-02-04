package tests.strict.types

import strict.*
import strict.util.LinkedMap
import zio.test.*
import zio.test.Gen.{byte, int, long, short}

case object spec extends ZIOSpecDefault {

  override def spec = {
    suite("strict")(
      suite("unsigned")(
        {
          import java.lang.Byte.toUnsignedLong

          suite("Uint8")(
            test("/") {
              check(
                byte.map(toUnsignedLong),
                byte.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint8
                val b = y.toUint8
                val c = (x / y).toUint8
                assertTrue(a / b == c)
              }
            },
            test("%") {
              check(
                byte.map(toUnsignedLong),
                byte.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint8
                val b = y.toUint8
                val c = (x % y).toUint8
                assertTrue(a % b == c)
              }
            },
            test("+") {
              check(byte.map(toUnsignedLong), byte.map(toUnsignedLong)) {
                (x, y) =>
                  val a               = x.toUint8
                  val b               = y.toUint8
                  val result: Uint8   = a + b
                  val expected: Uint8 = (x + y).toUint8
                  assertTrue(result == expected)
              }
            },
            test("-") {
              check(byte.map(toUnsignedLong), byte.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint8
                  val b = y.toUint8
                  val c = (x - y).toUint8
                  assertTrue(a - b == c)
              }
            },
            test("*") {
              check(byte.map(toUnsignedLong), byte.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint8
                  val b = y.toUint8
                  val c = (x * y).toUint8
                  assertTrue(a * b == c)
              }
            },
            test("+ overflow") {
              val result: Uint8   = 255.toUint8 + 4.toUint8
              val expected: Uint8 = (255 + 4).toUint8
              assertTrue(result == expected)
            },
            test("* overflow") {
              assertTrue(
                (Uint32.MaxValue.toLong - 11L).toUint32 * 3.toUint32 ==
                  ((Uint32.MaxValue.toLong - 11L) * 3).toUint32,
              )
            },
            test("- underflow") {
              assertTrue(0.toUint8 - 4.toUint8 == (-4).toUint8)
            },
            test("toString") {
              check(byte.map(toUnsignedLong)) { x =>
                assertTrue(
                  x.toUint8.toStr == java.lang.Long.toUnsignedString(x),
                )
              }
            },
            test("toInt") {
              check(byte.map(toUnsignedLong)) { x =>
                assertTrue(
                  x.toUint8.toInt == java.lang.Byte.toUnsignedInt(x.toByte),
                )
              }
            },
            test("corners") {
              assertTrue(1.toUint8 - 2.toUint8 == Uint8.MaxValue) &&
              assertTrue(Uint8.MaxValue + 2.toUint8 == 1.toUint8) &&
              assertTrue(
                Uint8.MaxValue * 2.toUint8 == Uint8.MaxValue - 1.toUint8,
              )
            },
          )
        }, {
          import java.lang.Short.toUnsignedLong

          suite("Uint16")(
            test("/") {
              check(
                short.map(toUnsignedLong),
                short.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint16
                val b = y.toUint16
                val c = (x / y).toUint16
                assertTrue(a / b == c)
              }
            },
            test("%") {
              check(
                short.map(toUnsignedLong),
                short.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint16
                val b = y.toUint16
                val c = (x % y).toUint16
                assertTrue(a % b == c)
              }
            },
            test("+") {
              check(short.map(toUnsignedLong), short.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint16
                  val b = y.toUint16
                  val c = (x + y).toUint16
                  assertTrue(a + b == c)
              }
            },
            test("-") {
              check(short.map(toUnsignedLong), short.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint16
                  val b = y.toUint16
                  val c = (x - y).toUint16
                  assertTrue(a - b == c)
              }
            },
            test("*") {
              check(short.map(toUnsignedLong), short.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint16
                  val b = y.toUint16
                  val c = (x * y).toUint16
                  assertTrue(a * b == c)
              }
            },
            test("+ overflow") {
              assertTrue(255.toUint16 + 4.toUint16 == (255 + 4).toUint16)
            },
            test("* overflow") {
              assertTrue(
                (Uint16.MaxValue.toLong - 11L).toUint16 * 3.toUint16 ==
                  ((Uint16.MaxValue.toLong - 11L) * 3).toUint16,
              )
            },
            test("- underflow") {
              assertTrue(0.toUint16 - 4.toUint16 == (-4).toUint16)
            },
            test("toString") {
              check(short.map(toUnsignedLong)) { x =>
                assertTrue(
                  x.toUint16.toStr == java.lang.Long.toUnsignedString(x),
                )
              }
            },
            test("corners") {
              assertTrue((1.toUint16 - 2.toUint16) == Uint16.MaxValue) &&
              assertTrue(Uint16.MaxValue + 2.toUint16 == 1.toUint16) &&
              assertTrue(
                Uint16.MaxValue * 2.toUint16 == Uint16.MaxValue - 1.toUint16,
              )
            },
          )
        }, {
          import java.lang.Integer.toUnsignedLong

          suite("Uint32")(
            test("/") {
              check(
                int.map(toUnsignedLong),
                int.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint32
                val b = y.toUint32
                val c = (x / y).toUint32
                assertTrue(a / b == c)
              }
            },
            test("%") {
              check(
                int.map(toUnsignedLong),
                int.withFilter(_ != 0).map(toUnsignedLong),
              ) { (x, y) =>
                val a = x.toUint32
                val b = y.toUint32
                val c = (x % y).toUint32
                assertTrue(a % b == c)
              }
            },
            test("+") {
              check(int.map(toUnsignedLong), int.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint32
                  val b = y.toUint32
                  val c = (x + y).toUint32
                  assertTrue(a + b == c)
              }
            },
            test("-") {
              check(int.map(toUnsignedLong), int.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint32
                  val b = y.toUint32
                  val c = (x - y).toUint32
                  assertTrue(a - b == c)
              }
            },
            test("*") {
              check(int.map(toUnsignedLong), int.map(toUnsignedLong)) {
                (x, y) =>
                  val a = x.toUint32
                  val b = y.toUint32
                  val c = (x * y).toUint32
                  assertTrue(a * b == c)
              }
            },
            test("+ overflow") {
              assertTrue(255.toUint32 + 4.toUint32 == (255 + 4).toUint32)
            },
            test("* overflow") {
              assertTrue(
                (Uint32.MaxValue.toLong - 11L).toUint32 * 3.toUint32 ==
                  ((Uint32.MaxValue.toLong - 11L) * 3).toUint32,
              )
            },
            test("- underflow") {
              assertTrue(0.toUint32 - 4.toUint32 == (-4).toUint32)
            },
            test("toString") {
              check(int.map(toUnsignedLong)) { x =>
                assertTrue(
                  x.toUint32.toStr == java.lang.Long.toUnsignedString(x),
                )
              }
            },
            test("corners") {
              assertTrue(1.toUint32 - 2.toUint32 == Uint32.MaxValue) &&
              assertTrue(Uint32.MaxValue + 2.toUint32 == 1.toUint32) &&
              assertTrue(
                Uint32.MaxValue * 2.toUint32 == Uint32.MaxValue - 1.toUint32,
              )
            },
          )
        }, {
          import java.lang.Long.toUnsignedString

          suite("Uint64")(
            test("/") {
              check(long, long.withFilter(_ != 0)) { (x, y) =>
                val a = x.toUint64
                val b = y.toUint64
                val c =
                  (BigInt(toUnsignedString(x)) / BigInt(toUnsignedString(y)))
                    .toUint64
                assertTrue(a / b == c)
              }
            },
            test("%") {
              check(long, long.withFilter(_ != 0)) { (x, y) =>
                val a = x.toUint64
                val b = y.toUint64
                val c =
                  (BigInt(toUnsignedString(x)) % BigInt(toUnsignedString(y)))
                    .toUint64
                assertTrue(a % b == c)
              }
            },
            test("+") {
              check(
                long.map(toUnsignedString).map(BigInt.apply),
                long.map(toUnsignedString).map(BigInt.apply),
              ) { (x, y) =>
                val a = x.toUint64
                val b = y.toUint64
                val c = (x + y).toUint64
                assertTrue(a + b == c)
              }
            },
            test("-") {
              check(
                long.map(toUnsignedString).map(BigInt.apply),
                long.map(toUnsignedString).map(BigInt.apply),
              ) { (x, y) =>
                val a = x.toUint64
                val b = y.toUint64
                val c = (x - y).toUint64
                assertTrue(a - b == c)
              }
            },
            test("*") {
              check(
                long.map(toUnsignedString).map(BigInt.apply),
                long.map(toUnsignedString).map(BigInt.apply),
              ) { (x, y) =>
                val a = x.toUint64
                val b = y.toUint64
                val c = (x * y).toUint64
                assertTrue(a * b == c)
              }
            },
            test("+ overflow") {
              assertTrue(
                Uint64.MaxValue + 4.toUint64 ==
                  (Uint64.MaxValue.toLong + 4).toUint64,
              )
            },
            test("* overflow") {
              assertTrue(
                (Uint64.MaxValue.toLong - 11L).toUint64 * 3.toUint64 ==
                  ((Uint64.MaxValue.toLong - 11L) * 3).toUint64,
              )
            },
            test("- underflow") {
              assertTrue(Uint64.MinValue - 4.toUint64 == (-4).toUint64)
            },
            test("toString") {
              check(long) { x =>
                assertTrue(
                  x.toUint64.toStr == java.lang.Long.toUnsignedString(x),
                )
              }
            },
            test("corners") {
              assertTrue(1.toUint64 - 2.toUint64 == Uint64.MaxValue) &&
              assertTrue(Uint64.MaxValue + 2.toUint64 == 1.toUint64) &&
              assertTrue(
                Uint64.MaxValue * 2.toUint64 == Uint64.MaxValue - 1.toUint64,
              )
            },
          )
        },
      ),
      suite("struct derivation")(
        test("Dummy") {
          val u: StructValue = Dummy(1.toInt32, 2.toInt64)
          val fs: Seq[(String, UniversalValue)] = Seq[(String, UniversalValue)](
            "a" -> 1.toInt32,
            "b" -> 2.toInt64,
          )

          assertTrue(u == StructValue(LinkedMap.from(fs)))
        },
      ),
      suite("list of struct derivation")(
        test("Dummy") {
          val u: ListValue = List(Dummy(1.toInt32, 2.toInt64))
          val fs: Seq[(String, UniversalValue)] = Seq[(String, UniversalValue)](
            "a" -> 1.toInt32,
            "b" -> 2.toInt64,
          )

          assertTrue(u == ListValue(Seq(StructValue(LinkedMap.from(fs))))) &&
          assertTrue(
            u.getTypeTag ==
              ListTypeTag(
                StructTypeTag(
                  LinkedMap.from(
                    fs.map { case (k, v) =>
                      k -> v.getTypeTag.asUniversalTag
                    },
                  ),
                ).asUniversalTag,
              ),
          )
        },
      ),
    )
  }

  case class Dummy(a: Int32, b: Int64)

}

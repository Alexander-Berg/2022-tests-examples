package vasgen.core.saas

object RawValueConverterTest extends ZIOSpecDefault {

  val positiveFt = 5.787f
  val negativeFt = -5.787f
  val positiveD  = 5.787d
  val negativeD  = -5.787d

  override def spec: Spec[TestEnvironment, Any] =
    suite("transformation")(
      suite("cast")(
        // format: off
        test("cast u64"){
          assert(cast(u64(Long.MaxValue), model.u64))(isSome(equalTo(u64(Long.MaxValue)))) &&
          assert(cast(u64(Long.MaxValue), model.i64))(isSome(equalTo(i64(Long.MaxValue)))) &&
          assert(cast(u64(Long.MinValue), model.i64))(isNone) &&
          assert(cast(u64(Long.MinValue), model.f64))(isSome(equalTo(f64(Long.MaxValue.toDouble + 1)))) &&
          assert(cast(u64(Long.MaxValue), model.f64))(isSome(equalTo(f64(Long.MaxValue.toDouble)))) &&
          assert(cast(u64(Long.MinValue), model.f32))(isSome(equalTo(f32(Long.MaxValue.toFloat + 1)))) &&
          assert(cast(u64(Long.MinValue), model.u32))(isNone) &&
          assert(cast(u64(Long.MaxValue), model.u32))(isNone) &&
          assert(cast(u64(Int.MaxValue.toLong), model.u32))(isSome(equalTo(u32(Int.MaxValue)))) &&
          assert(cast(u64(Int.MaxValue.toLong * 2 - 5L), model.u32))(isSome(equalTo(u32(-7)))) &&
          assert(cast(u64(RawValueUtil.MaxU32), model.u32))(isSome(equalTo(u32(-1)))) &&
          assert(cast(u64(Long.MinValue), model.i32))(isNone) &&
          assert(cast(u64(Long.MaxValue), model.i32))(isNone) &&
          assert(cast(u64(Int.MaxValue.toLong), model.i32))(isSome(equalTo(i32(Int.MaxValue)))) &&
          assert(cast(u64(Long.MaxValue), model.geo))(isSome(equalTo(i64(Long.MaxValue)))) &&
          assert(cast(u64(Long.MaxValue), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(u64(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(u64(Long.MaxValue), model.timestamp))(isSome(equalTo(timestamp(Long.MaxValue)))) &&
          assert(cast(u64(Long.MinValue), model.timestamp))(isNone) &&
            assert(cast(u64(Long.MinValue), model.str))(isSome(equalTo(str("9223372036854775808"))))
        },
        test("cast i64"){
          assert(cast(i64(Long.MinValue), model.u64))(isNone) &&
          assert(cast(i64(Long.MaxValue), model.u64))(isSome(equalTo(u64(Long.MaxValue)))) &&
          assert(cast(i64(Long.MinValue), model.i64))(isSome(equalTo(i64(Long.MinValue)))) &&
          assert(cast(i64(Long.MaxValue), model.i64))(isSome(equalTo(i64(Long.MaxValue)))) &&
          assert(cast(i64(Long.MinValue), model.f64))(isSome(equalTo(f64(Long.MinValue.toDouble)))) &&
          assert(cast(i64(Long.MaxValue), model.f64))(isSome(equalTo(f64(Long.MaxValue.toDouble)))) &&
          assert(cast(i64(Long.MinValue), model.f32))(isSome(equalTo(f32(Long.MinValue.toFloat)))) &&
          assert(cast(i64(Long.MaxValue), model.f32))(isSome(equalTo(f32(Long.MaxValue.toFloat)))) &&
          assert(cast(i64(Long.MinValue), model.u32))(isNone) &&
          assert(cast(i64(Long.MaxValue), model.u32))(isNone) &&
          assert(cast(i64(Int.MinValue.toLong), model.u32))(isNone) &&
          assert(cast(i64(Int.MaxValue.toLong), model.u32))(isSome(equalTo(u32(Int.MaxValue)))) &&
          assert(cast(u64(RawValueUtil.MaxU32), model.u32))(isSome(equalTo(u32(-1)))) &&
          assert(cast(i64(Long.MinValue), model.i32))(isNone) &&
          assert(cast(i64(Long.MaxValue), model.i32))(isNone) &&
          assert(cast(i64(Int.MinValue.toLong), model.i32))(isSome(equalTo(i32(Int.MinValue)))) &&
          assert(cast(i64(Int.MaxValue.toLong), model.i32))(isSome(equalTo(i32(Int.MaxValue)))) &&
          assert(cast(i64(Long.MaxValue), model.geo))(isSome(equalTo(i64(Long.MaxValue)))) &&
          assert(cast(i64(Long.MaxValue), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(i64(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(i64(Long.MaxValue), model.timestamp))(isSome(equalTo(timestamp(Long.MaxValue)))) &&
          assert(cast(i64(Long.MinValue), model.timestamp))(isNone) &&
            assert(cast(i64(Long.MinValue), model.str))(isSome(equalTo(str("9223372036854775808"))))
        },
        test("cast u32"){
          assert(cast(u32(Int.MaxValue), model.u64))(isSome(equalTo(u64(Int.MaxValue)))) &&
          assert(cast(u32(Int.MinValue), model.u64))(isSome(equalTo(u64(Int.MaxValue.toLong + 1)))) &&
          assert(cast(u32(Int.MaxValue), model.i64))(isSome(equalTo(i64(Int.MaxValue)))) &&
          assert(cast(u32(Int.MinValue), model.i64))(isSome(equalTo(i64(Int.MaxValue.toLong + 1)))) &&
          assert(cast(u32(Int.MinValue), model.f64))(isSome(equalTo(f64(Int.MaxValue.toDouble + 1)))) &&
          assert(cast(u32(Int.MaxValue), model.f64))(isSome(equalTo(f64(Int.MaxValue.toDouble)))) &&
          assert(cast(u32(Int.MinValue), model.f32))(isSome(equalTo(f32(Int.MaxValue.toFloat + 1)))) &&
          assert(cast(u32(Int.MinValue), model.u32))(isSome(equalTo(u32(Int.MinValue)))) &&
          assert(cast(u32(Int.MaxValue), model.u32))(isSome(equalTo(u32(Int.MaxValue)))) &&
          assert(cast(u32(Int.MinValue), model.i32))(isNone) &&
          assert(cast(u32(Int.MaxValue), model.i32))(isSome(equalTo(i32(Int.MaxValue)))) &&
          assert(cast(u32(Int.MaxValue), model.geo))(isSome(equalTo(i64(Int.MaxValue)))) &&
          assert(cast(u32(Int.MaxValue), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(u32(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(u32(Int.MaxValue), model.timestamp))(isSome(equalTo(timestamp(Int.MaxValue)))) &&
          assert(cast(u32(Int.MinValue), model.timestamp))(isSome(equalTo(timestamp(Int.MaxValue.toLong + 1)))) &&
          assert(cast(u32(Int.MinValue), model.str))(isSome(equalTo(str((Int.MaxValue.toLong + 1).toString))))
        },
        test("cast i32"){
          assert(cast(i32(Int.MaxValue), model.u64))(isSome(equalTo(u64(Int.MaxValue)))) &&
          assert(cast(i32(Int.MinValue), model.u64))(isNone) &&
          assert(cast(i32(Int.MaxValue), model.i64))(isSome(equalTo(i64(Int.MaxValue)))) &&
          assert(cast(i32(Int.MinValue), model.i64))(isSome(equalTo(i64(Int.MinValue.toLong)))) &&
          assert(cast(i32(Int.MinValue), model.f64))(isSome(equalTo(f64(Int.MinValue.toDouble)))) &&
          assert(cast(i32(Int.MaxValue), model.f64))(isSome(equalTo(f64(Int.MaxValue.toDouble)))) &&
          assert(cast(i32(Int.MinValue), model.f32))(isSome(equalTo(f32(Int.MinValue.toFloat)))) &&
          assert(cast(i32(Int.MinValue), model.u32))(isNone) &&
          assert(cast(i32(Int.MaxValue), model.u32))(isSome(equalTo(u32(Int.MaxValue)))) &&
          assert(cast(i32(Int.MinValue), model.i32))(isSome(equalTo(i32(Int.MinValue)))) &&
          assert(cast(i32(Int.MaxValue), model.i32))(isSome(equalTo(i32(Int.MaxValue)))) &&
          assert(cast(i32(Int.MaxValue), model.geo))(isSome(equalTo(i64(Int.MaxValue)))) &&
          assert(cast(i32(Int.MaxValue), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(i32(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(i32(Int.MaxValue), model.timestamp))(isSome(equalTo(timestamp(Int.MaxValue)))) &&
          assert(cast(i32(Int.MinValue), model.timestamp))(isSome(equalTo(timestamp(Int.MinValue.toLong)))) &&
          assert(cast(i32(Int.MinValue), model.str))(isSome(equalTo(str(Int.MinValue.toString))))
        },

        test("cast f32"){
          assert(cast(f32(positiveFt), model.u64))(isSome(equalTo(u64(positiveFt.toLong)))) &&
          assert(cast(f32(negativeFt), model.u64))(isNone) &&
          assert(cast(f32(positiveFt), model.i64))(isSome(equalTo(i64(positiveFt.toLong)))) &&
          assert(cast(f32(negativeFt), model.i64))(isSome(equalTo(i64(-5)))) &&
          assert(cast(f32(positiveFt), model.f64))(isSome(equalTo(f64(positiveFt.toDouble)))) &&
          assert(cast(f32(negativeFt), model.f64))(isSome(equalTo(f64(negativeFt.toDouble)))) &&
          assert(cast(f32(positiveFt), model.f32))(isSome(equalTo(f32(positiveFt)))) &&
          assert(cast(f32(negativeFt), model.f32))(isSome(equalTo(f32(negativeFt)))) &&
          assert(cast(f32(positiveFt), model.u32))(isSome(equalTo(u32(positiveFt.toInt)))) &&
          assert(cast(f32(negativeFt), model.u32))(isNone) &&
          assert(cast(f32(positiveFt), model.i32))(isSome(equalTo(i32(positiveFt.toInt)))) &&
          assert(cast(f32(negativeFt), model.i32))(isSome(equalTo(i32(negativeFt.toInt)))) &&
          assert(cast(f32(positiveFt), model.geo))(isNone) &&
          assert(cast(f32(positiveFt), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(f32(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(f32(positiveFt), model.timestamp))(isSome(equalTo(timestamp(positiveFt.toLong)))) &&
          assert(cast(f32(negativeFt), model.timestamp))(isNone) &&
          assert(cast(f32(negativeFt), model.str))(isSome(equalTo(str(negativeFt.toString))))
        },
        test("cast f64"){
          assert(cast(f64(positiveD), model.u64))(isSome(equalTo(u64(positiveD.toLong)))) &&
          assert(cast(f64(negativeD), model.u64))(isNone) &&
          assert(cast(f64(positiveD), model.i64))(isSome(equalTo(i64(positiveD.toLong)))) &&
          assert(cast(f64(negativeD), model.i64))(isSome(equalTo(i64(-5)))) &&
          assert(cast(f64(positiveD), model.f64))(isSome(equalTo(f64(positiveD)))) &&
          assert(cast(f64(negativeD), model.f64))(isSome(equalTo(f64(negativeD)))) &&
          assert(cast(f64(positiveD), model.f32))(isSome(equalTo(f32(positiveD.toFloat)))) &&
          assert(cast(f64(negativeD), model.f32))(isSome(equalTo(f32(negativeD.toFloat)))) &&
          assert(cast(f64(positiveD), model.u32))(isSome(equalTo(u32(positiveD.toInt)))) &&
          assert(cast(f64(negativeD), model.u32))(isNone) &&
          assert(cast(f64(positiveD), model.i32))(isSome(equalTo(i32(positiveD.toInt)))) &&
          assert(cast(f64(negativeD), model.i32))(isSome(equalTo(i32(negativeD.toInt)))) &&
          assert(cast(f64(positiveD), model.geo))(isNone) &&
          assert(cast(f64(positiveD), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(f64(0), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(f64(positiveD), model.timestamp))(isSome(equalTo(timestamp(positiveD.toLong)))) &&
          assert(cast(f64(negativeD), model.timestamp))(isNone) &&
          assert(cast(f64(negativeD), model.str))(isSome(equalTo(str(negativeD.toString))))
        },

        test("cast bool"){
          assert(cast(boolean(true), model.u64))(isSome(equalTo(u64(1L)))) &&
          assert(cast(boolean(false), model.u64))(isSome(equalTo(u64(0L)))) &&
          assert(cast(boolean(true), model.i64))(isSome(equalTo(i64(1L)))) &&
          assert(cast(boolean(false), model.i64))(isSome(equalTo(i64(0L)))) &&
          assert(cast(boolean(true), model.f64))(isSome(equalTo(f64(1d)))) &&
          assert(cast(boolean(false), model.f64))(isSome(equalTo(f64(0d)))) &&
          assert(cast(boolean(true), model.f32))(isSome(equalTo(f32(1f)))) &&
          assert(cast(boolean(false), model.f32))(isSome(equalTo(f32(0f)))) &&
          assert(cast(boolean(true), model.u32))(isSome(equalTo(u32(1)))) &&
          assert(cast(boolean(false), model.u32))(isSome(equalTo(u32(0)))) &&
          assert(cast(boolean(true), model.i32))(isSome(equalTo(i32(1)))) &&
          assert(cast(boolean(false), model.i32))(isSome(equalTo(i32(0)))) &&
          assert(cast(boolean(true), model.geo))(isNone) &&
          assert(cast(boolean(true), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(boolean(false), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(boolean(true), model.timestamp))(isNone) &&
          assert(cast(boolean(false), model.timestamp))(isNone) &&
          assert(cast(boolean(true), model.str))(isSome(equalTo(str("true")))) &&
          assert(cast(boolean(false), model.str))(isSome(equalTo(str("false"))))
        },

        test("cast timestamp"){
          assert(cast(timestamp(Long.MaxValue), model.u64))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.i64))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.f64))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.f32))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.u32))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.i32))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.timestamp))(isSome(equalTo(timestamp(Long.MaxValue)))) &&
          assert(cast(timestamp(Long.MinValue), model.timestamp))(isSome(equalTo(timestamp(Long.MinValue)))) && //отклонение, но такого не может быть
          assert(cast(timestamp(Long.MaxValue), model.boolean))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.str))(isNone) &&
          assert(cast(timestamp(Long.MaxValue), model.str))(isNone)
        },

        test("cast string"){
          assert(cast(str(Long.MaxValue.toString), model.u64))(isSome(equalTo(u64(Long.MaxValue)))) &&
          assert(cast(str(""), model.u64))(isNone) &&
          assert(cast(str("7.8"), model.u64))(isNone) &&
          assert(cast(str(Long.MinValue.toString), model.u64))(isNone) &&
          assert(cast(str((MaxU64 - 8).toString), model.u64))(isSome(equalTo(u64(-9)))) &&
          assert(cast(str(Long.MaxValue.toString), model.i64))(isSome(equalTo(i64(Long.MaxValue)))) &&
          assert(cast(str(""), model.i64))(isNone) &&
          assert(cast(str("7.8"), model.i64))(isNone) &&
          assert(cast(str(Long.MinValue.toString), model.i64))(isSome(equalTo(i64(Long.MinValue)))) &&
          assert(cast(str(Long.MaxValue.toString), model.f64))(isSome(equalTo(f64(Long.MaxValue.toDouble)))) &&
          assert(cast(str(""), model.f64))(isNone) &&
          assert(cast(str("7.8"), model.f64))(isSome(equalTo(f64(7.8d)))) &&
          assert(cast(str(Long.MinValue.toString), model.f64))(isSome(equalTo(f64(Long.MinValue.toDouble)))) &&
          assert(cast(str(Long.MaxValue.toString), model.f32))(isSome(equalTo(f32(Long.MaxValue.toFloat)))) &&
          assert(cast(str(""), model.f32))(isNone) &&
          assert(cast(str("7.8"), model.f32))(isSome(equalTo(f32(7.8f)))) &&
          assert(cast(str(Long.MinValue.toString), model.f32))(isSome(equalTo(f32(Long.MinValue.toFloat)))) &&
          assert(cast(str(Int.MaxValue.toString), model.u32))(isSome(equalTo(u32(Int.MaxValue)))) &&
          assert(cast(str(Long.MaxValue.toString), model.u32))(isNone) &&
          assert(cast(str(""), model.u32))(isNone) &&
          assert(cast(str("7.8"), model.u32))(isNone) &&
          assert(cast(str(Int.MinValue.toString), model.u32))(isNone) &&
          assert(cast(str((MaxU32 - 7).toString), model.u32))(isSome(equalTo(u32(-8)))) &&
          assert(cast(str(Long.MinValue.toString), model.u32))(isNone) &&
          assert(cast(str(Int.MaxValue.toString), model.i32))(isSome(equalTo(i32(Int.MaxValue)))) &&
          assert(cast(str(Long.MaxValue.toString), model.i32))(isNone) &&
          assert(cast(str(""), model.i32))(isNone) &&
          assert(cast(str("7.8"), model.i32))(isNone) &&
          assert(cast(str(Int.MinValue.toString), model.i32))(isSome(equalTo(i32(Int.MinValue)))) &&
          assert(cast(str(Long.MinValue.toString), model.i32))(isNone) &&
          assert(cast(str(""), model.geo))(isNone) &&
          assert(cast(str("true"), model.boolean))(isSome(equalTo(boolean(true)))) &&
          assert(cast(str("false"), model.boolean))(isSome(equalTo(boolean(false)))) &&
          assert(cast(str("0"), model.boolean))(isNone) &&
          assert(cast(str(""), model.boolean))(isNone) &&
          assert(cast(str(Long.MaxValue.toString), model.timestamp))(isSome(equalTo(timestamp(Long.MaxValue)))) &&
          assert(cast(str(""), model.timestamp))(isNone) &&
          assert(cast(str("7.8"), model.timestamp))(isNone) &&
          assert(cast(str(Long.MinValue.toString), model.timestamp))(isNone) &&
          assert(cast(str("khbibigbibb"), model.str))(isSome(equalTo(str("khbibigbibb"))))
        }
        // format: on
      ),
      suite("restore")(
        test("") {
          assert(restore("", model.u64))(
            isSome(equalTo(timestamp(Long.MinValue))),
          )
        },
      ) @@ ignore,
    )

}

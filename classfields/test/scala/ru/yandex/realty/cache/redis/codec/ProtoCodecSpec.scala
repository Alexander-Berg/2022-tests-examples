package ru.yandex.realty.cache.redis.codec

import com.google.protobuf.{Message, Option => ProtoOption}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.protobuf.ProtoMacro.?
import ru.yandex.vertis.protobuf.{ProtoInstance, ProtoInstanceProvider}

@RunWith(classOf[JUnitRunner])
class ProtoCodecSpec extends WordSpec with Matchers with ProtoInstanceProvider {

  private def checkSpec[M <: Message: ProtoInstance](m: M, show: M => String, compare: (M, M) => Boolean) =
    withClue(s"on '${show(m)}'") {
      val codec = new ProtoCodec[M]

      compare(
        m,
        codec.decodeKey(codec.encodeKey(m))
      ) shouldBe true
    }

  private def checkOption(o: ProtoOption) =
    checkSpec[ProtoOption](
      o,
      m => s"Option(name='${m.getName}'${?(m.getValue).map(_.getTypeUrl).map(",url=" + _)})", { (expected, actual) =>
        expected.getName == actual.getName &&
        (expected.hasValue == actual.hasValue) &&
        (!actual.hasValue || actual.getValue.getTypeUrl == expected.getValue.getTypeUrl)
      }
    )

  "ProtoCodec" should {
    "correctly encode and decode" in {
      for {
        _case <- Seq(
          ProtoOption.getDefaultInstance,
          ProtoOption.newBuilder().setName("name").build(),
          ProtoOption
            .newBuilder()
            .setName("name")
            .setValue(
              com.google.protobuf.Any
                .newBuilder()
                .setTypeUrl("url")
            )
            .build()
        )
      } checkOption(_case)
    }
  }

}

package ru.auto.api.util.json

import akka.util.ByteString
import com.google.common.base.Charsets
import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 26.02.18
  */
class ByteStringAppendableTest extends BaseSpec {
  "ByteStringsAppendable" should {
    "build ByteString by appending strings" in {
      val text = "Hello world"
      val charset = Charsets.UTF_8

      val appendable = new ByteStringAppendable(1024)
      appendable.write(text.getBytes(charset))

      val result = appendable.result()

      result shouldBe ByteString.fromString(text, charset)
    }

    "build ByteString divided by chunks" in {
      val text1 = "Hello world"
      val text2 = "Some text"
      val charset = Charsets.UTF_8

      val appendable = new ByteStringAppendable(5)
      appendable.write(text1.getBytes(charset))
      appendable.write(text2.getBytes(charset))

      val result = appendable.result()

      result shouldBe ByteString.fromString(text1 + text2, charset)
      result should not be Symbol("compact")
    }
  }

}

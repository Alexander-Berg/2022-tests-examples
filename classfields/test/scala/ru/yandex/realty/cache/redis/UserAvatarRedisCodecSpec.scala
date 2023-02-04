package ru.yandex.realty.cache.redis

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

@RunWith(classOf[JUnitRunner])
class UserAvatarRedisCodecSpec extends AsyncSpecBase {

  "UserAvatarRedisCodec" should {
    "encode/decode simple key" in {
      val key = "simpleKey"
      val res = UserAvatarRedisCodec.decodeKey(
        UserAvatarRedisCodec.encodeKey(key)
      )
      res should equal(key)
    }

    "encode/decode  Some(value)" in {
      val value = Some("simpleValue")
      UserAvatarRedisCodec.decodeValue(
        UserAvatarRedisCodec.encodeValue(value)
      ) should equal(value)
    }

    "encode/decode  None value" in {
      val value: Option[String] = None
      UserAvatarRedisCodec.decodeValue(
        UserAvatarRedisCodec.encodeValue(value)
      ) should equal(value)
    }

    "encode/decode  Some('') value" in {
      val value: Option[String] = Some("")
      UserAvatarRedisCodec.decodeValue(
        UserAvatarRedisCodec.encodeValue(value)
      ) should equal(value)
    }
  }

}

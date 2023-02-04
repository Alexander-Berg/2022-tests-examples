package ru.yandex.vertis.general.common.ab

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec, JsonKey}

@ConfiguredJsonCodec
case class TestBucket(@JsonKey("Testid") testId: Option[String] = None, @JsonKey("Bucket") bucket: Option[Int] = None)

object TestBucket {
  implicit val customConfig: Configuration = Configuration.default.withDefaults
}

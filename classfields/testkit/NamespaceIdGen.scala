package ru.yandex.vertis.general.feed.model.testkit

import ru.yandex.vertis.general.feed.model.NamespaceId
import zio.random.Random
import zio.test.{Gen, Sized}

object NamespaceIdGen {

  def anyNamespaceId(suffix: String = ""): Gen[Random with Sized, NamespaceId] = for {
    id <- Gen.alphaNumericString
  } yield NamespaceId(id + suffix)

}

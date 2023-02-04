package ru.yandex.vertis.general.feed.model.testkit

import java.time.Instant
import java.time.temporal.ChronoUnit
import ru.yandex.vertis.general.common.errors.FatalFeedErrors
import ru.yandex.vertis.general.feed.model.{FatalErrorAction, FatalErrorInfo, FeedTask}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen
import zio.test.magnolia.DeriveGen.instance

object FeedTaskGen {

  import ru.yandex.vertis.general.common.model.user.testkit.SellerGen.anySellerIdDeriveGen

  implicit val genFatalErrorInfo: DeriveGen[FatalErrorInfo] = instance {
    for {
      message <- Gen.alphaNumericStringBounded(5, 15)
      description <- Gen.alphaNumericStringBounded(10, 100)
      code <- Gen.int(0, FatalFeedErrors.values.size - 1).map(idx => FatalFeedErrors.values.toList(idx))
      action <- Gen.option(DeriveGen[FatalErrorAction])
    } yield FatalErrorInfo(message, description, code, action)
  }

  implicit val genInstantMs: DeriveGen[Instant] = {
    val to = Instant.now()
    val from = to.minus(1, ChronoUnit.DAYS)

    instance(
      Gen.instant(from, to).map(_.truncatedTo(ChronoUnit.MILLIS)) // Postgres do not support nanosecond precision
    )
  }
  val any = DeriveGen[FeedTask]

  val list = DeriveGen.genList[FeedTask].derive
}

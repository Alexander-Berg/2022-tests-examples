package ru.yandex.vertis.punisher.dao.impl

import cats.effect.Clock
import cats.syntax.functor._
import cats.syntax.flatMap._
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.config.EventEodConfig
import ru.yandex.vertis.punisher.model.TaskContext.Batch
import ru.yandex.vertis.punisher.model.TaskDomain
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl
import ru.yandex.vertis.quality.cats_utils.Awaitable._

@Ignore
@RunWith(classOf[JUnitRunner])
class RealtyYqlOffersDaoImplSpec extends BaseSpec {
  implicit private val clock: Clock[F] = Clock.create[F]

  private val config =
    YqlExecutorConfig(
      "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      "dvarygin",
      "",
      queryConfig = YqlQueryConfig(tmpFolder = Some("//home/verticals/.tmp"))
    )

  private val daoConfig =
    EventEodConfig(
      eventsFolder = "//home/verticals/broker/prod/warehouse/holocron/realty/full/events/1d",
      eodFolder = "//home/verticals/broker/prod/warehouse/holocron/realty/full/eod/1d"
    )

  val executorF = YqlQueryExecutorImpl.initialize(config)

  "RealtyYqlOffersDaoImpl" should {
    "find offers" in {
      val interval =
        TimeInterval(
          DateTimeUtils.now.minusHours(2),
          DateTimeUtils.now.minusHours(1)
        )
      implicit val batch = Batch(taskDomain = TaskDomain.Unknown, timeInterval = interval)

      val getOffers =
        for {
          exec <- executorF
          dao = new RealtyYqlOffersDaoImpl[F](exec, daoConfig)
          offers <- dao.offers(Set("987228093"))
        } yield offers

      val offers = getOffers.await
      println(offers.size)
      println(offers)
    }
  }
}

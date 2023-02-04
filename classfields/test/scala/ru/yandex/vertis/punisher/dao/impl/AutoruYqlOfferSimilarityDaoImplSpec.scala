package ru.yandex.vertis.punisher.dao.impl

import cats.effect.IO
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.dao.OfferSimilarityDao
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomain, TaskDomainImpl}
import ru.yandex.vertis.punisher.util.DateTimeUtils.{DefaultZoneId, TimeInterval}
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.yql_utils.YqlQueryExecutor
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl

import java.time.ZonedDateTime

@Ignore
@RunWith(classOf[JUnitRunner])
class AutoruYqlOfferSimilarityDaoImplSpec extends BaseSpec {

  private val config =
    YqlExecutorConfig(
      "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      "robot-vsq-test",
      "*"
    )

  private val executorF: F[YqlQueryExecutor[F]] = YqlQueryExecutorImpl.initialize(config)

  private val createDao: F[AutoruYqlOfferSimilarityDaoImpl[F]] =
    executorF.map { executor =>
      new AutoruYqlOfferSimilarityDaoImpl(
        yqlQueryExecutor = executor,
        imagesFolder = "//home/verticals/broker/prod/warehouse/auto/images/meta/1d",
        offersEventsFolder = "//home/verticals/broker/prod/warehouse/holocron/auto/full/cars/events/1d",
        offersEodFolder = "//home/verticals/broker/prod/warehouse/holocron/auto/full/cars/eod/1d",
        category = OfferSimilarityDao.HolocronCategory.Auto
      )
    }

  "AutoruYqlOfferSimilarityDaoImpl" should {
    "find similar offers" in {
      val ctx =
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.Cbir),
          timeInterval =
            TimeInterval(
              ZonedDateTime.of(2021, 8, 11, 12, 0, 0, 0, DefaultZoneId),
              ZonedDateTime.of(2021, 8, 11, 12, 30, 0, 0, DefaultZoneId)
            )
        )
      val getResult = createDao.flatMap(_.get(ctx))
      val result = getResult.await
      println(result.size)
      println(result)
    }
  }
}

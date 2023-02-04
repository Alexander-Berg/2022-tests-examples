package auto.dealers.trade_in_notifier.storage.postgres.test

import auto.dealers.trade_in_notifier.storage.postgres.PgTradeInMatcherErrorLog
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import common.zio.ops.tracing.testkit.TestTracing
import doobie._
import doobie.postgres.implicits._
import doobie.implicits._
import io.circe.Json
import io.circe.syntax._
import scalapb_circe.JsonFormat.protoToEncoder
import zio.test.Assertion._
import zio.test._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Has, Task, URIO, ZIO}
import zio.interop.catz._
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.subscriptions.model.Document
import auto.dealers.trade_in_notifier.storage.TradeInMatcherErrorLog

object TradeInMatcherErrorLogSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[Environment, Failure] = {
    val suit = suite("TradeInMatcherErrorLogSpec")(
      appendTest
    ) @@ beforeAll(dbInit) @@ after(dbClean) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor ++ TestTracing.noOp >+> PgTradeInMatcherErrorLog.live
    )
  }

  val appendTest = testM("append log") {
    val offer = Offer.defaultInstance
    val doc = Document.defaultInstance
    val err = "bad offer"
    val expected = (
      offer.asJson.noSpaces,
      Some(doc.asJson.noSpaces),
      offer.toByteArray.toSeq,
      None,
      err
    )

    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- TradeInMatcherErrorLog.append(offer, Some(doc), None, err)
      (offer, doc, reqShard, reqMatcher, err) <-
        sql"SELECT offer, document, shard_request, matcher_request, error_message FROM trade_in_matcher_error_log"
          .query[(String, Option[String], Array[Byte], Option[Array[Byte]], String)]
          .unique
          .transact(xa)
      res = (offer, doc, reqShard.toSeq, reqMatcher.map(_.toSeq), err)
    } yield assertTrue(res == expected)
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_matcher_error_log".update.run.transact(xa)
    }
}

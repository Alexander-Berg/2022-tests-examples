package ru.yandex.vertis.moderation.dao.impl.ydb

import monix.execution.FutureUtils
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.service.impl.PhoneIndexServiceImpl
import ru.yandex.vertis.moderation.util.FutureUtil
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class YdbPhoneIndexDaoSpec extends YdbSpecBase {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override val resourceSchemaFileName: String = "/phones-index.sql"
  lazy val dao = new YdbPhoneIndexDao[F, WithTransaction[F, *]](ydbWrapper)

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM indexed_phones;")).await)
  }

  val externalId = ExternalIdGen.next
  val externalId2 = ExternalIdGen.next

  "YdbPhoneIndexDao" should {
    "index data" in {
      dao.updateIndexes(Set("123", "456", "678"), Set.empty, externalId).futureValue
      dao.updateIndexes(Set("789", "001"), Set.empty, externalId2).futureValue
      val res = dao.getInstancesIds("123").futureValue
      res.head should be(externalId)
    }

    "reindex data" in {
      dao.updateIndexes(Set("123", "456", "678"), Set.empty, externalId).futureValue
      dao.updateIndexes(Set("222"), Set("123"), externalId).futureValue

      val res = dao.getInstancesIds("123").futureValue
      res.size should be(0)
      val res2 = dao.getInstancesIds("222").futureValue
      res2.head should be(externalId)
    }

    "cursor query" in {
      val ids = (1 to 1200).map(_ => ExternalIdGen.next)

      FutureUtil.batchTraverse(ids, 10)(id => dao.updateIndexes(Set("123"), Set.empty, id)).futureValue
      val res = dao.getInstancesIds("123").futureValue
      res.size should be(1200)
    }
  }
}

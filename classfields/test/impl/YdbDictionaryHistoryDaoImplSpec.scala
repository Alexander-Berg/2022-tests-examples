package ru.yandex.vertis.shark.dao.impl

import cats.implicits._
import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import ru.yandex.vertis.shark.dao.DictionaryHistoryDao
import ru.yandex.vertis.shark.dao.DictionaryHistoryDao._
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.proto.model.DictionaryUpdate.UpdateType
import ru.yandex.vertis.zio_baker.app.context
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.ZLayer
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, sequential}

import java.time.Instant

object YdbDictionaryHistoryDaoImplSpec extends DefaultRunnableSpec {

  private lazy val dictionaryHistoryDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> DictionaryHistoryDao.live

  private val timestamp = Instant.now
  private val updateType = UpdateType.CREATE
  private val operator = "operator".taggedWith[context.Tag.YandexUid]
  private val comment = "comment"

  private val bankPalmaKey: PalmaKey = "bankId".taggedWith[Tag.PalmaKey]

  private val bankDictionaryUpdate: DictionaryUpdate =
    DictionaryUpdate(
      key = bankPalmaKey,
      timestamp = timestamp,
      updateType = updateType,
      operator = operator.some,
      comment = comment.some,
      entity = None
    )

  private val creditProductPalmaKey: PalmaKey = "creditProductId".taggedWith[Tag.PalmaKey]

  private val creditProductDictionaryUpdate: DictionaryUpdate =
    DictionaryUpdate(
      key = creditProductPalmaKey,
      timestamp = timestamp,
      updateType = updateType,
      operator = operator.some,
      comment = comment.some,
      entity = None
    )

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("YdbDictionaryHistoryDaoImpl")(
      testM("upsert bank") {
        val res = transactionally(upsert(DictionaryType.Bank, bankDictionaryUpdate)).as(true)
        assertM(res)(isTrue)
      },
      testM("list bank") {
        val res = transactionally(list(DictionaryType.Bank, bankPalmaKey, None))
        assertM(res)(equalTo(Seq(bankDictionaryUpdate)))
      },
      testM("upsert credit product") {
        val res = transactionally(upsert(DictionaryType.CreditProduct, creditProductDictionaryUpdate)).as(true)
        assertM(res)(isTrue)
      },
      testM("list credit product") {
        val res = transactionally(list(DictionaryType.CreditProduct, key = creditProductPalmaKey, pagination = None))
        assertM(res)(equalTo(Seq(creditProductDictionaryUpdate)))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(dictionaryHistoryDaoLayer)
  }
}

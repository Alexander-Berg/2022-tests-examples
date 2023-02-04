package ru.auto.comeback.storage.sender_log

import common.zio.doobie.testkit.TestPostgresql
import ru.auto.comeback.model.testkit.{CommonGen, SenderLogGen}
import ru.auto.comeback.storage.Schema
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, checkM, DefaultRunnableSpec, Gen}

object DbSenderLogDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("SenderLogDao")(
      selectFromEmptyDb,
      selectExisting
    ) @@ after(Schema.cleanup) @@ beforeAll(Schema.init) @@ sequential @@ sized(10) @@ shrinks(0)
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >+> SenderLogDao.live
  )

  private val selectFromEmptyDb = testM("select from empty db") {
    checkM(CommonGen.anyYandexEmail, Gen.long(1, Long.MaxValue))((mail, comebackId) =>
      for {
        _ <- Schema.cleanup
        dao <- ZIO.service[SenderLogDao.Service]
        found <- dao.getBy(email = mail, comebackId = comebackId)
      } yield assert(found)(isNone)
    )
  }

  private val selectExisting = testM("select existing from") {
    checkM(SenderLogGen.anyNewSenderLogRecord)(record =>
      for {
        _ <- Schema.cleanup
        dao <- ZIO.service[SenderLogDao.Service]
        inserted <- dao.insert(record)
        found <- dao.getBy(email = record.email, comebackId = record.comebackId)
      } yield assert(found)(equalTo(Some(inserted)))
    )
  }
}

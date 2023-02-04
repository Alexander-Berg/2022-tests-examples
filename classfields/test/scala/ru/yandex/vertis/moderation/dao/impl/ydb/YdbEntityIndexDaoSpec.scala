package ru.yandex.vertis.moderation.dao.impl.ydb

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.model.index._
import ru.yandex.vertis.moderation.model.instance.ExternalId
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext
import scala.util.Try

class YdbEntityIndexDaoSpec extends YdbSpecBase with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  override val resourceSchemaFileName: String = "/entity-index.sql"
  lazy val dao = new YdbEntityIndexDao[F, WithTransaction[F, *]](ydbWrapper, limit = 5)

  override def beforeEach(): Unit = {
    super.beforeEach()
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM entity_index;")).await)
  }

  def upsertReq(entityId: String,
                externalId: String,
                conv: String => IndexedEntity = CvHashEntity
               ): UpsertIndexRequest = UpsertIndexRequest(conv(entityId), ExternalId(externalId))

  def deleteReq(entityId: String, externalId: String): DeleteIndexRequest =
    DeleteIndexRequest(CvHashEntity(entityId), ExternalId(externalId))

  "YdbEntityIndexDao" should {
    "upsert data" in {
      val requests = Seq(upsertReq("hash1", "yandex_uid_1#object1"))

      (for {
        _   <- ydbWrapper.runTx(dao.updateEntities(requests))
        ids <- dao.getRows(Set(CvHashEntity("hash1")))
      } yield {
        ids.length shouldBe 1
        val (id, extId, _) = ids.head
        id shouldBe "hash1"
        extId.id shouldBe "yandex_uid_1#object1"
      }).unsafeRunSync()
    }

    "rewrite data" in {
      val requests = Seq(upsertReq("hash1", "yandex_uid_1#object1"))

      (for {
        _         <- ydbWrapper.runTx(dao.updateEntities(requests))
        firstIds  <- dao.getRows(Set("hash1").map(CvHashEntity.apply))
        _         <- ydbWrapper.runTx(dao.updateEntities(requests))
        secondIds <- dao.getRows(Set("hash1").map(CvHashEntity.apply))
      } yield {
        firstIds.length shouldBe 1
        secondIds.length shouldBe 1
        val (id1, extId1, ts1) = firstIds.head
        val (id2, extId2, ts2) = secondIds.head
        id1 shouldBe id2
        extId1 shouldBe extId2
        (ts2.getMillis - ts1.getMillis) should be > 0L
      }).unsafeRunSync()
    }

    "delete data" in {
      val requestsUpsert =
        Seq(
          upsertReq("hash1", "yandex_uid_1#object1"),
          upsertReq("hash1", "yandex_uid_1#object2")
        )
      val requestsDelete = Seq(deleteReq("hash1", "yandex_uid_1#object1"))

      (for {
        _   <- ydbWrapper.runTx(dao.updateEntities(requestsUpsert))
        _   <- ydbWrapper.runTx(dao.updateEntities(requestsDelete))
        ids <- dao.getRows(Set("hash1").map(CvHashEntity.apply))
      } yield {
        ids.length shouldBe 1
        val (id, extId, _) = ids.head
        id shouldBe "hash1"
        extId.id shouldBe "yandex_uid_1#object2"
      }).unsafeRunSync()
    }

    "process empty queries" in {
      (for {
        _   <- ydbWrapper.runTx(dao.updateEntities(Nil))
        ids <- dao.getRows(Set.empty[String].map(CvHashEntity.apply))
      } yield ids.length shouldBe 0).unsafeRunSync()
    }

    "filter data" in {
      val requests =
        Seq(
          upsertReq("hash1", "yandex_uid_1#object1"),
          upsertReq("hash2", "yandex_uid_1#object2", PhoneEntity),
          upsertReq("hash2", "yandex_uid_1#object3", PhoneEntity),
          upsertReq("hash3", "yandex_uid_1#object4"),
          upsertReq("hash3", "yandex_uid_1#object5"),
          upsertReq("hash3", "yandex_uid_1#object6")
        )

      (for {
        _    <- ydbWrapper.runTx(dao.updateEntities(requests))
        ids1 <- dao.getRows(Set("hash1").map(CvHashEntity.apply))
        ids2 <- dao.getRows(Set("hash2").map(PhoneEntity.apply))
        ids3 <- dao.getRows(Set("hash1", "hash3").map(CvHashEntity.apply))
        ids4 <- dao.getRows(Set("hash1").map(PhoneEntity.apply))
        ids5 <- dao.getRows(Set("hash2").map(CvHashEntity.apply))
      } yield {
        ids1.length shouldBe 1
        ids2.length shouldBe 2
        ids3.length shouldBe 4
        ids4.length shouldBe 0
        ids5.length shouldBe 0
        ids1.head._2.id shouldBe "yandex_uid_1#object1"
        ids2.map(_._2.id) shouldBe Seq("yandex_uid_1#object2", "yandex_uid_1#object3")
        ids3.map(_._2.id).toSet shouldBe Set(
          "yandex_uid_1#object1",
          "yandex_uid_1#object4",
          "yandex_uid_1#object5",
          "yandex_uid_1#object6"
        )
      }).unsafeRunSync()
    }

    "paginate" in {
      val requests = (1 to 27).map(i => upsertReq("hash1", s"yandex_uid_1#object$i"))

      (for {
        _   <- ydbWrapper.runTx(dao.updateEntities(requests))
        ids <- dao.getRows(Set("hash1").map(CvHashEntity.apply))
      } yield {
        ids.length shouldBe 27
        ids.map(_._2).toSet.size shouldBe 27
      }).unsafeRunSync()
    }
  }
}

package ru.yandex.vos2.autoru.dao.blockedphotohashes

import org.joda.time.DateTimeUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.vos.BlockedPhotoHashModel.BlockedPhotoHash
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs

class BlockedPhotoHashesDaoImplTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll with Matchers {

  implicit private val trace: Traced = Traced.empty
  private val now = 1654087133931L
  private lazy val blockedPhotoHashesDao: BlockedPhotoHashesDao = components.blockedPhotoHashesDao

  private val blockedPhotoHash1 = BlockedPhotoHash
    .newBuilder()
    .setValue("test1")
    .setUserId("user1")
    .setOfferId("offer1")
    .setScope(BlockedPhotoHash.Scope.OFFER)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url1")
    .build()

  val update1_1 = BlockedPhotoHashesDao.Update(
    userId = Some("user1"),
    scope = Some(BlockedPhotoHash.Scope.OFFER),
    status = Some(BlockedPhotoHash.Status.ACTIVE),
    photoUrl = Some("photo_url1"),
    operator = Some("operator1"),
    comment = Some("initial")
  )

  val history1_1 = BlockedPhotoHash.History
    .newBuilder()
    .setScope(BlockedPhotoHash.Scope.OFFER)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url1")
    .setOperator("operator1")
    .setTs(now)
    .setComment("initial")
    .build()

  val update1_2 = BlockedPhotoHashesDao.Update(
    userId = None,
    scope = None,
    status = Some(BlockedPhotoHash.Status.DISABLED),
    photoUrl = None,
    operator = Some("operator2"),
    comment = Some("disabling")
  )

  val history1_2 = BlockedPhotoHash.History
    .newBuilder()
    .setStatus(BlockedPhotoHash.Status.DISABLED)
    .setOperator("operator2")
    .setTs(now)
    .setComment("disabling")
    .build()

  // ---

  val blockedPhotoHash2 = BlockedPhotoHash
    .newBuilder()
    .setValue("test2")
    .setUserId("user2")
    .setOfferId("offer2")
    .setScope(BlockedPhotoHash.Scope.SERVICE)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url2")
    .build()

  val update2_1 = BlockedPhotoHashesDao.Update(
    userId = Some("user2"),
    scope = Some(BlockedPhotoHash.Scope.SERVICE),
    status = Some(BlockedPhotoHash.Status.ACTIVE),
    photoUrl = Some("photo_url2"),
    operator = Some("operator1"),
    comment = Some("initial")
  )

  val history2_1 = BlockedPhotoHash.History
    .newBuilder()
    .setScope(BlockedPhotoHash.Scope.SERVICE)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url2")
    .setOperator("operator1")
    .setTs(now)
    .setComment("initial")
    .build()

  // ---

  val blockedPhotoHash3 = BlockedPhotoHash
    .newBuilder()
    .setValue("test2")
    .setUserId("user3")
    .setOfferId("offer3")
    .setScope(BlockedPhotoHash.Scope.USER)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url2")
    .build()

  val update3_1 = BlockedPhotoHashesDao.Update(
    userId = Some("user3"),
    scope = Some(BlockedPhotoHash.Scope.USER),
    status = Some(BlockedPhotoHash.Status.ACTIVE),
    photoUrl = Some("photo_url2"),
    operator = Some("operator1"),
    comment = Some("initial")
  )

  val history3_1 = BlockedPhotoHash.History
    .newBuilder()
    .setScope(BlockedPhotoHash.Scope.USER)
    .setStatus(BlockedPhotoHash.Status.ACTIVE)
    .setPhotoUrl("photo_url2")
    .setOperator("operator1")
    .setTs(now)
    .setComment("initial")
    .build()

  override def beforeAll(): Unit = {
    components.skypper.transaction("truncate-blocked-photo-hashes") { executor =>
      executor.update("truncate-blocked-photo-hashes")("delete from blocked_photo_hashes;")
    }
    DateTimeUtils.setCurrentMillisFixed(now)
  }

  override def afterAll(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  test("insert first and get all") {
    val expected = blockedPhotoHash1.toBuilder.addHistory(history1_1).build()
    blockedPhotoHashesDao.upsertHash("test1", "offer1", update1_1)
    val result = blockedPhotoHashesDao.getHashes(afterValue = None, limitCount = 10)
    result.size shouldBe 1
    result.head shouldBe expected
  }

  test("update and get first") {
    val expected = blockedPhotoHash1.toBuilder
      .setStatus(BlockedPhotoHash.Status.DISABLED)
      .addHistory(history1_1)
      .addHistory(history1_2)
      .build()
    blockedPhotoHashesDao.upsertHash("test1", "offer1", update1_2)
    val result = blockedPhotoHashesDao.getHashes(Seq("test1"))
    result.size shouldBe 1
    result.head shouldBe expected
  }

  test("insert and get second") {
    val expected = blockedPhotoHash2.toBuilder.addHistory(history2_1).build()
    blockedPhotoHashesDao.upsertHash("test2", "offer2", update2_1)
    val result1 = blockedPhotoHashesDao.getHashes(afterValue = None, limitCount = 10)
    result1.size shouldBe 2
    val result2 = blockedPhotoHashesDao.getHashes(Seq("test2"))
    result2.size shouldBe 1
    result2.head shouldBe expected
  }

  test("insert and get third") {
    val expected1 = blockedPhotoHash2.toBuilder.addHistory(history2_1).build()
    val expected2 = blockedPhotoHash3.toBuilder.addHistory(history3_1).build()
    blockedPhotoHashesDao.upsertHash("test2", "offer3", update3_1)
    val result1 = blockedPhotoHashesDao.getHashes(afterValue = None, limitCount = 10)
    result1.size shouldBe 3
    val result2 = blockedPhotoHashesDao.getHashes(Seq("test2"))
    result2.size shouldBe 2
    result2 shouldBe List(
      expected1,
      expected2
    )
  }

  test("get second page") {
    val result = blockedPhotoHashesDao.getHashes(afterValue = Some("test1"), limitCount = 1)
    result.size shouldBe 1
    result.head.getValue shouldBe "test2"
  }

}

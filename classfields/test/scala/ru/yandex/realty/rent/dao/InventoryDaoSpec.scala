package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.util.{Page, SlicedResult}
import ru.yandex.realty.rent.model.Inventory
import ru.yandex.realty.sharding.Shard
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, GroupStatus}

import scala.concurrent.Future
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class InventoryDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit private val traced: Traced = Traced.empty
  private val NotExistingOwnerRequestId = ""

  "InventoryDao" should {

    "create and find inventory by owner request id" in {
      val confirmedInsertedInventory = insertInventory(None, isConfirmed = true)
      val inventory = insertInventory(None, isConfirmed = false)

      val foundConfirmedInventory =
        inventoryDao.findLastConfirmedByOwnerRequestId(confirmedInsertedInventory.ownerRequestId).futureValue
      val foundLastInventory = inventoryDao.findLastByOwnerRequestId(inventory.ownerRequestId).futureValue

      foundConfirmedInventory should equal(Some(confirmedInsertedInventory))
      foundLastInventory should equal(Some(inventory))
    }

    "return None if there is no given owner request id in the table" in {
      val none = inventoryDao.findLastConfirmedByOwnerRequestId(NotExistingOwnerRequestId).futureValue

      none shouldBe None
    }

    "update existing inventory" in {
      val insertedInventory = insertInventory(None, isConfirmed = false)
      val ownerRequestId = insertedInventory.ownerRequestId

      val updatedInventory = inventoryDao
        .update(ownerRequestId) { inventory =>
          val data = inventory.data
          val updatedData = data.toBuilder
            .clearConfirmedByTenantDate()
            .setManagerComment("not confirmed")
            .build

          inventory.copy(data = updatedData)
        }
        .futureValue
      val foundInventory = inventoryDao.findLastByOwnerRequestId(ownerRequestId).futureValue

      foundInventory should equal(updatedInventory)
    }

    "find collection of inventory by owner request ids" in {
      val insertedInventories = (1 to 5).map(_ => insertInventory(None, isConfirmed = true))
      val ownerRequestIds = insertedInventories.map(_.ownerRequestId)

      val foundInventories = inventoryDao.findByOwnerRequestIds(ownerRequestIds).futureValue

      foundInventories should contain theSameElementsAs insertedInventories
    }

    "find collection of confirmed inventory by owner request id" in {
      testFindCollectionOfInventoriesByOwnerRequestId(
        isConfirmed = Some(true),
        inventoryDao.findConfirmedByOwnerRequestId
      )
    }

    "find collection of all inventory by owner request id" in {
      testFindCollectionOfInventoriesByOwnerRequestId(
        isConfirmed = None,
        inventoryDao.findAllByOwnerRequestId
      )
    }

    "find certain version of inventory for owner request id" in {
      val ownerRequestId = readableString.next
      val count = 5
      val insertedInventories = insertInventories(None, ownerRequestId, count, isConfirmed = false)
      val inventoryToFind = insertedInventories(new Random().nextInt(insertedInventories.size))

      val foundInventories =
        inventoryDao.findByVersionAndOwnerRequestId(inventoryToFind.version, ownerRequestId).futureValue

      foundInventories shouldBe Some(inventoryToFind)
    }

    "find last confirmed before version" in {
      val ownerRequestId = readableString.next
      val firstConfirmed = setConfirmedDates(inventoryGen.next.copy(version = 0, ownerRequestId = ownerRequestId))
      val secondConfirmed = setConfirmedDates(inventoryGen.next.copy(version = 2, ownerRequestId = ownerRequestId))
      val notConfirmed = inventoryGen.next.copy(version = 5, ownerRequestId = ownerRequestId)
      val inventoriesToInsert = Seq(firstConfirmed, secondConfirmed)

      insertInventories(Some(inventoriesToInsert), ownerRequestId, inventoriesToInsert.size, isConfirmed = true)
      insertInventory(Some(notConfirmed), isConfirmed = false)

      val foundInventory = inventoryDao.findLastConfirmedBeforeVersion(version = 8, ownerRequestId).futureValue

      foundInventory shouldBe Some(secondConfirmed)
    }

    "watch" in {
      val now = DateTimeUtil.now().minusMinutes(1)
      val firsShardInventories = inventoryGen.next(5).map(_.copy(visitTime = Some(now), shardKey = 0))
      val secondShardInventories = inventoryGen.next(2).map(_.copy(visitTime = Some(now), shardKey = 1))

      (firsShardInventories ++ secondShardInventories).foreach(
        i => insertInventory(Some(i), isConfirmed = Random.nextBoolean())
      )

      val watchSize = inventoryDao.watchSize(Shard(1, 2)).futureValue
      val result = inventoryDao.watch(10, Shard(0, 2))(i => Future.successful(i)).futureValue

      watchSize shouldBe secondShardInventories.size
      result.processedCount shouldBe firsShardInventories.size
    }

    "save confirmed inventory and change showing status" in {
      val insertedInventory = insertInventory(None, isConfirmed = false)
      val ownerRequestId = insertedInventory.ownerRequestId
      val flatShowing = {
        val sh = flatShowingGen(Some(FlatShowingStatus.NewShowing)).next
        sh.copy(ownerRequestId = ownerRequestId, groupStatus = GroupStatus.Approved)
      }

      flatShowingDao.insert(Seq(flatShowing)).futureValue
      val result = inventoryDao.confirm(ownerRequestId, withUpdate = true)(_.confirmByOwner.confirmByTenant).futureValue
      val showing = flatShowingDao.find(Seq(flatShowing.showingId)).futureValue

      result.map(_.isConfirmed) shouldBe Some(true)
      showing.exists(_.status == FlatShowingStatus.Settled) shouldBe true
      result.map(_.version) shouldBe Some(insertedInventory.version)
    }

    "save confirmed by owner inventory" in {
      val insertedInventory = insertInventory(None, isConfirmed = false)
      val ownerRequestId = insertedInventory.ownerRequestId

      val result = inventoryDao.confirm(ownerRequestId, withUpdate = true)(_.confirmByOwner).futureValue

      result.map(_.isConfirmedByOwnerOnly) shouldBe Some(true)
      result.map(_.version) shouldBe Some(insertedInventory.version + 1)
    }
  }

  private def testFindCollectionOfInventoriesByOwnerRequestId(
    isConfirmed: Option[Boolean],
    findFunction: (String, Page) => Future[SlicedResult[Inventory]]
  ) = {
    val total = 50
    val confirmedCount = total - 20
    val notConfirmedCount = total - 30
    val startPageNm = 0
    val endPageNum = 5
    val pageSize = 10
    val emptyPageSize = 0

    val ownerRequestId = readableString.next
    val page = Page(startPageNm, pageSize)
    val insertedInventories = isConfirmed match {
      case Some(isConfirmed) => insertInventories(None, ownerRequestId, total, isConfirmed = isConfirmed)
      case _ =>
        insertInventories(None, ownerRequestId, confirmedCount, isConfirmed = true) :::
          insertInventories(None, ownerRequestId, notConfirmedCount, isConfirmed = false)
    }

    val slicedInventories =
      (startPageNm to endPageNum).map(
        n => findFunction(ownerRequestId, page.copy(number = n)).futureValue
      )

    val firstElement = slicedInventories(0).head
    val pageTwo = slicedInventories(1)
    val pageToFirstElement = pageTwo.head
    val foundInventories = slicedInventories.flatMap(_.values)
    val lastPage = slicedInventories.reverse.head

    slicedInventories.size shouldBe endPageNum + 1
    firstElement.version should equal(foundInventories.map(_.version).max)
    pageTwo.size shouldBe pageSize
    pageToFirstElement.version should equal(pageTwo.map(_.version).max)
    foundInventories.size shouldBe insertedInventories.size
    foundInventories should contain theSameElementsAs insertedInventories
    lastPage.size shouldBe emptyPageSize
  }

  private def insertInventories(
    inventories: Option[Seq[Inventory]],
    ownerRequestId: String,
    count: Int,
    isConfirmed: Boolean
  ) = {
    val insertedInventories =
      inventories.getOrElse(inventoryGen.next(count)).map(_.copy(ownerRequestId = ownerRequestId)).toList

    insertedInventories.map(il => insertInventory(Some(il), isConfirmed = isConfirmed))
  }

  private def insertInventory(inventory: Option[Inventory], isConfirmed: Boolean) = {
    val initial = inventory.getOrElse(inventoryGen.next)
    val il = if (isConfirmed) {
      setConfirmedDates(initial)
    } else {
      initial
    }
    inventoryDao.insert(il).futureValue

    il
  }

  private def setConfirmedDates(initial: Inventory): Inventory = {
    val initialData = initial.data

    initial.copy(
      data = initialData.toBuilder
        .setConfirmedByOwnerDate(DateTimeFormat.defaultInstance)
        .setConfirmedByTenantDate(DateTimeFormat.defaultInstance)
        .build()
    )
  }
}

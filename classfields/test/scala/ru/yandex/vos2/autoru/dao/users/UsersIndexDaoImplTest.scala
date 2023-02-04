package ru.yandex.vos2.autoru.dao.users

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.AutoruModel.AutoruOffer.SearchUserIndex
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.dao.users.UserIndexDao
import ru.yandex.vos2.model.UserRef

/**
  * Created by sievmi on 25.02.19
  */
class UsersIndexDaoImplTest extends AnyFunSuite with InitTestDbs {
  initDbs()

  val userIndexDao: UserIndexDao = components.userIndexDao

  test("CRUD") {
    val userRef = UserRef.from("ac_1")
    //empty table
    assert(userIndexDao.getSearchUserIndex(userRef).isEmpty)

    //insert
    val index = createIndex("123-abc", "VIN", CompositeStatus.CS_ACTIVE, 100)
    assert(userIndexDao.upsertSearchUserIndex(userRef, 0)(_ => index))
    assert(userIndexDao.getSearchUserIndex(userRef).contains(index))

    // update
    assert(userIndexDao.upsertSearchUserIndex(userRef, 0) { index =>
      {
        index.toBuilder.putOffers("456-def", createField("VIN2", CompositeStatus.CS_BANNED, 200)).build()
      }
    })
    assert(!userIndexDao.getSearchUserIndex(userRef).contains(index))
    assert(userIndexDao.getSearchUserIndex(userRef).get.getOffersMap.get("456-def").getVin == "VIN2")
    //delete
  }

  def createIndex(offerId: String, vin: String, status: CompositeStatus, timestampCreate: Long): SearchUserIndex = {
    val builder = SearchUserIndex.newBuilder()
    val fieldBuilder = SearchUserIndex.Field.newBuilder()
    fieldBuilder.setVin(vin)
    fieldBuilder.setTimestampCreate(timestampCreate)
    fieldBuilder.setStatus(status)
    builder.putOffers(offerId, fieldBuilder.build())
    builder.build()
  }

  def createField(vin: String, status: CompositeStatus, timestampCreate: Long): SearchUserIndex.Field = {
    val fieldBuilder = SearchUserIndex.Field.newBuilder()
    fieldBuilder.setVin(vin)
    fieldBuilder.setTimestampCreate(timestampCreate)
    fieldBuilder.setStatus(status)
    fieldBuilder.build()
  }
}

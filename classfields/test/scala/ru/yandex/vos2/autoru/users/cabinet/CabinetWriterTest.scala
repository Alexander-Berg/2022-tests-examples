package ru.yandex.vos2.autoru.users.cabinet

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vos2.UserModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.model.UserRef

/**
  * Created by sievmi on 23.10.18
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CabinetWriterTest extends AnyFunSuite with MockitoSupport with InitTestDbs with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
  }

  implicit private val t: Traced = Traced.empty

  test("update autoru client") {
    val dealer: Dealer = createDealer("new_email@yandex.ru", 79999999999L)

    val vosDealer = createVosDealer("ac_123", Some("old_email@yandex.ru"), None)
    components.userDao.update(vosDealer)

    components.userDao.useIfExist(UserRef.from("ac_123")) { dbUser =>
      assert(dbUser.getUserRef === "ac_123")
      assert(dbUser.getUserContacts.getEmail === "old_email@yandex.ru")
      assert(dbUser.getAutoruDealerStatus === Dealer.Status.NEW)
    }

    val cabinetWriter = CabinetWriter(components.userDao, components.getOfferDao, components.autoruSalonsDao)

    cabinetWriter.processUserUpdate(UserRef.from("ac_123"), dealer)(Threads.SameThreadEc)

    components.userDao.useIfExist(UserRef.from("ac_123")) { dbUser =>
      assert(dbUser.getUserRef === "ac_123")
      assert(dbUser.getUserContacts.getEmail === "new_email@yandex.ru")
      assert(dbUser.getUserContacts.getPhones(0).getNumber === "79999999999")
      assert(dbUser.getAutoruDealerStatus === Dealer.Status.ACTIVE)
    }
  }

  private def createDealer(email: String, phone: Long, status: Dealer.Status = Dealer.Status.ACTIVE): Dealer = {
    val builder = Dealer.newBuilder()
    builder.setEmail(email)
    builder.setPhone(phone)
    builder.setStatus(status)
    builder.build()
  }

  private def createVosDealer(userRef: String, optEmail: Option[String], optPhone: Option[String]) = {
    val builder = UserModel.User.newBuilder()
    builder.setUserRef(userRef)
    optEmail.foreach(builder.getUserContactsBuilder.setEmail)
    optPhone.foreach(phone =>
      builder.getUserContactsBuilder.addPhones(UserModel.UserPhone.newBuilder().setNumber(phone))
    )

    builder.build()
  }

}

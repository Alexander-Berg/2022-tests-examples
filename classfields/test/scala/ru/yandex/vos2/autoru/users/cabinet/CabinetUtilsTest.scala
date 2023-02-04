package ru.yandex.vos2.autoru.users.cabinet

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.cabinet.DealerAutoru
import ru.auto.cabinet.DealerAutoru.Dealer.Status
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.UserModel.{User, UserPhone}
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Created by sievmi on 26.12.18
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CabinetUtilsTest extends AnyFunSuite with MockitoSupport with InitTestDbs {

  test("dealers equals") {
    val user1 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"))
    val user2 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"))

    assert(CabinetUtils.equals(user1, user2))
  }

  test("dealer have different phone") {
    val user1 = createUser("ac_1", "test@yandex.ru", Seq("71234568900"))
    val user2 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"))

    assert(!CabinetUtils.equals(user1, user2))
  }

  test("dealer have different email") {
    val user1 = createUser("ac_1", "test1@yandex.ru", Seq("71234568900", "71112223344"))
    val user2 = createUser("ac_1", "test2@yandex.ru", Seq("71234568900", "71112223344"))

    assert(!CabinetUtils.equals(user1, user2))
  }

  test("dealer have different status") {
    val user1 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"))
    val user2 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"), Status.FREEZED)

    assert(!CabinetUtils.equals(user1, user2))
  }

  test("dealer have different origin") {
    val user1 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"))
    val user2 = createUser("ac_1", "test@yandex.ru", Seq("71234568900", "71112223344"), origin = "spb2")

    assert(!CabinetUtils.equals(user1, user2))
  }

  private def createUser(id: String,
                         email: String,
                         phone: Seq[String],
                         status: Status = DealerAutoru.Dealer.Status.ACTIVE,
                         origin: String = "msk123"): User = {
    val builder = User.newBuilder()
    builder.setUserRef(id)
    builder.getUserContactsBuilder.setEmail(email)
    phone.foreach(p => builder.getUserContactsBuilder.addPhones(UserPhone.newBuilder().setNumber(p)))
    builder.setAutoruDealerStatus(status)
    builder.setOrigin(origin)

    builder.build()
  }

}

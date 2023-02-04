package ru.yandex.vertis.billing.banker.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountDao.{ForAccount, ForUser}
import ru.yandex.vertis.billing.banker.model.Account
import ru.yandex.vertis.billing.banker.model.Account.Properties
import ru.yandex.vertis.billing.banker.util.CollectionUtils.RichTraversableLike

/**
  * Specs for [[AccountDao]]
  *
  * @author alesavin
  */
trait AccountDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  def accounts: AccountDao

  "AccountDao" should {

    val Customer = "1"
    val Acc = "1001"
    val props = Properties(Some("email"), Some("phone"))
    val a = Account(Acc, Customer, props)

    "has no account on start" in {
      accounts.get(ForUser(Customer)).futureValue.isEmpty shouldBe true
      accounts.get(ForAccount(Acc)).futureValue.isEmpty shouldBe true
    }
    "create account with specified id" in {
      (accounts.upsert(a).toTry should be).a(Symbol("Success"))
      val acc = accounts.get(ForAccount(Acc)).await.exactlyOne
      acc.user shouldBe Customer
      acc.id shouldBe Acc
      acc.properties shouldBe props
    }
    "update by upsert" in {
      val newProps = Properties(Some("email1"), Some("phone1"))
      val a2 = a.copy(properties = newProps)
      (accounts.upsert(a2).toTry should be).a(Symbol("Success"))
      val acc = accounts.get(ForAccount(Acc)).futureValue.exactlyOne
      acc.user shouldBe Customer
      acc.id shouldBe Acc
      acc.properties shouldBe newProps
    }
    "update by update" in {
      val patch = Account.Patch(email = props.email, phone = props.phone)
      val updated = accounts.update(Acc, patch).futureValue
      updated.id shouldBe Acc
      updated.user shouldBe Customer
      updated.properties shouldBe props

      accounts.get(ForAccount(Acc)).futureValue.exactlyOne
    }
    "clear email and phone" in {
      val patch = Account.Patch(clearEmail = true, clearPhone = true)
      val updated = accounts.update(Acc, patch).futureValue
      updated.properties shouldBe Account.Properties(email = None, phone = None)
    }
    "fail to update not existent account" in {
      intercept[NoSuchElementException] {
        accounts.update("-", Account.Patch()).await
      }
    }
    "fail if upsert with another user" in {
      intercept[IllegalArgumentException] {
        accounts.upsert(a.copy(user = "2")).await
      }
      val acc = accounts.get(ForAccount(Acc)).await.exactlyOne
      acc.user shouldBe Customer
      acc.id shouldBe Acc
    }
  }

}

package ru.yandex.vertis.billing.banker.ammo

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.ammo.helpers.{
  AccountRoutePhantomRequestBuilders,
  PaymentRoutePhantomRequestBuilder
}
import ru.yandex.vertis.billing.banker.model.Account
import ru.yandex.vertis.billing.banker.model.gens.{
  AccountPatchGen,
  AccountPropertiesGen,
  AccountTransactionIdGen,
  Producer
}
import ru.yandex.vertis.billing.banker.ammo.Constants.AccountPrefix

import scala.jdk.CollectionConverters._

object AmmoGen extends App with AccountRoutePhantomRequestBuilders with PaymentRoutePhantomRequestBuilder {

  override def host: String =
    throw new NotImplementedError()

  override val defaultService =
    throw new NotImplementedError()

  val AccountCount = 10000

  val TotalRequestsCount = 200000

  val OutFile = s"banker_${AccountCount}_accounts_${TotalRequestsCount}_bullets_tolmach_dev.txt"

  private val AccountIds = (1 to AccountCount).map(i => s"$AccountPrefix$i")

  private val Properties = AccountPropertiesGen.next(AccountCount)

  private val Accounts = for {
    (id, properties) <- AccountIds.zip(Properties)
  } yield Account(id, id, properties = properties)

  object RequestTypes extends Enumeration {
    val CreateAccount = Value(0)
    val GetAccounts = Value(1)
    val UpdateAccount = Value(2)
    val Info = Value(3)
    val ConsumeWithdraw = Value(4)
    val DeactivateTransaction = Value(7)
    val GetAllowedPaymentsMethods = Value(8)
    val InitPayment = Value(9)
  }

  val ActionGen: Gen[RequestTypes.Value] =
    Gen.frequency(
      30 -> RequestTypes.GetAccounts,
      5 -> RequestTypes.UpdateAccount,
      30 -> RequestTypes.Info,
      3 -> RequestTypes.ConsumeWithdraw,
      3 -> RequestTypes.DeactivateTransaction,
      15 -> RequestTypes.GetAllowedPaymentsMethods,
      10 -> RequestTypes.InitPayment
    )

  val AccountWithActionGen = {
    for {
      action <- ActionGen
      account <- Gen.oneOf(Accounts)
    } yield (action, account)
  }

  val data = AccountWithActionGen.next(TotalRequestsCount).map { case (action, account) =>
    action match {
      case RequestTypes.GetAccounts =>
        getAccountsRequest(account.user)
      case RequestTypes.CreateAccount =>
        createAccountRequest(account)
      case RequestTypes.UpdateAccount =>
        val properties = AccountPatchGen.next
        updateAccountRequest(account, properties)
      case RequestTypes.Info =>
        getInfoRequest(account)
      case RequestTypes.ConsumeWithdraw =>
        consumeWithdrawRequest(account)
      case RequestTypes.DeactivateTransaction =>
        deactivateTransactionRequest(account, AccountTransactionIdGen.next)
      case RequestTypes.GetAllowedPaymentsMethods =>
        getAllowedPaymentsMethodsRequest(account)
      case RequestTypes.InitPayment =>
        initPaymentRequest(account)
    }
  }

  Files.write(Paths.get(OutFile), data.asJava, StandardCharsets.UTF_8)

}

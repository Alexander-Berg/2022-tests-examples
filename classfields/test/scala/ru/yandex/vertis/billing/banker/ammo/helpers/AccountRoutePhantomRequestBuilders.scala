package ru.yandex.vertis.billing.banker.ammo.helpers

import akka.http.scaladsl.model.ContentTypes
import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.api.v1.view.{AccountPatchView, AccountView, ConsumeAccountTransactionRequestView}
import ru.yandex.vertis.billing.banker.model.Account.Patch
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountTransactionId,
  ConsumeAccountTransactionRequest,
  Payload,
  User
}
import ru.yandex.vertis.billing.banker.ammo.Constants.Protocol
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.WithdrawRequest
import ru.yandex.vertis.billing.banker.model.gens.{withdrawRqGen, Producer, RequestParams}
import ru.yandex.vertis.billing.banker.ammo.Constants.SuperUser

trait AccountRoutePhantomRequestBuilders
  extends BasePhantomRequestBuilder
  with RequestPathProviders
  with HeaderHelpers {

  def defaultBasePath: String

  def createAccountRequest(account: Account): String = {
    val body = AccountView.jsonFormat.write(AccountView(account)).compactPrint
    build(
      s"POST ${accountPath(account.user)} $Protocol",
      headers = customHeaders(
        vertisUser = Some(account.user),
        contentType = Some(ContentTypes.`application/json`)
      ),
      body = Some(body),
      tag = "create_account"
    )
  }

  def getAccountsRequest(customer: User): String = {
    build(
      s"GET ${accountPath(customer)} $Protocol",
      customHeaders(vertisUser = Some(customer)),
      tag = "get_accounts"
    )
  }

  def updateAccountRequest(account: Account, patch: Patch): String = {
    val body = AccountPatchView.jsonFormat.write(AccountPatchView(patch)).compactPrint
    build(
      s"PUT ${accountWithIdPath(account)} $Protocol",
      customHeaders(
        vertisUser = Some(account.user),
        contentType = Some(ContentTypes.`application/json`)
      ),
      Some(body),
      "update_account"
    )
  }

  def getInfoRequest(account: Account): String = {
    build(
      s"GET ${accountWithIdPath(account)}/info $Protocol",
      customHeaders(vertisUser = Some(account.user)),
      tag = "info"
    )
  }

  def deactivateTransactionRequest(account: Account, transactionId: AccountTransactionId): String = {
    build(
      s"DELETE ${accountWithIdPath(account)}/transaction/${transactionId.`type`}/${transactionId.id} $Protocol",
      customHeaders(vertisUser = Some(SuperUser)),
      tag = "deactivate_transaction"
    )
  }

  private def asHttpGen(account: Account, gen: Gen[ConsumeAccountTransactionRequest]): Gen[String] = {
    gen.map { request =>
      val body = ConsumeAccountTransactionRequestView.jsonFormat
        .write(ConsumeAccountTransactionRequestView(request))
        .compactPrint
      build(
        s"PUT ${accountWithIdPath(account)}/consume $Protocol",
        customHeaders(
          vertisUser = Some(account.user),
          contentType = Some(ContentTypes.`application/json`)
        ),
        Some(body),
        request.id.`type`.toString.toLowerCase()
      )
    }
  }

  def consumeWithdrawRequest(account: Account, transactionId: Option[AccountTransactionId] = None): String = {
    val params = RequestParams(id = transactionId)
      .withPaymentPayload(Payload.Empty)
      .withWithdrawOpts(WithdrawRequest.Options())
      .withAccount(account.id)
    asHttpGen(account, withdrawRqGen(params)).next
  }
}

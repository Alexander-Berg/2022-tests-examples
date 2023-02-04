package ru.yandex.vertis.billing.banker.client

import ru.yandex.vertis.billing.banker.client.ClientPretender.Action
import ru.yandex.vertis.billing.banker.model.{AccountId, PaymentRequest, User}
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.util.Try

/**
  * Pretend client with actions
  *
  * @author alesavin
  */
trait ClientPretender {

  def customer: User
  def account: AccountId

  def next: Action
  def step(implicit rc: RequestContext): Try[Unit]
}

object ClientPretender {

  sealed trait Action
  object Empty extends Action
  object Audit extends Action
  case class CreateAccount(id: AccountId) extends Action
  case class PaymentAction(customer: User, source: PaymentRequest.Source) extends Action
}

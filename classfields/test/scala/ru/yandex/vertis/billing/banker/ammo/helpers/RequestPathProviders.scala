package ru.yandex.vertis.billing.banker.ammo.helpers

import ru.yandex.vertis.billing.banker.model.{Account, User}

trait RequestPathProviders {

  def defaultService: String

  def defaultBasePath: String = s"/api/1.x/service/$defaultService"

  private def customerPath(user: User): String =
    s"$defaultBasePath/customer/$user"

  def accountPath(user: User): String =
    s"${customerPath(user)}/account"

  def accountWithIdPath(account: Account): String =
    s"${customerPath(account.user)}/account/${account.id}"

  def paymentPath(user: User): String = {
    s"${customerPath(user)}/payment"
  }

}

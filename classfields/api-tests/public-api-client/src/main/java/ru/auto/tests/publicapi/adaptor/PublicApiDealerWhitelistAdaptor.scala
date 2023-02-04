package ru.auto.tests.publicapi.adaptor

import com.google.inject.{Inject, Singleton}
import io.qameta.allure.Step
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.api.DealerApi
import ru.auto.tests.publicapi.model.{AutoApiDealerSimplePhonesList, VertisPassportSession}
import ru.auto.tests.publicapi.operations.dealer.whitelist.WhitelistOps
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.jdk.CollectionConverters._

@Singleton
class PublicApiDealerWhitelistAdaptor extends PublicApiAdaptor with WhitelistOps {

  @Inject
  @Prod
  override val api: ApiClient = null

  def cleanup(account: Account)(
      implicit session: VertisPassportSession = login(account).getSession
  ): Unit =
    Option(
      listPhones(account)
        .executeAs(identity)
        .getPhones
    ).foreach { existingPhones =>
      removePhones(account, existingPhones.asScala.toSeq)
        .executeAs(identity)
    }


}

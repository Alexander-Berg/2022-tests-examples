package ru.yandex.vos2.services.phone

import ru.yandex.extdata.core.Controller
import ru.yandex.extdata.core.event.Event
import ru.yandex.extdata.core.lego.{Provider, SimpleProvider}
import ru.yandex.realty.context.v2.bunker.{BunkerProviderUpdated, BunkerStorage}
import ru.yandex.realty.loaders.AbstractProvider

import scala.util.Try

trait TestingSmsPhoneWhitelist {
  def isWhitelisted(normalizedPhone: String): Boolean
}

class TestingSmsPhoneWhitelistStorage(phones: Set[String]) extends TestingSmsPhoneWhitelist {
  override def isWhitelisted(normalizedPhone: String): Boolean = phones.contains(normalizedPhone)
}

class TestingSmsPhoneWhitelistProvider(val controller: Controller, bunkerProvider: Provider[BunkerStorage])
  extends SimpleProvider[TestingSmsPhoneWhitelist]
  with AbstractProvider {

  override def build(): Try[TestingSmsPhoneWhitelist] = Try {
    new TestingSmsPhoneWhitelistStorage(
      bunkerProvider
        .get()
        .get[Seq[String]]("/realty/testing-sms-phone-whitelist")
        .getOrElse(Seq.empty)
        .toSet
    )
  }

  override def updatedEvent: Event = TestingSmsPhoneWhitelistUpdated

  override def eventDependencies: List[Event] = List(BunkerProviderUpdated)
}

object TestingSmsPhoneWhitelistUpdated extends Event

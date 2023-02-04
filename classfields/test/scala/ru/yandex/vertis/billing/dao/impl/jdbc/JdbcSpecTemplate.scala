package ru.yandex.vertis.billing.dao.impl.jdbc

import org.scalatest.Suite
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplateBase.{NamedDatabase, Scheme}
import slick.jdbc

trait JdbcSpecTemplate extends JdbcSpecTemplateBase {
  this: Suite =>

  lazy val fullBillingDatabase: NamedDatabase =
    createNamed(Scheme("/sql/vs_billing.final.full.sql", "vs_billing_test_main"))

  lazy val billingDatabase: jdbc.JdbcBackend.Database =
    create(Scheme("/sql/vs_billing.final.sql", "vs_billing_unit_test_main"))

  lazy val billingDualDatabase = DualDatabase(billingDatabase, billingDatabase)

  lazy val holdDatabase: jdbc.JdbcBackend.Database =
    create(Scheme("/sql/vs_billing_hold.final.sql", "vs_billing_unit_test_hold"))

  lazy val namedCampaignEventDatabase: NamedDatabase =
    createNamed(Scheme("/sql/vs_billing_campaign_event.final.sql", "vs_billing_unit_test_campaign_event"))

  lazy val campaignEventDatabase: jdbc.JdbcBackend.Database =
    namedCampaignEventDatabase.database

  lazy val campaignEventDualDatabase = DualDatabase(campaignEventDatabase, campaignEventDatabase)

  lazy val eventStorageDatabase: jdbc.JdbcBackend.Database =
    create(Scheme("/sql/vs_billing_events_storage.final.sql", "vs_billing_unit_test_events"))

  lazy val eventStorageDualDatabase = DualDatabase(eventStorageDatabase, eventStorageDatabase)

  lazy val archiveDatabase: jdbc.JdbcBackend.Database =
    create(Scheme("/sql/vs_billing_archive.final.sql", "vs_billing_unit_test_archive"))

  lazy val archiveDualDatabase = DualDatabase(archiveDatabase, archiveDatabase)

  lazy val namedAutoruBalanceDatabase: NamedDatabase =
    createNamed(Scheme("/sql/autoru_balance_test.sql", "autoru_balance_test"))

  lazy val autoruBalanceDatabase: jdbc.JdbcBackend.Database =
    namedAutoruBalanceDatabase.database
}

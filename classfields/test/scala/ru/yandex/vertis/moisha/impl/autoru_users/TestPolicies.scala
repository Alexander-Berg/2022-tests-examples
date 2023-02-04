package ru.yandex.vertis.moisha.impl.autoru_users

import com.codahale.metrics.MetricRegistry
import com.yandex.yoctodb.immutable.IndexedDatabase
import org.apache.commons.io.{FileUtils, IOUtils}
import ru.yandex.vertis.moisha.impl.autoru.RootAutoRuPolicy
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies.TestIndexProvider
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.RootAutoRuUsersSubscriptionsPolicy
import ru.yandex.vertis.moisha.index.impl.autoru_dealers.AutoRuDealersYoctoIndexer
import ru.yandex.vertis.moisha.index.impl.autoru_users.AutoRuUsersYoctoIndexer
import ru.yandex.vertis.moisha.index.{IndexProvider, YoctoIndexer, YoctoSearcher}

import java.io.{File, FileOutputStream, OutputStream}
import java.net.URL

trait TestPolicies {

  private val userIndexProvider =
    new TestIndexProvider("/user-prices.xml", AutoRuUsersYoctoIndexer.fromResource)

  private val dealerIndexProvider =
    new TestIndexProvider("/dealer-prices.xml", AutoRuDealersYoctoIndexer.fromResource)

  private val dealerTariffIndexProvider =
    new TestIndexProvider("/dealer-prices-tariff.xml", AutoRuDealersYoctoIndexer.fromResource)

  val usersPolicy = new RootAutoRuUsersPolicy(userIndexProvider)
  val usersSubscriptionsPolicy = new RootAutoRuUsersSubscriptionsPolicy(userIndexProvider)

  val dealersPolicy = new RootAutoRuPolicy(dealerIndexProvider, new MetricRegistry)
  val dealersTariffPolicy = new RootAutoRuPolicy(dealerTariffIndexProvider, new MetricRegistry)
}

object TestPolicies {

  private val tmpIndexFilename = "tmpIndex"

  class TestIndexProvider(resourcePath: String, createIndexer: URL => YoctoIndexer) extends IndexProvider {

    override val getIndex: IndexedDatabase = {
      var fos: OutputStream = null
      try {
        val indexer = createIndexer(getClass.getResource(resourcePath))
        val indexFile = new File(tmpIndexFilename)
        fos = new FileOutputStream(indexFile)
        for {
          index <- indexer.buildIndex()
          _ = fos.write(index.toByteArray)
          indexedDatabase <- YoctoSearcher.from(indexFile)
        } yield indexedDatabase
      } finally {
        IOUtils.closeQuietly(fos)
        FileUtils.deleteQuietly(new File(tmpIndexFilename))
      }
    }.get
  }
}

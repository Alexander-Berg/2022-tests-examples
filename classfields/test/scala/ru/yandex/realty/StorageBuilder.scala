package ru.yandex.realty

import ru.yandex.common.util.IOUtils
import ru.yandex.common.util.currency.Currency
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.domain.currency.{BaseCurrency, CurrencyExchangeRate}
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.storage.CurrencyStorage

import scala.collection.JavaConverters._

/**
  * @author nstaroverova
  */
object StorageBuilder {

  def buildCurrencyStorage(): CurrencyStorage = {
    val exchangeRates = List(
      new CurrencyExchangeRate(Currency.USD, Currency.RUR, 31, 1),
      new CurrencyExchangeRate(Currency.EUR, Currency.RUR, 40, 1)
    )

    new CurrencyStorage(List.empty[BaseCurrency].asJava, exchangeRates.asJava, buildRegionGraphProvider())
  }

  def buildRegionGraphProvider(): Provider[RegionGraph] = {
    val regionGraph =
      RegionGraphProtoConverter.deserialize(
        IOUtils.gunzip(
          getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
        )
      )
    ProviderAdapter.create(regionGraph)
  }

}

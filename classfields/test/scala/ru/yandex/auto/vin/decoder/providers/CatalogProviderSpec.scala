package ru.yandex.auto.vin.decoder.providers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.Utils
import ru.yandex.auto.vin.decoder.extdata.DecoderDataType
import ru.yandex.auto.vin.decoder.model.catalog.CatalogCard
import ru.yandex.auto.vin.decoder.providers.catalog.CatalogProvider

/**
  * Created by artvl on 13.07.16.
  */
class CatalogProviderSpec extends AnyFlatSpec with MockitoSugar with Matchers with Utils {

  val provider = new CatalogProvider(prepareController("CATALOG.data", DecoderDataType.Catalog))

  "A catalog provider" should "parse proto catalog" in {
    val catalog = provider.build()

    catalog.isFailure should not be true

    catalog.get.cars.isEmpty should not be true

    catalog.get.cars.getOrElse("BMW", Map.empty[String, CatalogCard]).isEmpty should not be true
  }
}

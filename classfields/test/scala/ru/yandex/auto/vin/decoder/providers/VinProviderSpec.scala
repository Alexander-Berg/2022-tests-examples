package ru.yandex.auto.vin.decoder.providers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.Utils
import ru.yandex.auto.vin.decoder.extdata.DecoderDataType
import ru.yandex.auto.vin.decoder.providers.vin.VinProvider

/**
  * Created by artvl on 13.07.16.
  */
class VinProviderSpec extends AnyFlatSpec with MockitoSugar with Matchers with Utils {

  val provider = new VinProvider(prepareController("VIN.data", DecoderDataType.Vin))

  "A catalog provider" should "parse proto catalog" in {
    val vin = provider.build()

    vin.isFailure should not be true

    vin.get.markVDS.isEmpty should not be true
    vin.get.vinLogic.isEmpty should not be true
    vin.get.wmi.isEmpty should not be true
  }
}

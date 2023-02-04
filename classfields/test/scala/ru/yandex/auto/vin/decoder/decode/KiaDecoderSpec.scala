package ru.yandex.auto.vin.decoder.decode

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.Utils
import ru.yandex.auto.vin.decoder.decode.algorithms.{HyundaiAlgorithm, KiaAlgorithm}
import ru.yandex.auto.vin.decoder.extdata.DecoderDataType
import ru.yandex.auto.vin.decoder.providers.catalog.{CatalogProvider, ClientCatalogProvider}
import ru.yandex.auto.vin.decoder.providers.vin.{ClientVinProvider, VinProvider}

/**
  * Created by artvl on 13.07.16.
  */

class KiaDecoderSpec extends AnyFlatSpec with MockitoSugar with Matchers with Utils with BeforeAndAfter {

  implicit val t: Traced = Traced.empty

  val KiaList = List(
    "X4XGD224350001404",
    "XWEPC811DD0033616",
    "XWEKU814DEC003118",
    "XWEGN412BE0002200",
    "KNAFH221395090529",
    "XWEHC512BC0015902",
    "KNEJE55256K266448",
    "XWEMB752380000165",
    "U6YJE55258L033757",
    "KNAKU811DC5185786",
    "KNAGN412BC5236263",
    "XWEFF241280004351",
    "XWEPC81ABE0003665",
    "KNEJE55256K266636",
    "Z94CC41BACR066290",
    "XWEHH811BC0000657",
    "Z94CB41BBHR372689"
  )

  val kiaCerato2009 = "KNAFH221395090529"
  val kiaSportage2008 = "U6YJE55258L033757"

  before(prepare())

  "Decoder" should "decode " + kiaCerato2009 + " as Kia Cerato 2009" in {
    KiaAlgorithm
      .decode("KNAFH221395090529")
      .filter(result => result.mark.equals("KIA") & result.model.equals("CERATO") & result.year.contains(2009))

  }

  "Decoder" should "decode " + kiaSportage2008 + " as Kia Sportage 2008" in {
    KiaAlgorithm
      .decode("KNAFH221395090529")
      .filter(result => result.mark.equals("KIA") & result.model.equals("SPORTAGE") & result.year.contains(2008))

  }

  private def prepare(): Unit = {
    val vinP = new VinProvider(prepareController("VIN.data", DecoderDataType.Vin))
    val vinCatalog = vinP.build().get
    val vinProvider = mock[ClientVinProvider]
    when(vinProvider.get()).thenReturn(vinCatalog)

    val catalogP = new CatalogProvider(prepareController("CATALOG.data", DecoderDataType.Catalog))
    val catalog = catalogP.build().get
    val catalogProvider = mock[ClientCatalogProvider]
    when(catalogProvider.get()).thenReturn(catalog)

    HyundaiAlgorithm.vinProvider = Some(vinProvider)
    KiaAlgorithm.vinProvider = Some(vinProvider)
    HyundaiAlgorithm.catalogProvider = Some(catalogProvider)
    KiaAlgorithm.catalogProvider = Some(catalogProvider)
  }

}

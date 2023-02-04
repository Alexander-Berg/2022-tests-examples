package ru.yandex.vos2.autoru.dao.old.proxy

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.dao.old.{AutoruMotoDao, AutoruSalesDao, AutoruTrucksDao}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class OldDbWriterTest extends AnyFunSuite with MockitoSupport {
  private val autoruSalesDao = mock[AutoruSalesDao]
  private val autoruTrucksDao = mock[AutoruTrucksDao]
  private val autoruMotoDao = mock[AutoruMotoDao]
  private val oldDbWriter = new OldDbWriter(autoruSalesDao, autoruTrucksDao, autoruMotoDao)

  doNothing().when(autoruTrucksDao).removePhones(?, ?)(?)
  doNothing().when(autoruMotoDao).removePhones(?, ?)(?)

  test("removePhones: cars") {
    oldDbWriter.removePhones(Category.CARS, 100500, Seq("79291112233"))(Traced.empty)
  }

  test("removePhones: trucks") {
    val saleId = 100500
    val unknownPhones = Seq("79291112233")
    oldDbWriter.removePhones(Category.TRUCKS, saleId, unknownPhones)(Traced.empty)
    verify(autoruTrucksDao).removePhones(eq(saleId), eq(unknownPhones))(any())
  }

  test("removePhones: moto") {
    val saleId = 100500
    val unknownPhones = Seq("79291112233")
    oldDbWriter.removePhones(Category.MOTO, saleId, unknownPhones)(Traced.empty)
    verify(autoruMotoDao).removePhones(eq(saleId), eq(unknownPhones))(any())
  }
}

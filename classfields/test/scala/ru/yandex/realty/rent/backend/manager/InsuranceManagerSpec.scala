package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.dao.RentSpecBase
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.proto.api.insurance.InsurancePayload
import ru.yandex.realty.rent.proto.model.insurance.InternalRentInsurance.Request
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class InsuranceManagerSpec extends WordSpec with RentSpecBase with MockFactory with RentModelsGen {

  implicit val trace: Traced = Traced.empty

  "InsuranceManager.getRentInsurance" should {
    "return expected values" in new Wiring {

      forAllShouldBe(from = 0, to = 3000000)(expectedPrice = 51100)
      forAllShouldBe(from = 3000001, to = 6000000)(expectedPrice = 81100)
      forAllShouldBe(from = 6000001, to = 9000000)(expectedPrice = 111100)
      forAllShouldBe(from = 9000001, to = 12000000)(expectedPrice = 141100)
      forAllShouldBe(from = 12000001, to = 15000000)(expectedPrice = 171100)
      forAllShouldBe(from = 15000001, to = 18000000)(expectedPrice = 201100)
      forAllShouldBe(from = 18000001, to = 21000000)(expectedPrice = 231100)
      forAllShouldBe(from = 21000001, to = 24000000)(expectedPrice = 261100)
      forAllShouldBe(from = 24000001, to = 27000000)(expectedPrice = 291100)

    }
  }

  trait Wiring {
    val manager: InsuranceManager = new InsuranceManager()
    val N = 30

    def seq(from: Int, to: Int, size: Int): Iterable[Int] =
      Gen.choose(from, to).next(size) ++ Seq(from, to)

    def forAllShouldBe(from: Int, to: Int)(expectedPrice: Long): Unit = {
      val expected = InsurancePayload.newBuilder.setInsuranceAmount(expectedPrice).build
      seq(from, to, N) foreach { price =>
        val result = manager.getRentInsurance(Request.newBuilder.setRentPriceInKopecks(price).build).futureValue

        result shouldEqual expected
      }
    }
  }
}

package ru.yandex.realty.traffic.logic

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.RequestParameter
import ru.yandex.realty.canonical.base.params.RequestParameter.RoomsValue
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.offercategory.{CommercialCategory, HouseCategory}
import zio.random.Random
import zio.test._
import zio.test.junit._

@RunWith(classOf[ZTestJUnitRunner])
class UnifiedOfferParamCheckerLogicSpec extends JUnitRunnableSpec {

  private def houseWithRoomsGen: Gen[Random, UnifiedOffer] =
    for {
      cnt <- Gen.fromIterable(-2 to 10)
    } yield UnifiedOffer
      .newBuilder()
      .setHouse(HouseCategory.newBuilder().setRoomsTotal(cnt))
      .build()

  private def commercialWithRoomsGen: Gen[Random, UnifiedOffer] =
    for {
      cnt <- Gen.fromIterable(-2 to 10)
    } yield UnifiedOffer
      .newBuilder()
      .setCommercial(CommercialCategory.newBuilder().setRoomsTotal(cnt))
      .build()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UnifiedOfferParamCheckerLogic")(
      testM("should not extract rooms from house and commercial offers") {

        val gen = for {
          rooms <- Gen.fromIterable(RoomsValue.values)
          offer <- Gen.concatAll(Iterable(commercialWithRoomsGen, houseWithRoomsGen))
        } yield RequestParameter.RoomsTotal(rooms) -> offer

        checkAll(gen) {
          case (param, offer) =>
            val check = UnifiedOfferParamCheckerLogic.checkParam(offer, param)(NoopUnsupportedParamHandler)
            assertTrue(!check)
        }
      }
    )
}

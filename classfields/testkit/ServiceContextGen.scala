package auto.carfax.carfax_money.logic.testkit

import auto.carfax.money.money_model.ServiceContext.PackageInfo
import auto.carfax.money.money_model.ServiceContext
import zio.random.Random
import zio.test.{Gen, Sized}

object ServiceContextGen {

  def serviceContextGen(count: Set[Int]): Gen[Random with Sized, ServiceContext] = for {
    service <- Gen.oneOf(
      Gen.const(ServiceContext.ServiceType.VIN_REPORT_SERVICE),
      Gen.const(ServiceContext.ServiceType.OFFER_REPORT_SERVICE)
    )
    reportCount <- Gen.fromIterable(count)
  } yield ServiceContext.of(service, Some(PackageInfo.of(reportCount)))

}

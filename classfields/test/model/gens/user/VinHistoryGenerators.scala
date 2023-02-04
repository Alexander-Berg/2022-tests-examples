package ru.auto.salesman.test.model.gens.user

import ru.auto.salesman.test.model.gens.AutoruOfferIdGen
import org.scalacheck.Gen
import ru.auto.salesman.model.OfferVin
import ru.auto.salesman.model.user.ApiModel.VinHistoryBoughtReport
import ru.auto.salesman.test.model.gens.BasicSalesmanGenerators

trait VinHistoryGenerators extends BasicSalesmanGenerators {

  def vinHistoryBoughtReportGenerator: Gen[VinHistoryBoughtReport] =
    for {
      vin <- Gen.alphaNumStr
      offerId <- AutoruOfferIdGen
      createdAt <- timestampGen
      deadline <- timestampInFutureGen
    } yield
      VinHistoryBoughtReport.newBuilder
        .setVin(vin)
        .setOfferId(offerId.toString)
        .setCreatedAt(createdAt)
        .setDeadline(deadline)
        .build

  def vinGenerator: Gen[OfferVin] = readableString(1, 10)

}

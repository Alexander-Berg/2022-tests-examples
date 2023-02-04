package ru.auto.comeback.model.testkit

import ru.auto.api.comeback_model.ComebackListingRequest
import zio.random.Random
import zio.test.Gen._
import zio.test.{Gen, Sized}

object ComebackListingRequestFilterGen {

  def anyGeoFilter[R <: Random with Sized](): Gen[R, ComebackListingRequest.Filter] =
    for {
      geoRadius <- option(anyInt)
      rids <- oneOf(listOf1(anyInt), listOfBounded(1, 5)(anyInt))
    } yield ComebackListingRequest.Filter(
      rid = rids,
      geoRadius = geoRadius
    )

}

package ru.auto.api.services

import ru.auto.api.ApiSuiteBase
import ru.auto.api.managers.offers.OffersManager
import org.scalatest.Suite

/**
  * Created by andrey on 9/26/17.
  */
trait MockedOffersManager extends ApiSuiteBase { suite: Suite =>
  override lazy val offersManager = mock[OffersManager]
}

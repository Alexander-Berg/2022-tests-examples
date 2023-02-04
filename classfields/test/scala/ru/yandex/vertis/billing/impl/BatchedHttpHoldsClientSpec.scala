package ru.yandex.vertis.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.billing.{BatchedHoldsClient, HoldsClientSpec}

/**
  * Tests for [[HttpHoldsClient]]
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class BatchedHttpHoldsClientSpec extends HoldsClientSpec {

  val client = new HttpHoldsClient(InternalApiBaseUrl, "realty_commercial") with BatchedHoldsClient {
    override val batchSize = 10
  }

}

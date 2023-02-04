package ru.yandex.auto.extdata.jobs

import org.scalatest.{Matchers, WordSpec}

class DealersFetcherUnitSpec extends WordSpec with Matchers {
  import YaMapsDealersProducer._

  "test cleanTag function" in {
    cleanTag("category=CARS#section=NEW#offer_id=1097305446-9e5ba97b") shouldBe "category=CARS#section=NEW"
    cleanTag("category=CARS#section=NEW#offer_id=1096437688-22148ba1#offer_id=1097305446-9e5ba97b") shouldBe "category=CARS#section=NEW"
    cleanTag("category=CARS#section=NEW") shouldBe "category=CARS#section=NEW"
    cleanTag("category=CARS") shouldBe "category=CARS"
    cleanTag("") shouldBe ""
    cleanTag("offer_id=1096437688-22148ba1#category=CARS#section=NEW#offer_id=1097305446-9e5ba97b#category=USED") shouldBe "category=CARS#section=NEW#category=USED"
    cleanTag("offer_id=1096437688-22148ba1#category=CARS#category=USED") shouldBe "category=CARS#category=USED"
  }

}

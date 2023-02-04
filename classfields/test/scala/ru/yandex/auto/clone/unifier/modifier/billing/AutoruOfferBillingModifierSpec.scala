package ru.yandex.auto.clone.unifier.modifier.billing

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.clone.unifier.modifier.AutoruOfferBillingModifier
import ru.yandex.auto.core.model.{AutoruBilling, UnifiedCarInfo}

import scala.collection.JavaConverters._

/**
  * Created by goodfella on 30.01.17.
  */
@RunWith(classOf[JUnitRunner])
class AutoruOfferBillingModifierSpec extends WordSpecLike with Matchers {
  val _info = new UnifiedCarInfo("1")
  val modifier = new AutoruOfferBillingModifier
  _info.setAutoruBilling(new AutoruBilling(List("s").asJava, null))
  _info.setLastFreshDate(2L)
  _info.setTrueCreationDate(3L)
  _info.setCreationDate(3L)
  _info.setUpdateDate(4L)

  "extractFreshDate" should {
    "return 3" in {
      val freshDate = modifier.extractFreshDate(_info)
      freshDate shouldBe 3L
    }
  }

}

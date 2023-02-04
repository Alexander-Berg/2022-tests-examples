package ru.yandex.vertis.moisha.impl.autoru.v6

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy

/**
  * Specs on [[AutoRuPolicyV6]]
  */
@RunWith(classOf[JUnitRunner])
class AutoRuPolicyV6Spec
  extends AutoRuPlacementSpec
  with AutoRuPremiumSpec
  with AutoRuSpecialOfferSpec
  with AutoRuBoostSpec
  with AutoRuBadgeSpec {

  val policy: AutoRuPolicy = new AutoRuPolicyV6

}

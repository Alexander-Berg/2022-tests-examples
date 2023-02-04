package ru.yandex.vertis.telepony.model.mtt

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.Phone

class SipIdSpec extends SpecBase {

  private val transformations = Map(
    "883140586495911" -> Phone("+79586495911"), // DEF 7958
    "883140587074583" -> Phone("+79587074583"), // DEF 7958
    "79587045911" -> Phone("+79587045911"), // DEF 7958704
    "79606495911" -> Phone("+79606495911"), // DEF 7960
    "73412325780" -> Phone("+73412325780") // ABC
  )

  "SipId" should {
    transformations.foreach {
      case (mttAccountId, phone) =>
        s"transform mtt account id '$mttAccountId' to phone '${phone.value}'" in {
          SipId(mttAccountId).toPhone should ===(phone)
        }
        s"transform phone '${phone.value} to mtt account id '$mttAccountId'" in {
          SipId(phone).value should ===(mttAccountId)
        }
    }
  }

}

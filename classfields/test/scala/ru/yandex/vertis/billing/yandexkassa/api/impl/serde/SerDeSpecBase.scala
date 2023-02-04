package ru.yandex.vertis.billing.yandexkassa.api.impl.serde

import org.joda.time.DateTime
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiSpecBase
import ru.yandex.vertis.billing.yandexkassa.api.model.{AmountValue, Currencies, OrderInfo, Recipient}

/**
  * Base stuff for SerDe testing
  *
  * @author alex-kovalenko
  */
trait SerDeSpecBase extends YandexKassaApiSpecBase {

  val orderId = "1ddd77af-0bd7-500d-895b-c475c55fdefc"
  val clientOrderId = "1235/2015/abcde"
  val order = OrderInfo(clientOrderId, "customer", AmountValue(100, Currencies.RUB), Map("key" -> "value"))
  val createdDt = DateTime.parse("2015-11-11T17:05:07.5+03:00")
  val authorizedDt = DateTime.parse("2015-11-11T18:29:55.3+03:00")
  val recipient = Recipient(settings.shopId, settings.shopArticleId)
  val charge = AmountValue(100, Currencies.RUB)
  val income = AmountValue(98.5, Currencies.RUB)

}

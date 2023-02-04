package ru.yandex.vertis.billing

import ru.yandex.vertis.billing.receipt.model.{
  AdditionalUserRequisite,
  AgentTypes,
  Payment,
  PaymentTypeTypes,
  PaymentTypes,
  ReceiptContent,
  ReceiptTypes,
  Row,
  SupplierInfo,
  TaxTypes,
  TaxationTypes
}

package object receipt {

  val row = Row(
    PaymentTypeTypes.Prepayment,
    100,
    1,
    TaxTypes.Nds18,
    "Test",
    AgentTypes.Agent,
    Some(SupplierInfo("122323", Some("+79992221222"), Some("test")))
  )

  val content = ReceiptContent(
    "7736207543",
    AgentTypes.NoneAgent,
    "test_Whitespirit@yandex.ru",
    Iterable(Payment(100, PaymentTypes.Card)),
    ReceiptTypes.Income,
    Iterable(row),
    Some("sdds"),
    TaxationTypes.OSN,
    "test@emai.ru",
    "firm_url.ru",
    AdditionalUserRequisite("balance_payment_id", "666606666")
  )

}

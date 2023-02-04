package ru.auto.salesman.test.data

object MoishaTestDataGen {

  def testJsonPriceFor(
      product: String,
      price: Long,
      from: String,
      to: String,
      duration: Int
  ): Array[Byte] =
    s"""
    |{
    |  "request": "does not matter for tests",
    |  "points": [
    |    {
    |      "policy": "autoru_2016-07-15",
    |      "interval": {
    |        "from": "$from",
    |        "to": "$to"
    |      },
    |      "product": {
    |        "product": "$product",
    |        "goods": [
    |          {
    |            "good": "Custom",
    |            "cost": "PerIndexing",
    |            "price": $price
    |          }
    |        ],
    |        "total": $price,
    |        "duration": $duration
    |      }
    |    }
    |  ]
    |}
     """.stripMargin.getBytes
}

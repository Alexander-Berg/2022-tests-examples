package ru.auto.api.model.bunker.c2b.auction

import org.scalatest.funsuite.AnyFunSuite

import java.io.ByteArrayInputStream

class AuctionStatusesTest extends AnyFunSuite {

  test("parse C2BAuctionStatuses from test json") {
    val in = new ByteArrayInputStream(
      """
        |[
        |  {
        |    "fullName": "/auto_ru/c2b_auction/statuses",
        |    "content": [
        |      {
        |        "code": 1,
        |        "title": "Осмотр",
        |        "description": "Подтверждение осмотра с менеджером Авто.ру и встреча с экспертом"
        |      },
        |      {
        |        "code": 2,
        |        "title": "Аукцион",
        |        "description": "Участники знакомятся с результатами осмотра и делают ставки"
        |      },
        |      {
        |        "code": 3,
        |        "title": "Сделка",
        |        "description": "Получение лучшего предложения и подписание договора в автосалоне"
        |      }
        |    ]
        |  }
        |]
        |""".stripMargin.getBytes("UTF-8")
    )
    val statuses = C2BAuctionStatuses.parse(in)
    val inspection = Status(1, "Осмотр", "Подтверждение осмотра с менеджером Авто.ру и встреча с экспертом")
    val auction = Status(2, "Аукцион", "Участники знакомятся с результатами осмотра и делают ставки")
    val deal = Status(3, "Сделка", "Получение лучшего предложения и подписание договора в автосалоне")
    assert(statuses == List(inspection, auction, deal))
  }
}

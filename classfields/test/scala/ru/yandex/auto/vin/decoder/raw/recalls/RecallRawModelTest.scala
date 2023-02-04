package ru.yandex.auto.vin.decoder.raw.recalls

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class RecallRawModelTest extends AnyFunSuite {

  val manager = new RecallRawModelManager(new RawRecallToPreparedConverter)

  test("parse raw") {
    val raw =
      "{\"vin\":\"WDB4632361X221554\",\"raw_data\":\"{\\\"recallId\\\":\\\"2\\\",\\\"campaignId\\\":\\\"19\\\",\\\"title\\\":\\\"Проблема затяжки муфты рулевого вала\\\",\\\"description\\\":\\\"Причиной отзыва автомобилей Mercedes-Benz является возможный недостаточный момент затяжки болтового соединения соединительной муфты рулевого вала. Данный дефект может вызывать больший износ зубцов карданного шарнира при повышенной нагрузке.\\\",\\\"url\\\":\\\"http://old.gost.ru/wps/portal/pages/news/?article_id\\\\u003d6804\\\",\\\"published\\\":\\\"2017-07-28T21:00:00Z\\\"}\"}"

    val result = manager.parse(raw, "", "").toOption.get
    assert(result.identifier.toString == "WDB4632361X221554")
    assert(result.groupId == "2")
    assert(result.recall.getRecallId == 2)
    assert(result.recall.getCampaignId == 19)
    assert(result.recall.getTitle == "Проблема затяжки муфты рулевого вала")
    assert(
      result.recall.getDescription == "Причиной отзыва автомобилей Mercedes-Benz является возможный недостаточный момент затяжки болтового соединения соединительной муфты рулевого вала. Данный дефект может вызывать больший износ зубцов карданного шарнира при повышенной нагрузке."
    )
    assert(result.recall.getUrl == "http://old.gost.ru/wps/portal/pages/news/?article_id=6804")
    assert(result.recall.getPublished.getSeconds == 1501275600)

  }
}

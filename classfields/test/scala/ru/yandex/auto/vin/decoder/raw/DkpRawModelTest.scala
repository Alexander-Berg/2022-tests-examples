package ru.yandex.auto.vin.decoder.raw

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.dkp.DkpRawModelManager

class DkpRawModelTest extends AnyFunSuite {

  val manager = new DkpRawModelManager(null)

  test("parse raw") {
    val raw =
      "{\"date\":\"2018-06-28T00:00:00Z\",\"ip\":\"91.185.51.171\",\"pts\":null,\"price\":20000,\"action\":\"print\",\"sts\":\"3845418942\",\"gosnomer\":\"9880АЕ38\",\"mark\":\"Кавасаки EX300B\",\"year\":2013,\"vin\":\"JKAEX300ABDA08536\",\"place\":\"г.Иркустк\",\"ts\":\"{\\\"color\\\":\\\"\\\\u0417\\\\u0435\\\\u043b\\\\u0435\\\\u043d\\\\u044b\\\\u0439\\\",\\\"category\\\":\\\"\\\\u041c\\\\u043e\\\\u0442\\\\u043e\\\\u0446\\\\u0438\\\\u043a\\\\u043b\\\",\\\"chassis\\\":\\\"JKAEX300ABDA08536\\\",\\\"body\\\":\\\"JKAEX300ABDA08536\\\",\\\"run\\\":2000,\\\"engine\\\":{\\\"model\\\":\\\"\\\",\\\"number\\\":\\\"\\\",\\\"power\\\":\\\"39.40\\\",\\\"volume\\\":\\\"300\\\"}}\",\"create_date\":\"2018-06-28T10:38:17Z\",\"user_id\":null,\"phone\":null,\"id\":5095715}"

    val result = manager.parse(raw, "", "").toOption.get
    assert(result.identifier.toString == "JKAEX300ABDA08536")
    assert(result.groupId == "5095715")
    assert(result.data.place.contains("г.Иркустк"))
    assert(result.data.tsInfo.mileage == 2000)

  }

}

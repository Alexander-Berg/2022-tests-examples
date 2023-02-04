package ru.auto.api.util

import java.time.Instant

import ru.auto.api.BaseSpec

class TskvLogWriterTest extends BaseSpec {

  "TskvLogWriter" should {
    "format some timestamps" in {
      TskvLogWriter.timeStamp(Instant.ofEpochMilli(1513733587000L)) shouldBe "\ttimestamp=2017-12-20T01:33:07.000+0000"

      TskvLogWriter.timeStamp(Instant.ofEpochMilli(1513733587050L)) shouldBe "\ttimestamp=2017-12-20T01:33:07.050+0000"

      TskvLogWriter.timeStamp(Instant.ofEpochSecond(1513733587L)) shouldBe "\ttimestamp=2017-12-20T01:33:07.000+0000"
    }
  }
}

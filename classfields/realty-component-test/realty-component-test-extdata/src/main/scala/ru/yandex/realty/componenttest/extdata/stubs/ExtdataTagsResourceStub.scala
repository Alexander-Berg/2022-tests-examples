package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.TagMessage

import scala.collection.JavaConverters._

trait ExtdataTagsResourceStub extends ExtdataResourceStub {

  private val tags: Seq[TagMessage] =
    Seq(
      TagMessage
        .newBuilder()
        .setId(1794401)
        .setTitle("от собственника")
        .setPriority(-1)
        .addAllSuggests(Seq("от собственника", "от хозяина").asJava)
        .build()
    )

  stubGzipped(RealtyDataType.Tags, tags)

}

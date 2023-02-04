package ru.yandex.realty.componenttest.extdata.stubs

import java.util.zip.GZIPOutputStream

import ru.yandex.realty.canonical.base.request.MortgageProgramRequest
import ru.yandex.realty.componenttest.data.banks.Banks
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.CanonicalUrlMessage
import ru.yandex.realty.util.extdata.result

import scala.collection.JavaConverters._

trait ExtdataMortgageProgramCanonicalUrlsResourceStub extends ExtdataResourceStub {

  private val canonicalUrls: Seq[CanonicalUrlMessage] = {
    Banks.all
      .flatMap { bank =>
        bank.getMortgageProgramList.asScala
          .map { mortgageProgram =>
            CanonicalUrlMessage
              .newBuilder()
              .setRequestKey(MortgageProgramRequest(bank, mortgageProgram).key)
              .setCanonicalPart(ComponentTestDataUtils.mortgageProgramCanonicalUrl(mortgageProgram))
              .build()
          }
      }
  }

  stubResource(
    RealtyDataType.MortgageProgramCanonicalUrls,
    result(canonicalUrls)
  )

}

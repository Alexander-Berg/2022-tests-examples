package ru.yandex.auto.vin.decoder.ysign

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.tvm._
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.StringUtils.RichString
import auto.carfax.common.utils.config.Environment.config
import ru.yandex.auto.vin.decoder.ysign.model.{SignContent, SignRequest}
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import sun.security.pkcs.PKCS7
import sun.security.util.DerInputStream

import java.io.{File, FileOutputStream}
import java.util.Base64

@Ignore
class YsignClientIntTest extends AnyFunSuite {

  implicit val t: Traced = Traced.empty

  lazy val remoteService = new RemoteHttpService(
    "ysign",
    new HttpEndpoint("ysign-test.yandex-team.ru", 80, "http")
  )

  lazy val tvmConfig: TvmConfig = DefaultTvmConfig(config.getConfig("auto-vin-decoder.tvm"))
  lazy val tvmTicketsProvider: TvmTicketsProvider = DefaultTvmTicketsProvider(tvmConfig)

  lazy val ysignClient = new YsignClient(remoteService, tvmTicketsProvider)

  test("test sign") {
    val request = SignRequest(
      Seq(
        SignContent(
          "name",
          "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0id2luZG93cy0xMjUxIj8+CjxDb2xsYXRBdXRvPgogIDxSZXF1ZXN0PgogICAgPElkPjEyMzQ1Njc4OTBBQkNERUZHSDwvSWQ+CiAgICA8VHlwZUNvZGU+MTwvVHlwZUNvZGU+CiAgICA8QXV0aD4KICAgICAgPE1lbWJlckNvZGU+NzAwMVNTMDAwMDAwPC9NZW1iZXJDb2RlPgogICAgICA8VXNlcj43MDAxU1MwMDAwMDU8L1VzZXI+CiAgICAgIDxQYXNzd29yZD53eHVTc0NaMzwvUGFzc3dvcmQ+CiAgICA8L0F1dGg+CiAgICA8Q29udGVudHM+CiAgICAgIDxWSU4+V0JBMTIzNDU2Nzg5MDEyMzQ8L1ZJTj4KICAgIDwvQ29udGVudHM+CiAgPC9SZXF1ZXN0Pgo8L0NvbGxhdEF1dG8+"
        )
      ),
      certificateId = 19270,
      certificateType = "ORG",
      taxUnitCode = "YVER",
      login = "kostenko",
      detached = false,
      signatureCAdESType = "BES",
      signatureType = "CAdES",
      confirmationType = "NONE"
    )

    val result = ysignClient.sign(request).await
    println(result)
  }

  test("get tmv ticket") {
    val ticket = tvmTicketsProvider.getServiceTicket(TvmExternalServices.Ysign)
    println(ticket)
  }

  test("to base64") {
    val content =
      "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n<CollatAuto>\n  <Request>\n    <Id>1234567890ABCDEFGH</Id>\n    <TypeCode>1</TypeCode>\n    <Auth>\n      <MemberCode>7001SS000000</MemberCode>\n      <User>7001SS000005</User>\n      <Password>wxuSsCZ3</Password>\n    </Auth>\n    <Contents>\n      <VIN>WBA12345678901234</VIN>\n    </Contents>\n  </Request>\n</CollatAuto>"
    val base64 = content.encodeBase64()
    println(base64)
  }

  test("decode pkcs7 response content") {
    val base64Response =
      "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"
    val pkcs7DecodedContent = getContentFromBase64Pkcs7(base64Response)
    println(pkcs7DecodedContent)
  }

  test("cert base64 to bytes") {
    val base64String =
      "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"
    val bytes = Base64.getDecoder.decode(base64String)

    val file = new File("/Users/sievmi/Desktop/nbki/test.cer")
    val writer = new FileOutputStream(file)
    writer.write(bytes)
    writer.close()

    new String(bytes)

    assert(2 + 2 == 4)
  }

  private def getContentFromBase64Pkcs7(raw: String) = {
    val der = new DerInputStream(Base64.getDecoder.decode(raw))
    val p7 = new PKCS7(der)

    val content = p7.getContentInfo.getContent
    new String(content.toByteArray, "windows-1251")
  }

}

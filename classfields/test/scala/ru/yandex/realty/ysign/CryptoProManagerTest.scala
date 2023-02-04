package ru.yandex.realty.ysign

import cats.implicits.catsSyntaxOptionId
import org.junit.runner.RunWith
import org.scalatest.AsyncFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.ysign.client.YSignClient
import ru.yandex.realty.ysign.client.request.cert.{DssCertificateFormat, GetCertContentRequest}
import ru.yandex.realty.ysign.client.response.cert.{CertResponseStatus, GetCertContentResponse}
import ru.yandex.realty.ysign.client.response.sign.{SignDocumentResponse, SignResponseStatus}
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Base64
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CryptoProManagerTest extends AsyncFunSuite with MockitoSupport {

  val ySignClient: YSignClient = mock[YSignClient]
  val cryptoProManager = new CryptoProManager("test", 2, ySignClient)
  implicit val traced: Traced = Traced.empty

  test("signDocument") {
    val base64cert =
      "MIIDtTCCAp2gAwIBAgIUa4h9dwoAaNXo9af/otT1Rd1iYUswDQYJKoZIhvcNAQELBQAwajELMAkGA1UEBhMCUlUxDzANBgNVBAgMBk1vc2Nvdz" +
        "EPMA0GA1UEBwwGTW9zY293MRMwEQYDVQQKDApZYW5kZXggTExDMSQwIgYJKoZIhvcNAQkBFhVudWtsZWFAeWFuZGV4LXRlYW0ucnUwHhcNMj" +
        "EwNTI3MTYyMzQwWhcNMjQwMzE2MTYyMzQwWjBqMQswCQYDVQQGEwJSVTEPMA0GA1UECAwGTW9zY293MQ8wDQYDVQQHDAZNb3Njb3cxEzARBg" +
        "NVBAoMCllhbmRleCBMTEMxJDAiBgkqhkiG9w0BCQEWFW51a2xlYUB5YW5kZXgtdGVhbS5ydTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQ" +
        "oCggEBALcnN09gxDGOfaiANzSHHc74n9ZEMRv2KTpIj9RUH2ZkvKw4c9NS2hrxVEXzQM7Py+lcDyk0aUFxwU6NtHKBJWyvW3qy0qD6kGlndR" +
        "xadeNaw6NSbwq6vCE7QSoQ58jhkLD5VWgYHztT3da5lkbDOk1hMaPgZxWVYdhjsTVmhPpS+pS2kJFadsFSz90Ej6DwkMXaLKS/YT6rwJuT+O" +
        "xuIfbs44fYh2osbx5c857q9bZfzQRpST21FOtClY5rtsNLSDIbFfBXMD4GRRNRK2N3nRlBTDFNRirZujFm0iS3aAiwQFCfS77MwEom+ACIXR" +
        "U9o67prWARTMsFk9i1rDy+TC8CAwEAAaNTMFEwHQYDVR0OBBYEFKQsPfNUfXS2iU+1AFWiev0AlV+YMB8GA1UdIwQYMBaAFKQsPfNUfXS2iU" +
        "+1AFWiev0AlV+YMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAJNCguJ3i0tXYtuXFh1FynQgmV3bhndFQOEpOlEe9q8BUK" +
        "b0DFzXOWKQpTTOkPeeBIPVPD8OCLXvVpgz3RSKzHxvn7swyhioPbcXgv+LzJTfU+5glQTDwma6o/VBg+ZTUMSK+LgrGfd+gzPjaKGrffnRfj" +
        "XZcrgLAhLYI0JBIgi8+T+0tWQxPbw6kv64xy0d6MEZw8f0oby2qsrC9fOvU//DtPq7FJlrSC1lUNSVBfI+ogZCSZv78H3mZm5Rl1QnAldqmL" +
        "366HaG7AY8+9ESLxvsltHppsd1Lsl/zMMv6NxkFVa7Oeq2qW96I2M4eQaUTTpWH0rvW6tT95LJPo8+k2A="
    when(ySignClient.getCertContent(eq(GetCertContentRequest("test", 2, DssCertificateFormat.RAW)))(?))
      .thenReturn(Future.successful(GetCertContentResponse(CertResponseStatus.OK, "huura", base64cert, 6)))
    val signDocumentResponse =
      SignDocumentResponse(
        SignResponseStatus.OK,
        "hoora",
        Base64.getEncoder
          .encodeToString(
            ("<root>" +
              "<DigestValue>digest</DigestValue>" +
              "<SignatureValue>signature</SignatureValue>" +
              "</root>").getBytes
          )
          .some,
        7
      )
    when(ySignClient.signDocument(?)(?)).thenReturn(Future.successful(signDocumentResponse))

    cryptoProManager.signText("test string").map { signResult =>
      assert(signResult.digest == "digest")
      assert(signResult.signature == "signature")
    }
  }
}

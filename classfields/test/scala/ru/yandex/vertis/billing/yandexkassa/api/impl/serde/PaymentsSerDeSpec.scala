package ru.yandex.vertis.billing.yandexkassa.api.impl.serde

import ru.yandex.vertis.billing.yandexkassa.api.model.payments.{DsrpResponse, OnePhaseApplePayRequest}
import ru.yandex.vertis.billing.yandexkassa.api.model.{CardAuthorizeResult, ErrorCodes, OrderStatuses}
import ru.yandex.vertis.util.crypto.UrlEncodedUtils
import spray.json.JsonParser

/**
  * Specs on [[PaymentsSerDe]]
  *
  * @author alex-kovalenko
  */
class PaymentsSerDeSpec extends SerDeSpecBase {

  val applePayToken =
    "eyJ2ZXJzaW9uIjoiRUNfdjEiLCJkYXRhIjoiQm9mVW9zdUdKbVhocWpHWkJvRmtxb2JMQmFjczh5ZXgwWHM0Um9PaitYUFAwVkIyYjBwckNMQ01mcTFtTFRyWWNGeC9lazdYdU51ak56dFlIeDY0UU9DSWphaEx0SVZxbkZ0d0tZTFFHMXdHd3dOL0E3ZkVXenR4YWl2UWxlMEdmM2g1QzRnY0I3N2RLOTZjcCtLcjBkbG5LeVBudTliRTZJNjJmYTNHMER0MlhteFc1Qy9XN0dzNHFOK2pDaXljSjAvWVdnOWpQd2JSYWdQS0l2OEFDbXRiMnlxbFdqTXcxdDJOZDhzL3oxZlRueVFJU2N1NWErSHNSUnZucGZxeGVPaHZYZW4zZ0tkWDR4dG93bEZoOVVUM29OYTFQQWZabENOTmViKzJzSkU1NFV5WnZLUEdvbTdEN0kwcHJZaS9YNnVVbTZHMTJycnBKTXh6ejJFdmxWemU4WTBMakNQTXoyemxQU3ZnUjZWUlRqS2RyZWxEUlZ5d1RkQ2VnUWwxNHM4SkdIbEFJZUNZV0ZZSVV3PT0iLCJzaWduYXR1cmUiOiJNSUFHQ1NxR1NJYjNEUUVIQXFDQU1JQUNBUUV4RHpBTkJnbGdoa2dCWlFNRUFnRUZBRENBQmdrcWhraUc5dzBCQndFQUFLQ0FNSUlENGpDQ0E0aWdBd0lCQWdJSUpFUHlxQWFkOVhjd0NnWUlLb1pJemowRUF3SXdlakV1TUN3R0ExVUVBd3dsUVhCd2JHVWdRWEJ3YkdsallYUnBiMjRnU1c1MFpXZHlZWFJwYjI0Z1EwRWdMU0JITXpFbU1DUUdBMVVFQ3d3ZFFYQndiR1VnUTJWeWRHbG1hV05oZEdsdmJpQkJkWFJvYjNKcGRIa3hFekFSQmdOVkJBb01Da0Z3Y0d4bElFbHVZeTR4Q3pBSkJnTlZCQVlUQWxWVE1CNFhEVEUwTURreU5USXlNRFl4TVZvWERURTVNRGt5TkRJeU1EWXhNVm93WHpFbE1DTUdBMVVFQXd3Y1pXTmpMWE50Y0MxaWNtOXJaWEl0YzJsbmJsOVZRelF0VUZKUFJERVVNQklHQTFVRUN3d0xhVTlUSUZONWMzUmxiWE14RXpBUkJnTlZCQW9NQ2tGd2NHeGxJRWx1WXk0eEN6QUpCZ05WQkFZVEFsVlRNRmt3RXdZSEtvWkl6ajBDQVFZSUtvWkl6ajBEQVFjRFFnQUV3aFYzN2V2V3g3SWhqMmpkY0pDaElZM0hzTDF2TENnOWhHQ1YyVXIwcFVFYmcwSU8yQkh6UUg2RE14OGNWTVAzNnpJZzFyclYxTy8wa29tSlBud1BFNk9DQWhFd2dnSU5NRVVHQ0NzR0FRVUZCd0VCQkRrd056QTFCZ2dyQmdFRkJRY3dBWVlwYUhSMGNEb3ZMMjlqYzNBdVlYQndiR1V1WTI5dEwyOWpjM0F3TkMxaGNIQnNaV0ZwWTJFek1ERXdIUVlEVlIwT0JCWUVGSlJYMjIvVmRJR0dpWWwyTDM1WGhRZm5tMWdrTUF3R0ExVWRFd0VCL3dRQ01BQXdId1lEVlIwakJCZ3dGb0FVSS9KSnhFK1Q1TzhuNXNUMktHdy9vcnY5TGtzd2dnRWRCZ05WSFNBRWdnRVVNSUlCRURDQ0FRd0dDU3FHU0liM1kyUUZBVENCL2pDQnd3WUlLd1lCQlFVSEFnSXdnYllNZ2JOU1pXeHBZVzVqWlNCdmJpQjBhR2x6SUdObGNuUnBabWxqWVhSbElHSjVJR0Z1ZVNCd1lYSjBlU0JoYzNOMWJXVnpJR0ZqWTJWd2RHRnVZMlVnYjJZZ2RHaGxJSFJvWlc0Z1lYQndiR2xqWVdKc1pTQnpkR0Z1WkdGeVpDQjBaWEp0Y3lCaGJtUWdZMjl1WkdsMGFXOXVjeUJ2WmlCMWMyVXNJR05sY25ScFptbGpZWFJsSUhCdmJHbGplU0JoYm1RZ1kyVnlkR2xtYVdOaGRHbHZiaUJ3Y21GamRHbGpaU0J6ZEdGMFpXMWxiblJ6TGpBMkJnZ3JCZ0VGQlFjQ0FSWXFhSFIwY0RvdkwzZDNkeTVoY0hCc1pTNWpiMjB2WTJWeWRHbG1hV05oZEdWaGRYUm9iM0pwZEhrdk1EUUdBMVVkSHdRdE1Dc3dLYUFub0NXR0kyaDBkSEE2THk5amNtd3VZWEJ3YkdVdVkyOXRMMkZ3Y0d4bFlXbGpZVE11WTNKc01BNEdBMVVkRHdFQi93UUVBd0lIZ0RBUEJna3Foa2lHOTJOa0JoMEVBZ1VBTUFvR0NDcUdTTTQ5QkFNQ0EwZ0FNRVVDSUhLS253K1NveXE1bVhRcjFWNjJjMEJYS3BhSG9kWXU5VFdYRVBVV1BwYnBBaUVBa1RlY2ZXNitXNWwwcjBBRGZ6VENQcTJZdGJTMzl3MDFYSWF5cUJOeThiRXdnZ0x1TUlJQ2RhQURBZ0VDQWdoSmJTKy9PcGphbHpBS0JnZ3Foa2pPUFFRREFqQm5NUnN3R1FZRFZRUUREQkpCY0hCc1pTQlNiMjkwSUVOQklDMGdSek14SmpBa0JnTlZCQXNNSFVGd2NHeGxJRU5sY25ScFptbGpZWFJwYjI0Z1FYVjBhRzl5YVhSNU1STXdFUVlEVlFRS0RBcEJjSEJzWlNCSmJtTXVNUXN3Q1FZRFZRUUdFd0pWVXpBZUZ3MHhOREExTURZeU16UTJNekJhRncweU9UQTFNRFl5TXpRMk16QmFNSG94TGpBc0JnTlZCQU1NSlVGd2NHeGxJRUZ3Y0d4cFkyRjBhVzl1SUVsdWRHVm5jbUYwYVc5dUlFTkJJQzBnUnpNeEpqQWtCZ05WQkFzTUhVRndjR3hsSUVObGNuUnBabWxqWVhScGIyNGdRWFYwYUc5eWFYUjVNUk13RVFZRFZRUUtEQXBCY0hCc1pTQkpibU11TVFzd0NRWURWUVFHRXdKVlV6QlpNQk1HQnlxR1NNNDlBZ0VHQ0NxR1NNNDlBd0VIQTBJQUJQQVhFWVFaMTJTRjFScGVKWUVIZHVpQW91L2VlNjVONEkzOFM1UGhNMWJWWmxzMXJpTFFsM1lOSWs1N3VnajlkaGZPaU10MnUyWnd2c2pvS1lUL1ZFV2pnZmN3Z2ZRd1JnWUlLd1lCQlFVSEFRRUVPakE0TURZR0NDc0dBUVVGQnpBQmhpcG9kSFJ3T2k4dmIyTnpjQzVoY0hCc1pTNWpiMjB2YjJOemNEQTBMV0Z3Y0d4bGNtOXZkR05oWnpNd0hRWURWUjBPQkJZRUZDUHlTY1JQaytUdkorYkU5aWhzUDZLNy9TNUxNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdId1lEVlIwakJCZ3dGb0FVdTdEZW9WZ3ppSnFraXBuZXZyM3JyOXJMSktzd053WURWUjBmQkRBd0xqQXNvQ3FnS0lZbWFIUjBjRG92TDJOeWJDNWhjSEJzWlM1amIyMHZZWEJ3YkdWeWIyOTBZMkZuTXk1amNtd3dEZ1lEVlIwUEFRSC9CQVFEQWdFR01CQUdDaXFHU0liM1kyUUdBZzRFQWdVQU1Bb0dDQ3FHU000OUJBTUNBMmNBTUdRQ01EclBjb05SRnBteGh2czF3MWJLWXIvMEYrM1pEM1ZOb282KzhaeUJYa0szaWZpWTk1dFpuNWpWUVEyUG5lbkMvZ0l3TWkzVlJDR3dvd1YzYkYzek9EdVFaLzBYZkN3aGJaWlB4bkpwZ2hKdlZQaDZmUnVaeTVzSmlTRmhCcGtQQ1pJZEFBQXhnZ0ZnTUlJQlhBSUJBVENCaGpCNk1TNHdMQVlEVlFRRERDVkJjSEJzWlNCQmNIQnNhV05oZEdsdmJpQkpiblJsWjNKaGRHbHZiaUJEUVNBdElFY3pNU1l3SkFZRFZRUUxEQjFCY0hCc1pTQkRaWEowYVdacFkyRjBhVzl1SUVGMWRHaHZjbWwwZVRFVE1CRUdBMVVFQ2d3S1FYQndiR1VnU1c1akxqRUxNQWtHQTFVRUJoTUNWVk1DQ0NSRDhxZ0duZlYzTUEwR0NXQ0dTQUZsQXdRQ0FRVUFvR2t3R0FZSktvWklodmNOQVFrRE1Rc0dDU3FHU0liM0RRRUhBVEFjQmdrcWhraUc5dzBCQ1FVeER4Y05NVFl3T1RJMk1UVXdNekkzV2pBdkJna3Foa2lHOXcwQkNRUXhJZ1FnMzZOS3JFQ2NTMFJISE5yZ2xuUXJ6V1hmVzVSeFQ0dXNNeEJ5N0hCazk2TXdDZ1lJS29aSXpqMEVBd0lFU0RCR0FpRUFnWHB0RjN6Z2dWRk4zWVRvcnkvdmZBOHNXZHFGSDlwc1Y1RVRvK1AzRGNzQ0lRRHczWVpEMCt5ZE9xUGtFUG9ybkpWeWN2TEZOM3hnQkI3OGozVlBwQkt2U0FBQUFBQUFBQT09IiwiaGVhZGVyIjp7ImVwaGVtZXJhbFB1YmxpY0tleSI6Ik1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRTRBVldCUmpUVUcwMlE4TGs2dThGYUhWZkNDVVJQdzZSZGE5RWxDYjc5d2daWW1KWEpPdDVKVG50TFNGc0MvUkhOc09uMnFKVExyNll0dllod2F3MEpnPT0iLCJwdWJsaWNLZXlIYXNoIjoiTTF1R3ZIZVZKSEw3Rjd2MWdYREdWbVU4endtMUYzdXZXaXZJamJJU3cyYz0iLCJ0cmFuc2FjdGlvbklkIjoiYmMyNTVkZTE2ZDhhOGFlMTc5ZTMzN2IwN2ZiNmQyMTFkNzQwYWIzZjM0ZTRjYTc4OTliNzc3ZjUxYWI5NmJiZCJ9fQ=="

  "PaymentsSerDe" should {
    "serialize DsrpRequest" in {
      val rq = OnePhaseApplePayRequest(order, applePayToken, Some("127.0.0.1"))

      val serialized = PaymentsSerDe.serialize(rq, settings)
      val requestData = UrlEncodedUtils.from(serialized).get.toList match {
        case ("request", rqData) :: Nil =>
          rqData
        case other =>
          fail(s"Unexpected $other")
      }

      val parsedPayload = PaymentsSerDe.parseJWS(requestData, settings).getUnverifiedPayload

      val expected = getJson("/api/dsrp_rq_applepay_payload.json")

      JsonParser(parsedPayload) shouldBe expected
    }

    def checkResponse(name: String, expected: DsrpResponse): Unit = {
      s"parse $name DsrpResponse" in {
        val json = getJson(s"/api/dsrp_rs_applepay_$name.json")
        val response = PaymentsSerDe.parseDsrp(json)

        response shouldBe expected
      }
    }

    checkResponse(
      "success",
      DsrpResponse(
        status = Some(OrderStatuses.Authorized),
        orderId = Some("1da5c87d-0984-50e8-a7f3-8de646dd9ec9"),
        cardAuthorizeResult = Some(
          CardAuthorizeResult(
            responseCode = "00",
            rrn = Some("603668680243"),
            authId = Some("062467"),
            eci = Some("02"),
            mpiResult = None
          )
        ),
        error = None,
        parameterName = None,
        errorDescription = None,
        nextRetry = None
      )
    )

    checkResponse(
      "refuse",
      DsrpResponse(
        status = Some(OrderStatuses.Refused),
        orderId = Some("1da5c87d-0984-50e8-a7f3-8de646dd9ec9"),
        error = Some(ErrorCodes.AuthorizationRejected),
        parameterName = None,
        errorDescription = None,
        nextRetry = None,
        cardAuthorizeResult = None
      )
    )

    checkResponse(
      "processing",
      DsrpResponse(
        status = Some(OrderStatuses.Processing),
        orderId = Some("1da5c87d-0984-50e8-a7f3-8de646dd9ec9"),
        nextRetry = Some(5000L),
        error = None,
        parameterName = None,
        errorDescription = None,
        cardAuthorizeResult = None
      )
    )

    checkResponse(
      "error",
      DsrpResponse(
        error = Some(ErrorCodes.IllegalParameter),
        parameterName = Some("paymentToken"),
        errorDescription = None,
        status = None,
        orderId = None,
        nextRetry = None,
        cardAuthorizeResult = None
      )
    )
  }
}

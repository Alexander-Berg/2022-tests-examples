package ru.auto.api.services.complaints

import ru.auto.api.exceptions.ComplaintsBadRequestException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.gen.ComplaintsGenerators._
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.complaints.ComplaintsClient.Domains
import ru.auto.api.util.FutureMatchers._

/**
  * @author potseluev
  */
class DefaultComplaintsClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("complaints-common-api-test-int.slb.vertis.yandex.net", 80)

  private val complaintsClient = new DefaultComplaintsClient(http)

  test("post valid complaint") {
    val complaintsRequest = ComplaintsClientRequestGen.next
    complaintsClient.createComplaint(complaintsRequest).futureValue
  }

  test("fail with BAD_REQUEST on incorrect complaint") {
    val complaintsRequest = ComplaintsClientRequestGen.next.copy(
      objectId = ComplaintsClient.ObjectId(Domains.Autoru, "authorId", "instanceId")
    )
    complaintsClient.createComplaint(complaintsRequest) should failWith[ComplaintsBadRequestException]
  }
}

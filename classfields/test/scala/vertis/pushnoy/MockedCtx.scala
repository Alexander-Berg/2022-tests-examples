package vertis.pushnoy

import ru.yandex.vertis.tracing.Traced
import vertis.pushnoy.model.request.{RequestInfo, RequestInfoImpl}

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 18/09/2017.
  */
trait MockedCtx {
  implicit val ctx: RequestInfo = RequestInfoImpl("test-request-id", Traced.empty)

}

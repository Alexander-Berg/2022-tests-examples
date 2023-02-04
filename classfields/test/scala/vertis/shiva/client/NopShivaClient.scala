package vertis.shiva.client

import ru.yandex.vertis.shiva.api.service_map.api.{ListRequest, ResolveTvmIDRequest, ResolveTvmIDResponse}
import ru.yandex.vertis.shiva.service_map.schema.ServiceMap
import zio.{Task, UIO, ZIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
object NopShivaClient extends ShivaPublicApiClient {

  override def resolveTvmID(request: ResolveTvmIDRequest): Task[ResolveTvmIDResponse] =
    ZIO.fail(new UnsupportedOperationException("resolveTvmID is not supported"))

  override def listServices(request: ListRequest): Task[Seq[ServiceMap]] =
    UIO(Seq.empty)
}

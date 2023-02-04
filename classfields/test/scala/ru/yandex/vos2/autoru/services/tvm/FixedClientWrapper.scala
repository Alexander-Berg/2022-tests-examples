package ru.yandex.vos2.autoru.services.tvm

import org.apache.http.HttpMessage
import ru.yandex.vertis.baker.components.http.client.tvm.FixedIdTvmClientWrapper
import ru.yandex.vertis.baker.components.http.client.tvm.TvmClientWrapper

object FixedTicketWrapper {

  def apply(ticket: String): FixedIdTvmClientWrapper =
    new FixedIdTvmClientWrapper {
      override def getServiceTicket: String = ticket

      override def setServiceTicket(request: HttpMessage): Unit =
        request.addHeader(TvmClientWrapper.ServiceTicketHeaderName, ticket)
    }
}

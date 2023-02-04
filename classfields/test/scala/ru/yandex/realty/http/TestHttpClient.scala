package ru.yandex.realty.http

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder

trait TestHttpClient {
  lazy val testClient: HttpClient = {
    val client = HttpAsyncClientBuilder
      .create()
      .setMaxConnPerRoute(10)
      .setMaxConnTotal(10)
      .build()
    client.start()
    sys.addShutdownHook(client.close())
    new ApacheHttpClient(client)
  }

}

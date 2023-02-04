package ru.auto.api

import io.prometheus.client.Counter

trait AsyncTasksSupport {

  implicit val asyncTasksCounter: Counter = Counter
    .build("test_async_counter", "Async tasks counter for tests")
    .labelNames("name", "error")
    .create()
}

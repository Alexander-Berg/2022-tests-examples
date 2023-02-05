package io.reactivex.schedulers

import org.hamcrest.Matchers
import org.junit.Assert

fun TestScheduler.assertQueueSize(size: Int) {
    Assert.assertThat(
        "Ожидаемое количество заданий в очереди не совпало с реальностью",
        queue.size,
        Matchers.equalTo(size)
    )
}
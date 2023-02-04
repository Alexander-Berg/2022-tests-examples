package ru.yandex.vertis.shark.controller.credit_application.testkit

import ru.yandex.vertis.shark.controller.scheduler.CreditApplicationScheduler
import zio.test.mock.mockable

@mockable[CreditApplicationScheduler.Service]
object CreditApplicationSchedulerMock

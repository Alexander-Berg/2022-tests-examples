package ru.yandex.vertis.uploader.clients.mds

import ru.yandex.vertis.baker.components.akka.AkkaSupport
import ru.yandex.vertis.baker.components.execution.ExecutionContextSupport
import ru.yandex.vertis.baker.components.execution.metrics.ExecutionContextMetricsSupport
import ru.yandex.vertis.baker.components.http.HttpAsyncClientSupport
import ru.yandex.vertis.baker.components.io.IOSupport
import ru.yandex.vertis.baker.components.monitor.service.MonitorSupport
import ru.yandex.vertis.baker.components.operational.OperationalSupport
import ru.yandex.vertis.baker.components.pool.ThreadPoolExecutorSupport
import ru.yandex.vertis.baker.components.time.TimeSupport
import ru.yandex.vertis.baker.components.tracing.TracingSupport
import ru.yandex.vertis.baker.components.uptime.UptimeSupport
import ru.yandex.vertis.baker.components.workersfactory.WorkersFactorySupport
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersExecutionContextSupport
import ru.yandex.vertis.baker.lifecycle.Application

class TestComponents(val app: Application)
  extends TracingSupport
  with IOSupport
  with ThreadPoolExecutorSupport
  with AkkaSupport
  with OperationalSupport
  with ExecutionContextMetricsSupport
  with ExecutionContextSupport
  with WorkersExecutionContextSupport
  with WorkersFactorySupport
  with TimeSupport
  with MonitorSupport
  with UptimeSupport
  with HttpAsyncClientSupport

package ru.yandex.vertis.baker

import ru.yandex.vertis.baker.components.execution.metrics.ExecutionContextMetricsSupport
import ru.yandex.vertis.baker.components.http.HttpAsyncClientSupport
import ru.yandex.vertis.baker.components.io.IOSupport
import ru.yandex.vertis.baker.components.monitor.service.MonitorSupport
import ru.yandex.vertis.baker.components.operational.OperationalSupport
import ru.yandex.vertis.baker.components.time.TimeSupport
import ru.yandex.vertis.baker.components.tracing.TracingSupport
import ru.yandex.vertis.baker.components.workersfactory.WorkersFactorySupport
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersExecutionContextSupport
import ru.yandex.vertis.baker.components.zookeeper.ZookeeperSupport
import ru.yandex.vertis.baker.lifecycle.Application

class TestComponents(val app: Application)
  extends TracingSupport
  with IOSupport
  with ZookeeperSupport
  with OperationalSupport
  with TimeSupport
  with ExecutionContextMetricsSupport
  with WorkersExecutionContextSupport
  with WorkersFactorySupport
  with MonitorSupport
  with HttpAsyncClientSupport

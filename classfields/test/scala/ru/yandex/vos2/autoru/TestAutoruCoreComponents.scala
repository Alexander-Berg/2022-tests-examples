package ru.yandex.vos2.autoru

import ru.yandex.vertis.baker.env.Env
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vos2.autoru.components.DefaultAutoruCoreComponents
import ru.yandex.vertis.ops.test.TestOperationalSupport

object TestAutoruCoreComponents extends DefaultAutoruCoreComponents {
  override val operational: OperationalSupport = TestOperationalSupport
  override lazy val env: Env = new Env(TestEnvProvider)

}

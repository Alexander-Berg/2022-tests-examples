package ru.yandex.vos2.autoru.config

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponents

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 19.03.18
  */
class TestAutoruApiComponents extends DefaultAutoruApiComponents with MockitoSupport {

  override lazy val ops: OperationalSupport = TestOperationalSupport
  override lazy val coreComponents: AutoruCoreComponents = DockerAutoruCoreComponents
}

package ru.yandex.vos2.autoru.utils.docker

import ru.yandex.vertis.ydb.YdbContainer

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-08-07
  */
object LocalYdb {
  val ydb: YdbContainer = ru.yandex.vertis.ydb.YdbContainer.stable
  ydb.container.start()
}

package ru.yandex.vertis.parsing.auto.components.shards

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.util.dao.Shard

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedParsingShardSupport extends ParsingShardAware with MockitoSupport {
  val parsingShard: Shard = mock[Shard]
}
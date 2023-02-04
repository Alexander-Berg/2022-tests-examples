package ru.yandex.vertis.parsing.auto.components.shards

import ru.yandex.vertis.parsing.components.ApplicationAware
import ru.yandex.vertis.parsing.util.dao.{MySql, Shard}

/**
  * TODO
  *
  * @author aborunov
  */
trait ParsingShardSupport extends ParsingShardAware with ApplicationAware {

  val parsingShard: Shard =
    MySql.createNonMeteredShard(config = app.env.props.getConfig("parsing.mysql"), shardName = "parsing")
}

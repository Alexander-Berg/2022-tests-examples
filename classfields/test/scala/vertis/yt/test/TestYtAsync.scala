package vertis.yt.test

import io.netty.channel.EventLoopGroup
import ru.yandex.inside.yt.kosher.acl.async.YtAcl
import ru.yandex.inside.yt.kosher.async.{Yt => YtAsync}
import ru.yandex.inside.yt.kosher.cypress.async.Cypress
import ru.yandex.inside.yt.kosher.tables.async.YtTables
import ru.yandex.inside.yt.kosher.transactions.async.YtTransactions
import ru.yandex.inside.yt.kosher.ytree.YTreeNode

import java.util

/** @author kusaeva
  */
class TestYtAsync extends YtAsync {

  override def cypress(): Cypress = ???

  override def transactions(): YtTransactions = ???

  override def tables(): YtTables = ???

  override def acl(): YtAcl = ???

  override def eventLoopGroup(): EventLoopGroup = ???

  override def withAdditionalParameters(parameters: util.Map[String, YTreeNode]): YtAsync =
    super.withAdditionalParameters(parameters)

  override def withToken(token: String): YtAsync = super.withToken(token)
}

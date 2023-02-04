package ru.yandex.realty.developer.chat
import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.application.ng.chat.ChatClientSupplier
import ru.yandex.realty.application.ng.db.{
  DefaultSlickAsyncExecutorProvider,
  DefaultSlickDatabaseProvider,
  HikariDataSourceProvider,
  SlickMasterSlaveDatabaseProvider
}
import ru.yandex.realty.chat.model.DeveloperChatBotUser
import ru.yandex.realty.chat.util.RoomProperties
import ru.yandex.realty.developer.chat.application.DeveloperChatConsumerConfig
import ru.yandex.realty.developer.chat.service.state.RoomStateTable
import ru.yandex.realty.developer.chat.util.ChatBackendUtils
import ru.yandex.realty.http.DefaultHttpComponents
import ru.yandex.realty.chat.model.RoomId
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.tvm.TvmLibraryComponents
import ru.yandex.realty.util.lang.Futures
import ru.yandex.realty.util.tracing.NoopTracingProvider
import slick.jdbc.MySQLProfile.api._

import scala.async.Async.{async, await}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object SetUidAndDevidTool
  extends App
  with SlickMasterSlaveDatabaseProvider
  with DefaultSlickDatabaseProvider
  with HikariDataSourceProvider
  with DefaultSlickAsyncExecutorProvider
  with TestOperationalComponents
  with ChatClientSupplier
  with DefaultHttpComponents
  with TvmLibraryComponents
  with NoopTracingProvider {

  // honey badger don't care
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val t: Traced = Traced.empty

  val c = DeveloperChatConsumerConfig(DefaultConfigProvider.provideForName("developer-chat"))

  override def chatClientConfig = c.chatClientConfig

  val database = createMasterSlave(c.dbConfig).master

  Await.result(
    async {
      val roomIds = await(database.run(RoomStateTable.table.map(_.roomId).result))
      await(Futures.traverse(roomIds) { roomId =>
        chatClient
          .getRoom(RoomId(roomId))(
            ChatBackendUtils.robotRequestContext(DeveloperChatBotUser, idempotencyKey = None)
          )
          .flatMap { room =>
            database.run(
              RoomStateTable.table
                .filter(_.roomId === room.getId)
                .map(r => (r.userId, r.developerId))
                .update(room.getCreator, RoomProperties.getDeveloperIdOrThrow(room))
            )
          }
      })
    },
    5.seconds
  )

}

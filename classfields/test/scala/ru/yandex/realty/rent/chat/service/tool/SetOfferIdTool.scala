package ru.yandex.realty.rent.chat.service.tool

import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.application.ng.chat.ChatClientSupplier
import ru.yandex.realty.application.ng.db.{
  DefaultSlickAsyncExecutorProvider,
  DefaultSlickDatabaseProvider,
  HikariDataSourceProvider,
  SlickMasterSlaveDatabaseProvider
}
import ru.yandex.realty.chat.model.{RealtyChatUser, RentOfferChatBotUser, RoomId}
import ru.yandex.realty.chat.util.RoomProperties
import ru.yandex.realty.clients.chat.{Client, RobotRequestContext}
import ru.yandex.realty.http.DefaultHttpComponents
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.rent.chat.backend.RentChatConsumerConfig
import ru.yandex.realty.rent.chat.service.state.OfferChatRoomStateTable
import ru.yandex.realty.rent.chat.util.ChatBackendUtils.RobotNameForChatBackend
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.tvm.TvmLibraryComponents
import ru.yandex.realty.util.lang.Futures
import ru.yandex.realty.util.tracing.NoopTracingProvider
import slick.jdbc.MySQLProfile.api._

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object SetOfferIdTool
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

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val t: Traced = Traced.empty

  val c = RentChatConsumerConfig(DefaultConfigProvider.provideForName("rent-chat"))

  override def chatClientConfig = c.chatClientConfig

  val database = createMasterSlave(c.dbConfig).master

  Await.result(
    async {
      val roomIds: Seq[String] = await(database.run(OfferChatRoomStateTable.table.map(_.roomId).result))
      val robotRequestContext = prepareRobotRequestContext(RentOfferChatBotUser)
      await(Futures.traverse(roomIds) { roomId =>
        chatClient
          .getRoom(RoomId(roomId))(robotRequestContext)
          .flatMap { room =>
            RoomProperties.getOfferIdOpt(room) match {
              case Some(offerId) =>
                database.run(
                  OfferChatRoomStateTable.table
                    .filter(_.roomId === room.getId)
                    .map(_.offerId)
                    .update(offerId)
                )
            }
          }
      })
    },
    5.seconds
  )

  private def prepareRobotRequestContext(
    user: RealtyChatUser
  )(implicit trace: Traced): RobotRequestContext = {
    RobotRequestContext(
      requester = Client(),
      user = user.toPlain,
      name = RobotNameForChatBackend,
      trace = trace,
      idempotencyKey = None
    )
  }

}

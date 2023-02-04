package ru.auto.cabinet.test.dao.jdbc

import ru.auto.cabinet.model.ClientId
import ru.auto.cabinet.service.instr.DatabaseProxy
import ru.auto.cabinet.test.dao.entities.BalanceClientsChangedBuffer
import ru.auto.cabinet.test.model.BalanceClientsChangedBufferRecord
import ru.auto.cabinet.trace.Context
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _}

import scala.concurrent.{ExecutionContext, Future}

final class JdbcBalanceClientsChangedBufferDao(
    val database: DatabaseProxy,
    val slaveDatabase: DatabaseProxy)(implicit ec: ExecutionContext) {

  def add(inputRecord: JdbcBalanceClientsChangedBufferDao.InputRecord)(implicit
      rc: Context): Future[Unit] =
    add(Seq(inputRecord))

  def add(inputRecords: Seq[JdbcBalanceClientsChangedBufferDao.InputRecord])(
      implicit rc: Context): Future[Unit] =
    database
      .run(
        BalanceClientsChangedBuffer.add(inputRecords.map { r =>
          BalanceClientsChangedBufferRecord(r.clientId, r.event)
        })
      )
      .map(_ => ())

  def get()(implicit
      rc: Context): Future[Seq[BalanceClientsChangedBufferRecord]] =
    slaveDatabase.run(
      BalanceClientsChangedBuffer.get()
    )
}

object JdbcBalanceClientsChangedBufferDao {
  case class InputRecord(clientId: ClientId, event: String)
}

package ru.auto.cabinet.test.dao.entities

import ru.auto.cabinet.model.ClientId
import ru.auto.cabinet.test.model.BalanceClientsChangedBufferRecord
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _, _}

class BalanceClientsChangedBuffer(tag: Tag)
    extends Table[BalanceClientsChangedBufferRecord](
      tag,
      "clients_changed_buffer") {
  def clientId = column[ClientId]("client_id")
  def event = column[String]("event")

  override def * =
    (
      clientId,
      event) <> (BalanceClientsChangedBufferRecord.tupled, BalanceClientsChangedBufferRecord.unapply)
}

object BalanceClientsChangedBuffer {
  val table = TableQuery[BalanceClientsChangedBuffer]

  def add(record: BalanceClientsChangedBufferRecord) = table += record

  def add(records: Seq[BalanceClientsChangedBufferRecord]) = table ++= records

  def get() = table.result
}

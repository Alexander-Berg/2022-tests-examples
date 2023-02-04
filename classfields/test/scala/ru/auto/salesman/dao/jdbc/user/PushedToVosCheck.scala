package ru.auto.salesman.dao.jdbc.user

import ru.auto.salesman.dao.impl.jdbc.database.Database

trait PushedToVosCheck {

  protected def database: Database
  protected def pushedToVosCheckTable: String

  final protected def setPushedToVos(id: String, pushedToVos: Boolean): Unit =
    database.withSession { session =>
      val ptv = if (pushedToVos) 1 else 0
      session.conn
        .prepareStatement(
          s"UPDATE $pushedToVosCheckTable SET pushed_to_vos=$ptv WHERE goods_id='$id'"
        )
        .execute()
    }

  final protected def getPushedToVos(id: String): Boolean =
    database.withSession { session =>
      val r = session.conn
        .prepareStatement(
          s"SELECT pushed_to_vos FROM $pushedToVosCheckTable WHERE goods_id='$id'"
        )
        .executeQuery()
      r.next()
      r.getBoolean(1)
    }

}

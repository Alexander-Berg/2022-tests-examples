package ru.yandex.verba.core

import org.scalatest.{Outcome, TestSuite}
import ru.yandex.verba.core.util.Logging
import scalikejdbc._
import scalikejdbc.globalsettings.NoCheckForIgnoredParams

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 11.03.13 18:43
  */
trait H2DBTest extends TestSuite with Logging with DBHelpers {

  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false, singleLineMode = true)
  GlobalSettings.nameBindingSQLValidator = NameBindingSQLValidatorSettings(ignoredParams = NoCheckForIgnoredParams)

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:test;MODE=Oracle", "", "")

  var session: DBSession = AutoSession
  implicit def s = session


  override protected def withFixture(test: NoArgTest): Outcome = {
    using(DB(ConnectionPool.borrow())) { db =>
      try {
        db.begin()
        session = db.withinTxSession()
        super.withFixture(test)
      } finally {
        db.rollbackIfActive()
        session = AutoSession
      }
    }
  }
}

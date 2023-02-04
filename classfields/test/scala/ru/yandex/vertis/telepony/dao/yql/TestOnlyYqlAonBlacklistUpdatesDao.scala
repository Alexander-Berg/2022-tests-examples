package ru.yandex.vertis.telepony.dao.yql

import ru.yandex.vertis.telepony.util.yql.YqlClient

import java.util.concurrent.atomic.AtomicInteger
import TestOnlyYqlAonBlacklistUpdatesDao.nextTablePath

/**
  * Should only be used in tests because of the concurrent use of the yt table.
  * Use [[ru.yandex.vertis.telepony.dao.yql.YqlAonBlacklistUpdatesDao]] not in tests
  *
  * @author tolmach
  */
class TestOnlyYqlAonBlacklistUpdatesDao(yqlClient: YqlClient) extends YqlAonBlacklistUpdatesDaoBase(yqlClient) {

  override protected val aonBlacklistYTFolder: String = nextTablePath

}

object TestOnlyYqlAonBlacklistUpdatesDao {

  private val counter = new AtomicInteger()

  private def nextTablePath: String = {
    val i = counter.getAndIncrement()
    s"//home/verticals/test_$i/telepony/aon"
  }

}

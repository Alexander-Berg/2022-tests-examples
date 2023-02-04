package ru.yandex.vertis.billing.dao.impl.jdbc

import com.google.common.util.concurrent.ThreadFactoryBuilder
import ru.yandex.vertis.billing.service.{HoldService, HoldServiceSpec}

import java.util.concurrent.{Executors, TimeUnit}

/**
  * Runnable specs on [[JdbcHoldDao]]
  *
  * @author dimas
  */
class JdbcHoldDaoSpec extends HoldServiceSpec with JdbcSpecTemplate {

  private lazy val cleanScheduler = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactoryBuilder()
      .setNameFormat("JdbcHoldDaoSpec-%d")
      .build()
  )

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true

  override def afterAll(): Unit = {
    cleanScheduler.shutdownNow()

    super.afterAll()
  }

  lazy val holdService: HoldService = {
    val service = new JdbcHoldDao(holdDatabase)
    cleanScheduler.scheduleAtFixedRate(service.newCleaner(), 0, 1, TimeUnit.SECONDS)
    service
  }
}

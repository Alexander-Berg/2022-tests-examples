package ru.yandex.vertis.billing.dao.impl.doobie

import ru.yandex.vertis.billing.GlobalRuntime
import ru.yandex.vertis.billing.dao.{CallCenterCallDao, CallCenterCallDaoSpec}
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcContainerSpec, JdbcSpecTemplate}
import ru.yandex.vertis.billing.util.clean.CleanableDao
import doobie.implicits._
import common.zio.doobie.syntax._
import ru.yandex.vertis.billing.dao.impl.doobie.call_center_call.CallCenterCallDaoLive

import scala.util.Try

class CallCenterCallDaoLiveSpec extends CallCenterCallDaoSpec with JdbcSpecTemplate {

  override protected def callCenterDao: CallCenterCallDao with CleanableDao = {
    val transactor = JdbcContainerSpec.asTransactor(namedCampaignEventDatabase.name)
    new CallCenterCallDaoLive(transactor) with CleanableDao {
      override def clean(): Try[Unit] = {
        Try {
          GlobalRuntime.zioRuntime.unsafeRun(
            sql"DELETE FROM call_center_call".update.run.transactIO(transactor).unit
          )
        }
      }
    }
  }
}

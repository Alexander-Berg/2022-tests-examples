package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.JdbcClientDao
import ru.auto.salesman.dao.{ClientDao, ClientDaoSpec}
import ru.auto.salesman.model.DatabaseName
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.auto.salesman.test.template.MainJdbcSpecTemplate
import zio.blocking._

class JdbcClientDaoSpec extends ClientDaoSpec with MainJdbcSpecTemplate {

  val clientDao: ClientDao =
    new JdbcClientDao(database, DatabaseName(office7), DatabaseName(poi7))

  "ClientDao with db" should {

    "enable call tracking for client" in {
      val clientId = 16281L
      val poiId = 10601L
      val verifyingQuery = sql"""SELECT p.call_tracking_on 
                                 FROM #$poi7.poi p where id = $poiId"""

      def getCallTrackingEnabled = effectBlocking {
        database.withSession { implicit session =>
          verifyingQuery.as[Int].list
        }
      }

      getCallTrackingEnabled.success.value should contain only 0

      val callTrackingForClient = (for {
        _ <- clientDao.setCallTrackingEnabled(clientId, true)
        callTrackingEnabled <- getCallTrackingEnabled
      } yield callTrackingEnabled).success.value

      callTrackingForClient should contain only 1
    }

    "write to buffer table on `getCallTrackingEnabled`" in {
      val clientId = 16281L
      val poiId = 10601L

      val readBuffer = effectBlocking {
        database.withSession { implicit session =>
          sql"""
               SELECT poi_id, data_source from #$poi7.clients_changed_buffer WHERE poi_id = $poiId
             """.as[(Long, String)].list
        }
      }

      (for {
        _ <- clientDao.setCallTrackingEnabled(clientId, true)
        resp <- readBuffer
      } yield resp).success.value should contain(poiId, "poi")
    }
  }
}

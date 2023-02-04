package mockups

import ru.yandex.realty.archive.scheduler.updater.persistence.cassandra.CassandraClient
import ru.yandex.realty.model.archive.ClusterBrothersList.ArchiveOfferClusterBrothersList
import ru.yandex.realty.model.archive.{ArchiveOfferCassandra, ArchiveOfferCassandraRecord}

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.Future

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-03-18
  */

class CassandraClientMockup extends CassandraClient {

  type OfferId = String
  type OffersByAddress = MutableMap[OfferId, (ArchiveOfferCassandra, ArchiveOfferClusterBrothersList)]
  val offers: MutableMap[String, OffersByAddress] = MutableMap.empty
  // slave_offer_id -> master_offer_id
  val clustering: MutableMap[OfferId, OfferId] = MutableMap.empty
  // possible_header_id -> broken_master_id
  val headers: MutableMap[OfferId, OfferId] = MutableMap.empty

  override def getCurrentMasterId(offerId: String): Option[String] = {
    clustering.get(offerId)
  }

  override def getOffersByAddress(address: String): List[ArchiveOfferCassandra] = {
    offers
      .get(address)
      .map {
        _.values.toList.map { case (offer, brothers) => offer }
      }
      .getOrElse(List.empty)
  }

  override def getOfferRecord(address: String, offerId: String): Option[ArchiveOfferCassandraRecord] = {

    offers
      .get(address)
      .flatMap(_.get(offerId))
      .map {
        case (offer, brothers) => ArchiveOfferCassandraRecord(address, offerId, offer, brothers)
      }

  }

  override def writeNewMasterId(slaveOfferId: String, newMasterId: String): Future[Unit] = {
    clustering(slaveOfferId) = newMasterId
    Future.unit
  }

  override def writeRecord(record: ArchiveOfferCassandraRecord): Future[Unit] = {
    val addressOffers = offers.getOrElse(record.address, MutableMap.empty)
    addressOffers(record.offerId) = (record.offer, record.clusterBrothers)
    offers(record.address) = addressOffers
    Future.unit
  }

  override def getAllPossibleHeaders: Map[String, String] = {
    headers.toMap
  }

  override def memorizePossibleHeader(offerId: String, brokenMasterId: String): Unit = {
    headers(offerId) = brokenMasterId
  }

  override def forgetPossibleHeader(offerId: String): Unit = {
    headers.clear()
  }
}

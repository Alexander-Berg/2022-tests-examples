package ru.yandex.complaints.dao.complaints

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.dao.offers.OffersDao
import ru.yandex.complaints.dao.statistics.StatisticsDao
import ru.yandex.complaints.dao.users.UsersDao
import ru.yandex.complaints.model.{Statistics, UserType}

/**
  * Spec for [[ru.yandex.complaints.dao.statistics.StatisticsDao]]
  *
  * @author potseluev
  */
trait StatisticsDaoSpec extends WordSpec with Matchers {

  def statisticDao: StatisticsDao

  def complaintsDao: ComplaintsDao

  def offersDao: OffersDao

  def usersDao: UsersDao

  import Generators._

  "Statistics dao" should {
    "return correct statistics for author by complaint's type" in {
      val (nOffers, nAppellants, nComplaints) = (10, 20, 100)
      val offersId = OfferIdGen.next(nOffers)
      val authorId = AuthorIdGen.next

      offersId.foreach(offersDao.create(_, authorId))

      val appellantsId = UserIdGen.next(nAppellants)
      appellantsId.foreach(usersDao.upsert(_, UserType.Undefined))

      val complaints = ComplaintsGen.forOffers(offersId).fromUsers(appellantsId).next(nComplaints)

      complaints.foreach { c =>
        import c._
        complaintsDao.create(
          complaintId, userId, UserType.Undefined, offerId, c.description, ctype.code, c.created, userData, None
        )
      }

      val (offersForMod, _) = offersId.splitAt(offersId.size / 2)

      ModObjIdGen.next(offersForMod.size).zip(offersForMod).foreach {
        case (modObjId, offerId) => complaintsDao.setModObjId(offerId, modObjId)
      }

      val expectedStatistics = Statistics.GroupedByComplaintTypeAndUserId {
        complaints
          .filter(c => offersForMod.contains(c.offerId))
          .groupBy(_.ctype)
          .mapValues(_.groupBy(_.userId.value).mapValues(_.size))
      }

      val result = statisticDao.groupedByComplaintTypeAndUserId(
        Statistics.Query.GroupedByComplaintTypeAndUserId(authorId)
      ).get

      result shouldBe expectedStatistics
    }

    "return empty statistics when author doesn't exist" in {
      val query = Statistics.Query.GroupedByComplaintTypeAndUserId("author_id_0")
      val result = statisticDao.groupedByComplaintTypeAndUserId(query)
      result.get.data shouldBe empty
    }

  }
}

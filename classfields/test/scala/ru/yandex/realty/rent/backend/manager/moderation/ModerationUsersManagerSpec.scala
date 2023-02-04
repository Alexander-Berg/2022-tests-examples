package ru.yandex.realty.rent.backend.manager.moderation

import com.sksamuel.elastic4s.ElasticDsl.{createIndex, deleteIndex, indexInto, refreshIndex}
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import realty.palma.rent_user.RentUser
import realty.palma.spectrum_report.SpectrumReport
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.backend.manager.{UserManager, UtilManager}
import ru.yandex.realty.rent.clients.elastic.ElasticSearchSpecBase
import ru.yandex.realty.rent.clients.elastic.model.user.{User => ElasticUser}
import ru.yandex.realty.rent.dao.{RentContractDao, UserDao, UserFilter}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{RentContract, User}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class ModerationUsersManagerSpec
  extends SpecBase
  with ElasticSearchSpecBase
  with RentModelsGen
  with RequestAware
  with PropertyChecks
  with Logging {

  before {
    val usersJson = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("elastic/create-users-index.json"))
    val createUsersIndexResponse = elasticSearchClient.createIndex {
      createIndex(ElasticUser.IndexName).source(usersJson)
    }(Traced.empty).futureValue
    createUsersIndexResponse.acknowledged should be(true)
  }

  after {
    val deleteUserIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(ElasticUser.IndexName)
    }(Traced.empty).futureValue
    deleteUserIndexResponse.acknowledged should be(true)
  }

  "ModerationUsersManager" should {
    "find user by full name" in new Wiring with TestHelpers {
      val userId = "id123456"
      val uid = 654321L
      val name = "имя"
      val patronymic = "отчество"
      val surname = "фамилия"
      val user = createUser(uid, userId, name = name, patronymic = patronymic, surname = surname)

      indexUser(user)

      mockUserDaoFindByUids(Set(uid), Iterable(user))
      mockContractDaoFindByFlatIds(Iterable.empty, Iterable.empty)

      val resp = moderationUsersManager.getUsers(userFilter(Some("фамилия"))).futureValue

      resp.getUsersList.size() shouldBe (1)
      resp.getUsersList.get(0).getUser.getUserId shouldBe (userId)
      resp.getUsersList.get(0).getUser.getPerson.getName shouldBe (name)
      resp.getUsersList.get(0).getUser.getPerson.getSurname shouldBe (surname)
    }

    "find user by phone" in new Wiring with TestHelpers {
      val userId = "id112233"
      val uid = 332211L
      val phone = "+79998887766"
      val user = createUser(uid, userId, phone = phone)

      indexUser(user)

      mockUserDaoFindByUids(Set(uid), Iterable(user))
      mockContractDaoFindByFlatIds(Iterable.empty, Iterable.empty)

      val resp = moderationUsersManager.getUsers(userFilter(Some("9998887766"))).futureValue

      resp.getUsersList.size() shouldBe (1)
      resp.getUsersList.get(0).getUser.getUserId shouldBe (userId)
      resp.getUsersList.get(0).getUser.getPhone shouldBe (phone)
    }

    "find user by email" in new Wiring with TestHelpers {
      val userId = "id132435"
      val uid = 324354L
      val email = "test@pisem.net"
      val user = createUser(uid, userId, email = email)

      indexUser(user)

      mockUserDaoFindByUids(Set(uid), Iterable(user))
      mockContractDaoFindByFlatIds(Iterable.empty, Iterable.empty)

      val resp = moderationUsersManager.getUsers(userFilter(Some("test@pisem"))).futureValue

      resp.getUsersList.size() shouldBe (1)
      resp.getUsersList.get(0).getUser.getUserId shouldBe (userId)
      resp.getUsersList.get(0).getUser.getEmail shouldBe (email)
    }
  }

  trait Wiring {
    val mockUserDao = mock[UserDao]
    val mockContractDao = mock[RentContractDao]
    val mockPalmaRentUserClient: PalmaClient[RentUser] = mock[PalmaClient[RentUser]]
    val mockSpectrumDataPalmaClient: PalmaClient[SpectrumReport] = mock[PalmaClient[SpectrumReport]]
    val mockUserManager: UserManager = null
    val mockUtilManager = mock[UtilManager]
    val features = new SimpleFeatures
    features.ModerationUsersFromElastic.setNewState(true)

    val moderationUsersManager = new ModerationUsersManager(
      mockUserDao,
      mockContractDao,
      mockPalmaRentUserClient,
      mockSpectrumDataPalmaClient,
      elasticSearchClient,
      mockUserManager,
      mockUtilManager,
      features
    )
  }

  trait TestHelpers {
    self: Wiring =>

    def mockUserDaoFindByUids(uids: Set[Long], users: Iterable[User]) =
      (mockUserDao
        .findByUids(_: Set[Long])(_: Traced))
        .expects(uids, *)
        .returns(Future.successful(users))
        .once

    def mockContractDaoFindByFlatIds(flatIds: Iterable[String], contracts: Iterable[RentContract]) =
      (mockContractDao
        .findByFlatIds(_: Iterable[String])(_: Traced))
        .expects(flatIds, *)
        .returns(Future.successful(contracts))
        .once

    def indexUser(user: User): Unit = {
      elasticSearchClient
        .index(
          indexInto(ElasticUser.IndexName)
            .id(user.uid.toString)
            .doc(
              ElasticUser(
                user.uid,
                user.userId,
                user.fullName,
                user.phone,
                user.email,
                user.createTime.getMillis
              )
            )
        )(Traced.empty)
        .futureValue
      elasticSearchClient.refresh(refreshIndex(ElasticUser.IndexName))(Traced.empty).futureValue
    }

    def userFilter(query: Option[String]): UserFilter = UserFilter(query, Page(0, 10))

    def createUser(
      uid: Long,
      userId: String,
      name: String = "",
      patronymic: String = "",
      surname: String = "",
      phone: String = "",
      email: String = ""
    ): User = {
      val randomUser = userGen(false).next
      randomUser.copy(
        uid = uid,
        userId = userId,
        name = Some(name),
        patronymic = Some(patronymic),
        surname = Some(surname),
        fullName = Some(s"$name $patronymic $surname"),
        phone = Some(phone),
        email = Some(email)
      )
    }
  }
}

package ru.yandex.realty.rent.backend.validator

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.geohub.api.GeohubApi.{UnifyLocationRequest, UnifyLocationResponse}
import ru.yandex.realty.geohub.proto.api.routes.{InsidePolygonRequest, InsidePolygonResponse}
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.dao.UserDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.Flat
import ru.yandex.realty.rent.model.enums.{OwnerRequestStatus, Role}
import ru.yandex.realty.rent.proto.api.flats.FlatValidationErrorNamespace.FlatValidationError
import ru.yandex.realty.rent.proto.model.flat.FlatData
import ru.yandex.realty.telepony.AsyncPhoneUnifierClient.InvalidPhoneException
import ru.yandex.realty.telepony.{AsyncPhoneUnifierClient, PhoneInfo}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.ProtobufUtils

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatDraftValidatorSpec extends AsyncSpecBase with RentModelsGen {
  import ProtobufUtils.fromJson

  import Future.{failed, successful}
  implicit private val trace: Traced = Traced.empty

  trait TestEnv {
    val features = new SimpleFeatures()

    val geohubClient = new AnyRef {
      val obj = mock[GeohubClient]
      val unifyLocation = toMockFunction2(obj.unifyLocation(_: UnifyLocationRequest)(_: Traced))
      val isPointInsidePolygon = toMockFunction2(obj.isPointInsidePolygon(_: InsidePolygonRequest)(_: Traced))
    }

    val phoneUnifyClient = new AnyRef {
      val obj = mock[AsyncPhoneUnifierClient]
      val unify = toMockFunction2(obj.unify(_: String)(_: Traced))
    }

    val userDao = new AnyRef {
      val obj = mock[UserDao]
      val findByUids = toMockFunction2(obj.findByUids(_: Set[Long])(_: Traced))
      def findByUidsDefault: Unit = findByUids.expects(*, *).returning(successful(users))
    }

    val validator = new FlatDraftValidator(geohubClient.obj, phoneUnifyClient.obj, features, userDao.obj)
  }

  val phone = "+79998883333"
  val address = "Россия, Санкт-Петербург, Ленинский проспект, 140"
  val flatNumber = "6"
  val user1 = userGen(false).next.copy(uid = 1L)
  val user2 = userGen(false).next.copy(uid = 2L)
  val users = Seq(user1, user2)
  val passportUser = PassportUser(user1.uid)

  val flatPrototype = Flat(
    flatId = "27fd9023bf3c4b43bd2fd25437fa3513",
    code = Some("27-FAAD"),
    address = address,
    unifiedAddress = None, //Some("Россия, Санкт-Петербург, Ленинский проспект, 140"),
    flatNumber = flatNumber,
    nameFromRequest = Some("Михайлов Руслан Иванович"),
    phoneFromRequest = Some(phone),
    isRented = false,
    keyCode = None,
    ownerRequests = Nil,
    assignedUsers = Map(
      Role.Owner -> users
    ),
    createTime = DateTime.now(),
    updateTime = DateTime.now(),
    visitTime = None,
    shardKey = 0,
    data = fromJson(
      FlatData.getDefaultInstance,
      s"""{
         |  "phone": "$phone",
         |  "person": {
         |    "name": "Руслан",
         |    "surname": "Михайлов",
         |    "patronymic": "Иванович"
         |  },
         |  "email": "qwerty@yandex.ru"
         |}""".stripMargin
    )
  )

  val usersWithFlatsData = Seq(
    user1.copy(
      assignedFlats = Map(
        Role.Owner -> Seq(
          flatGen(false).next.copy(
            address = address,
            flatNumber = flatNumber,
            ownerRequests = Seq(ownerRequestGen.next.copy(status = OwnerRequestStatus.Confirmed))
          )
        )
      )
    ),
    user2
  )

  val successfulUnifyLocationResponse = fromJson(
    UnifyLocationResponse.getDefaultInstance,
    """{
      |  "location": {
      |    "localityName": "Санкт-Петербург",
      |    "street": "Ленинский проспект",
      |    "houseNum": "140",
      |    "geocoderPoint": { "version": 1, "latitude": 59.85247, "longitude": 30.2852 },
      |    "geocoderAddress": "Россия, Санкт-Петербург, Ленинский проспект, 140",
      |    "rawAddress": "Россия, Санкт-Петербург, Ленинский проспект, 140",
      |    "accuracyInt": 1,
      |    "regionGraphId": "287842",
      |    "parsingStatus": 1,
      |    "subjectFederationId": 10174,
      |    "regionName": "Санкт-Петербург",
      |    "structuredAddress2": {
      |      "unifiedOneline": "",
      |      "component": [
      |        {
      |          "value": "140",
      |          "regionType": "HOUSE",
      |          "geoId": 2,
      |          "rgid": "417899",
      |          "valueForAddress": "140"
      |        }
      |      ]
      |    },
      |    "subjectFederationRgid": "741965",
      |    "populatedRgid": "741965"
      |  }
      |}""".stripMargin
  )

  "FlatDraftValidator" when {
    "validateFlatDraft" should {
      "return no validation error on valid flat" in new TestEnv {
        userDao.findByUidsDefault
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation
          .expects(where {
            case (request, _) => request.getRawLocation.getAddress == address
          })
          .returning(successful(successfulUnifyLocationResponse))
          .once()
        geohubClient.isPointInsidePolygon
          .expects(where {
            case (request, _) =>
              request.getPoint.getLatitude == 59.85247f &&
                request.getPoint.getLongitude == 30.2852f
          })
          .returning(successful(InsidePolygonResponse.newBuilder().setIsPointInsidePolygon(true).build()))
          .once()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue
        res.getValidationErrorsCount should be(0)
      }
      "return no validation error on valid flat with feature.CheckRentPolygonInApi = false" in new TestEnv {
        userDao.findByUidsDefault
        features.CheckRentPolygonInApi.setNewState(false)
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation
          .expects(where {
            case (request, _) => request.getRawLocation.getAddress == address
          })
          .returning(successful(successfulUnifyLocationResponse))
          .once()
        geohubClient.isPointInsidePolygon.expects(*, *).never()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue
        res.getValidationErrorsCount should be(0)
      }
      "return error if subject federation outside rent area" in new TestEnv {
        userDao.findByUidsDefault
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation
          .expects(where {
            case (request, _) => request.getRawLocation.getAddress == address
          })
          .returning(successful {
            val builder = successfulUnifyLocationResponse.toBuilder
            builder.getLocationBuilder.setSubjectFederationId(0)
            builder.build()
          })
          .once()
        geohubClient.isPointInsidePolygon.expects(*, *).never()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.ADDRESS_OUTSIDE_OF_WORKING_POLYGON)
      }
      "return error if address outside rent polygon" in new TestEnv {
        userDao.findByUidsDefault
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation
          .expects(where {
            case (request, _) => request.getRawLocation.getAddress == address
          })
          .returning(successful(successfulUnifyLocationResponse))
          .once()
        geohubClient.isPointInsidePolygon
          .expects(*, *)
          .returning(successful(InsidePolygonResponse.newBuilder().setIsPointInsidePolygon(false).build()))
          .once()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.ADDRESS_OUTSIDE_OF_WORKING_POLYGON)
      }
      "return error if address without house" in new TestEnv {
        userDao.findByUidsDefault
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation
          .expects(*, *)
          .returning(successful {
            val builder = successfulUnifyLocationResponse.toBuilder
            builder.getLocationBuilder.clearStructuredAddress2()
            builder.build()
          })
          .once()
        geohubClient.isPointInsidePolygon.expects(*, *).never()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.ADDRESS_NOT_HOUSE)
      }
      "return error if phone is invalid" in new TestEnv {
        userDao.findByUidsDefault
        val phoneException = new InvalidPhoneException("Invalid phone", new Exception())
        phoneUnifyClient.unify.expects(phone, *).returning(failed(phoneException)).once()
        geohubClient.unifyLocation.expects(*, *).returning(successful(successfulUnifyLocationResponse)).once()
        geohubClient.isPointInsidePolygon
          .expects(*, *)
          .returning(successful(InsidePolygonResponse.newBuilder().setIsPointInsidePolygon(true).build()))
          .once()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.INVALID_PHONE)
      }
      "return error if phone is empty" in new TestEnv {
        userDao.findByUidsDefault
        phoneUnifyClient.unify.expects(phone, *).never()
        geohubClient.unifyLocation.expects(*, *).returning(successful(successfulUnifyLocationResponse)).once()
        geohubClient.isPointInsidePolygon
          .expects(*, *)
          .returning(successful(InsidePolygonResponse.newBuilder().setIsPointInsidePolygon(true).build()))
          .once()
        val data = flatPrototype.data.toBuilder.setPhone("").build()
        val flat = flatPrototype.copy(data = data)
        val res = validator.validateFlatDraft(flat, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.EMPTY_PHONE)
      }
      "return error if flat is a duplicate" in new TestEnv {
        userDao.findByUids.expects(Set(1L), *).returning(successful(usersWithFlatsData))
        phoneUnifyClient.unify.expects(phone, *).returning(successful(PhoneInfo(phone, 1, ""))).once()
        geohubClient.unifyLocation.expects(*, *).returning(successful(successfulUnifyLocationResponse)).once()
        geohubClient.isPointInsidePolygon
          .expects(*, *)
          .returning(successful(InsidePolygonResponse.newBuilder().setIsPointInsidePolygon(true).build()))
          .once()
        val res = validator.validateFlatDraft(flatPrototype, passportUser).futureValue.getValidationErrorsList.asScala
        res.length should be(1)
        res.head.getCode should be(FlatValidationError.DUPLICATE_FLAT)
      }
    }
  }
}

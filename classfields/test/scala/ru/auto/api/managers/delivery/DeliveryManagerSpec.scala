package ru.auto.api.managers.delivery

import org.mockito.Mockito._
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, DeliveryInfo, DeliveryRegion, Location, Section}
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.{GeoPoint, RegionInfo}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.DeliveryUpdateFailed
import ru.auto.api.geo.{Region, RegionTypes, Tree}
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.CategorySelector.{Cars, Moto}
import ru.auto.api.model.ModelGenerators.DeliveryInfoGen
import ru.auto.api.model.{ModelGenerators, OfferID, RequestParams}
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.util.Success

class DeliveryManagerSpec extends BaseSpec with MockitoSupport {

  private val vosClient = mock[VosClient]
  private val geobaseClient = mock[GeobaseClient]
  private val tree = mock[Tree]
  private val offerLoader = mock[OfferLoader]

  private val deliveryManager =
    new DeliveryManager(vosClient, offerLoader, geobaseClient, tree)

  private val offerId = OfferID.parse("1234-5678")
  private val allowedCategory = Cars
  private val notAllowedCategory = Moto
  private val latitude = 1.0
  private val longitude = 2.0

  private val deliveryInfo = DeliveryInfo
    .newBuilder()
    .addDeliveryRegions(
      DeliveryRegion
        .newBuilder()
        .setLocation(
          Location
            .newBuilder()
            .setAddress("Sadovnicheskaya 82.2")
            .setCoord(
              GeoPoint
                .newBuilder()
                .setLatitude(latitude)
                .setLongitude(longitude)
            )
        )
    )
    .build()

  private val closestRegionId = 1

  private val city = Region(
    213,
    1,
    RegionTypes.City,
    "Moscow",
    "Moscow",
    "Moscow",
    "Moscow",
    "Moscow",
    "Moscow",
    1.123,
    2.345,
    1,
    None
  )

  private val closestRegion = city.copy(id = closestRegionId)

  private val federalSubject = city.copy(id = 1)

  private val expectedDeliveryInfo = DeliveryInfo
    .newBuilder()
    .addDeliveryRegions(
      DeliveryRegion
        .newBuilder()
        .setLocation(
          Location
            .newBuilder()
            .setAddress("Sadovnicheskaya 82.2")
            .setGeobaseId(closestRegionId)
            .setFederalSubjectId(federalSubject.id)
            .setRegionInfo(
              RegionInfo
                .newBuilder()
                .setName(city.name)
                .setId(city.id)
            )
            .setCoord(
              GeoPoint
                .newBuilder()
                .setLatitude(latitude)
                .setLongitude(longitude)
            )
        )
    )
    .build()

  implicit private val trace = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  private val userRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setUser(ModelGenerators.PrivateUserRefGen.next)
    req.setRequestParams(RequestParams.empty)
    req
  }

  private val dealerRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    val dealerRef = ModelGenerators.DealerUserRefGen.next
    val userRef = ModelGenerators.DealerUserRefGen.next
    req.setDealer(dealerRef)
    req.setUser(userRef)
    req.setRequestParams(RequestParams.empty)
    req
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    when(geobaseClient.regionIdByLocation(latitude, longitude))
      .thenReturnF(closestRegionId)
    when(geobaseClient.regionIdByLocation(latitude, longitude))
      .thenReturnF(closestRegionId)
    when(tree.findCityByRegionId(closestRegionId))
      .thenReturn(Success(city))
    when(tree.unsafeFederalSubject(closestRegionId))
      .thenReturn(federalSubject)
    when(offerLoader.findRawOffer(eq(allowedCategory), ?, ?, ?)(?))
      .thenReturnF {
        ModelGenerators.OfferGen.next.toBuilder
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .build()
      }
    when(offerLoader.findRawOffer(eq(notAllowedCategory), ?, ?, ?)(?))
      .thenReturnF {
        ModelGenerators.OfferGen.next.toBuilder
          .setCategory(Category.CARS)
          .setSection(Section.NEW)
          .build()
      }
  }

  "DeliveryManager" should {

    "update delivery" in {
      when(vosClient.updateDelivery(?, ?, ?, ?)(?))
        .thenReturnF(())

      deliveryManager
        .updateDelivery(offerId, allowedCategory, Some(deliveryInfo))(dealerRequest)
        .futureValue

      verify(vosClient).updateDelivery(allowedCategory, dealerRequest.user.ref.asDealer, offerId, expectedDeliveryInfo)(
        dealerRequest
      )
    }

    "update delivery with default value if no deliveryInfo present" in {
      reset(vosClient)
      when(vosClient.updateDelivery(?, ?, ?, ?)(?))
        .thenReturnF(())

      deliveryManager
        .updateDelivery(offerId, allowedCategory, None)(dealerRequest)
        .futureValue

      verify(vosClient).updateDelivery(
        allowedCategory,
        dealerRequest.user.ref.asDealer,
        offerId,
        DeliveryInfo.getDefaultInstance
      )(dealerRequest)
    }

    "not update delivery if not dealer" in {
      deliveryManager
        .updateDelivery(offerId, allowedCategory, Some(deliveryInfo))(userRequest)
        .failed
        .futureValue shouldBe a[DeliveryUpdateFailed]
      verifyNoMoreInteractions(vosClient)
    }

    "not update delivery if category not allowed" in {
      deliveryManager
        .updateDelivery(offerId, notAllowedCategory, Some(deliveryInfo))(userRequest)
        .failed
        .futureValue shouldBe a[DeliveryUpdateFailed]
      verifyNoMoreInteractions(vosClient)
    }

    "not update delivery if section not allowed" in {
      when(offerLoader.findRawOffer(eq(allowedCategory), ?, ?, ?)(?))
        .thenReturnF {
          ModelGenerators.OfferGen.next.toBuilder
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
            .build()
        }
      deliveryManager
        .updateDelivery(offerId, notAllowedCategory, Some(deliveryInfo))(userRequest)
        .failed
        .futureValue shouldBe a[DeliveryUpdateFailed]
      verifyNoMoreInteractions(vosClient)
    }

    "not update delivery deliveryInfo is invalid" in {
      when(offerLoader.findRawOffer(eq(allowedCategory), ?, ?, ?)(?))
        .thenReturnF {
          ModelGenerators.OfferGen.next.toBuilder
            .setCategory(Category.CARS)
            .setSection(Section.USED)
            .build()
        }

      val invalidDeliveryInfo = DeliveryInfoGen.next.toBuilder.clearDeliveryRegions().build()
      deliveryManager
        .updateDelivery(offerId, allowedCategory, Some(invalidDeliveryInfo))(dealerRequest)
        .failed
        .futureValue shouldBe a[IllegalArgumentException]
      verifyNoMoreInteractions(vosClient)
    }

    "not update delivery if VOS not available" in {
      when(vosClient.updateDelivery(?, ?, ?, ?)(?))
        .thenThrowF(new RuntimeException("test"))

      deliveryManager
        .updateDelivery(offerId, allowedCategory, Some(deliveryInfo))(dealerRequest)
        .failed
        .futureValue shouldBe a[RuntimeException]
    }
  }
}

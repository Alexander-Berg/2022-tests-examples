package ru.auto.api.managers.recalls

import org.mockito.Mockito.reset
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{NavigatorVehicleInfoResponse, NavigatorVehicleRecallsResponse, NavigatorVehicleResponse}
import ru.auto.api.exceptions._
import ru.auto.api.managers.carfax.CarfaxManager
import ru.auto.api.managers.carfax.CarfaxManager.VinResolveResult
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.vin.{LicensePlate, VinCode}
import ru.auto.api.services.recalls.RecallsClient
import ru.auto.api.util.{ManagerUtils, Request}
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class VehicleManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {
  private val recallsClient = mock[RecallsClient]
  private val carfaxManager = mock[CarfaxManager]

  implicit private val trace: Traced = Traced.empty
  implicit private val request: Request = RequestGen.next

  private val manager = new VehicleManager(recallsClient, carfaxManager)

  private val vin = "Z8T4C5S19BM005269"
  private val maskedVin = "Z8T**************"
  private val licensePlate = "H116XP174"

  private val invalidVin = "VVVVVVVVVVVVVVVVV"
  private val invalidLicensePlate = "a000aa00"

  before {
    reset(recallsClient, carfaxManager)
  }

  "VehicleManager" should {
    "return info" in {
      when(recallsClient.getVehicleInfo(eq(vin))(?)).thenReturnF {
        val responseBuilder = NavigatorVehicleInfoResponse.newBuilder()
        responseBuilder.getVehicleInfoBuilder.setVinCode(vin)
        responseBuilder.build()
      }

      val response = manager.getInfo(VinCode(vin)).futureValue
      val vehicleInfo = response.getVehicleInfo
      assert(vehicleInfo.getVinCode.isEmpty)
      assert(vehicleInfo.getVinCodeMasked == maskedVin)
    }

    "return info and resolve vin" in {
      when(carfaxManager.translateToVin(eq(licensePlate))(?))
        .thenReturnF(VinResolveResult(vin, IdentifierType.LICENSE_PLATE, Some(licensePlate)))
      when(recallsClient.getVehicleInfo(eq(vin))(?)).thenReturnF {
        val responseBuilder = NavigatorVehicleInfoResponse.newBuilder()
        responseBuilder.getVehicleInfoBuilder.setVinCode(vin)
        responseBuilder.build()
      }
      val response = manager.getInfo(LicensePlate(licensePlate)).futureValue
      val vehicleInfo = response.getVehicleInfo
      assert(vehicleInfo.getVinCode.isEmpty)
      assert(vehicleInfo.getVinCodeMasked == maskedVin)
    }

    "return info and vin resolving in progress" in {
      when(carfaxManager.translateToVin(eq(licensePlate))(?)).thenThrowF(new InProgress(""))
      assertThrows[NavigatorVehicleInfoNotReady] {
        manager.getInfo(LicensePlate(licensePlate)).await
      }
    }

    "add vehicle" in {
      when(recallsClient.addVehicle(?, ?, ?)(?)).thenReturnF {
        val responseBuilder = NavigatorVehicleResponse.newBuilder()
        responseBuilder.getVehicleBuilder.getInfoBuilder.setVinCode(vin)
        responseBuilder.build()
      }
      val response = manager.add(1, subscribe = true).futureValue
      val vehicleInfo = response.getVehicle.getInfo
      assert(vehicleInfo.getVinCode.isEmpty)
      assert(vehicleInfo.getVinCodeMasked == maskedVin)
    }

    "delete vehicle" in {
      when(recallsClient.deleteVehicle(?, ?)(?)).thenReturn(Future.unit)
      val response = manager.delete(1).futureValue
      assert(response == ManagerUtils.SuccessResponse)
    }

    "get recalls" in {
      when(recallsClient.getVehicleRecalls(?, ?)(?)).thenReturnF(NavigatorVehicleRecallsResponse.newBuilder().build())
      val response = manager.getRecalls(1).futureValue
      assert(response.getRecallsCount == 0)
    }

    "raise invalid vin exception" in {
      when(recallsClient.getVehicleInfo(eq(invalidVin))(?)).thenThrowF(new NavigatorInvalidVehicleIdentifier)
      assertThrows[NavigatorInvalidVehicleIdentifier] {
        manager.getInfo(VinCode(invalidVin)).await
      }
    }

    Set(VinInvalid, LicensePlateInvalid).foreach { exc =>
      s"raise $exc exception" in {
        when(carfaxManager.translateToVin(eq(invalidLicensePlate))(?)).thenThrowF(exc("", None))
        assertThrows[NavigatorInvalidVehicleIdentifier] {
          manager.getInfo(LicensePlate(invalidLicensePlate)).await
        }
      }
    }
  }
}

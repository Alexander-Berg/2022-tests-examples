package auto.dealers.dealer_pony.storage.resources.test

import auto.common.manager.cabinet.model._
import auto.common.model.ClientId
import auto.dealers.dealer_pony.storage.dao.RedirectConfigOverrideForRegions
import auto.dealers.dealer_pony.storage.resources.DefaultRedirectConfigOverrideForRegions
import auto.dealers.dealer_pony.storage.resources.DefaultRedirectConfigOverrideForRegions.SettingsOverrides
import common.geobase.model.RegionIds.RegionId
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.dealer_pony.palma.proto.palma.{Transport => ProtoTransportType}
import zio._
import zio.test._

object RedirectConfigOverrideForRegionsSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[Environment, Failure] =
    suite("RedirectConfigOverrideForRegions")(
      noOverrideConfigForRegionTest,
      noOverrideConfigForTransportTest,
      calltrackingEnabledOverrideTrueTest,
      calltrackingEnabledOverrideFalseTest,
      calltrackingEnabledOverrideMissingTest,
      calltrackingByOfferEnabledOverrideTrueTest,
      calltrackingByOfferEnabledOverrideFalseTest,
      calltrackingByOfferEnabledOverrideMissingTest
    )

  val clientDefault = Client(
    id = ClientId(0),
    regionId = RegionId(1)
  )

  val clientSettingsCalltrackingEnabledDefault = true
  val clientSettingsOffersStatEnabledDefault = true

  val noOverrideConfigForRegionTest =
    testM("no override config for region") {
      val client = clientDefault

      val clientSettingsCalltrackingEnabled = false

      val conf = createConfigOverride(
        RegionId(9999),
        transport = List(ProtoTransportType.CARS_USED),
        callTrackingEnabled = Some(true)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingEnabled(
          client,
          clientSettingsCalltrackingEnabled,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { calltrackingEnabled =>
          assertTrue(calltrackingEnabled == false)
        }
    }

  val noOverrideConfigForTransportTest =
    testM("no override config for transport") {
      val client = clientDefault

      val clientSettingsCalltrackingEnabled = false

      val conf = createConfigOverride(
        RegionId(1),
        transport = List(ProtoTransportType.CARS_NEW),
        callTrackingEnabled = Some(true)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingEnabled(client, clientSettingsCalltrackingEnabled, Category.CARS, Section.USED, paidCall = false)
      )
        .provideSomeLayer(conf)
        .map { calltrackingEnabled =>
          assertTrue(calltrackingEnabled == false)
        }

    }

  val calltrackingEnabledOverrideTrueTest =
    testM("calltracking enabled override true") {
      val client = clientDefault

      val conf = createConfigOverride(
        RegionId(1),
        transport = List(ProtoTransportType.CARS_USED),
        callTrackingEnabled = Some(true)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingEnabled(
          client,
          clientSettingsCalltrackingEnabledDefault,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { callTrackingEnabled =>
          assertTrue(callTrackingEnabled)
        }
    }

  val calltrackingEnabledOverrideFalseTest =
    testM("calltracking enabled override false") {
      val client = clientDefault

      val conf = createConfigOverride(
        RegionId(1),
        transport = List(ProtoTransportType.CARS_USED),
        callTrackingEnabled = Some(false)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingEnabled(
          client,
          clientSettingsCalltrackingEnabledDefault,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { callTrackingEnabled =>
          assertTrue(callTrackingEnabled == false)
        }
    }

  val calltrackingEnabledOverrideMissingTest =
    testM("calltracking enabled is missing in override config") {
      val client = clientDefault
      val clientSettingsCalltrackingEnabled = false

      val conf = createConfigOverride(
        client.regionId,
        transport = List(ProtoTransportType.CARS_USED)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingEnabled(client, clientSettingsCalltrackingEnabled, Category.CARS, Section.USED, paidCall = false)
      )
        .provideSomeLayer(conf)
        .map { calltrackingEnabled =>
          assertTrue(calltrackingEnabled == false)
        }
    }

  val calltrackingByOfferEnabledOverrideTrueTest =
    testM("calltracking by offer enabled override true") {
      val client = clientDefault

      val conf = createConfigOverride(
        RegionId(1),
        transport = List(ProtoTransportType.CARS_USED),
        callTrackingByOfferEnabled = Some(true)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingByOfferEnabled(
          client,
          clientSettingsOffersStatEnabledDefault,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { callTrackingByOfferEnabled =>
          assertTrue(callTrackingByOfferEnabled)
        }
    }

  val calltrackingByOfferEnabledOverrideFalseTest =
    testM("calltracking by offer enabled override false") {
      val client = clientDefault

      val conf = createConfigOverride(
        RegionId(1),
        transport = List(ProtoTransportType.CARS_USED),
        callTrackingByOfferEnabled = Some(false)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingByOfferEnabled(
          client,
          clientSettingsOffersStatEnabledDefault,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { callTrackingByOfferEnabled =>
          assertTrue(callTrackingByOfferEnabled == false)
        }
    }

  val calltrackingByOfferEnabledOverrideMissingTest =
    testM("calltracking by offer enabled is missing in override config") {
      val client = clientDefault

      val conf = createConfigOverride(
        client.regionId,
        transport = List(ProtoTransportType.CARS_USED)
      )

      RedirectConfigOverrideForRegions(
        _.callTrackingByOfferEnabled(
          client,
          clientSettingsOffersStatEnabledDefault,
          Category.CARS,
          Section.USED,
          paidCall = false
        )
      )
        .provideSomeLayer(conf)
        .map { callTrackingByOfferEnabled =>
          assertTrue(callTrackingByOfferEnabled)
        }
    }

  private def createConfigOverride(
      regionId: RegionId,
      transport: List[ProtoTransportType],
      callTrackingEnabled: Option[Boolean] = None,
      callTrackingByOfferEnabled: Option[Boolean] = None): ULayer[Has[RedirectConfigOverrideForRegions]] = {

    ZLayer.fromEffect {
      val settings = SettingsOverrides(callTrackingEnabled, callTrackingByOfferEnabled)

      val transportMap = transport.map { t =>
        t -> settings
      }.toMap

      val map = Map(regionId -> transportMap)

      ZRef.make(map).map(new DefaultRedirectConfigOverrideForRegions(_))
    }
  }

}

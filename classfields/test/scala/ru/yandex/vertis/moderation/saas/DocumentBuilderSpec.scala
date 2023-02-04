package ru.yandex.vertis.moderation.saas

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, SpecBase}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Essentials, ExternalId, RealtyEssentials}
import ru.yandex.vertis.moderation.model.realty.AreaInfo
import ru.yandex.vertis.moderation.model.signal.{NoMarker, Signal, SignalSet}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.proto.RealtyLight.AreaUnit
import ru.yandex.vertis.moderation.searcher.core.saas
import ru.yandex.vertis.moderation.searcher.core.saas.client.{SaasOptions, SaasPrefixes, UpdateRequestBuilder}
import ru.yandex.vertis.moderation.searcher.core.saas.document.{AutoruFieldBuilder, DocumentBuilder}
import ru.yandex.vertis.moderation.searcher.core.saas.search.AutoruSearchQuery.UserQuota
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.duration._

/**
  * Specs on [[DocumentBuilder]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class DocumentBuilderSpec extends SpecBase {

  val saasOptions =
    new SaasOptions {
      def saasPrefixes: SaasPrefixes = SaasPrefixes(100500, Seq(100500), Seq(100500))
      def usePruning = true
      def realtime = true
    }

  def documentBuilder(service: Service) =
    new DocumentBuilder(
      Globals.opinionCalculator(service)
    )

  "documentBuilder" should {

    val updateRequestBuilder = new UpdateRequestBuilder(saasOptions, ttl = Some(5.minutes))

    "build doc for autoru" in {
      val externalId = ExternalId("auto_ru_1#1053016832-b0499")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated = CoreGenerators.instanceGen(CoreGenerators.AutoruEssentialsGen).next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          include(""""s_obj_id":[{"type":"#l","value":"1053016832-b0499"},{"type":"#l","value":"1053016832"}]""")

    }
    "build doc for realty with multiple geocoder_id" in {
      val externalId = ExternalId("partner_1#1")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated =
        CoreGenerators
          .instanceGen(CoreGenerators.RealtyEssentialsGen.suchThat(_.geoInfo.toSeq.flatMap(_.geocoderId).nonEmpty))
          .next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          include(""""i_geocoder_id":[{"type":"#i"""")
    }
    "build doc for realty with empty geocoder_id" in {
      val externalId = ExternalId("partner_1#1")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated =
        CoreGenerators
          .instanceGen(CoreGenerators.RealtyEssentialsGen.suchThat(_.geoInfo.toSeq.flatMap(_.geocoderId).isEmpty))
          .next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        (updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          not).include("i_geocoder_id")
    }
    "build doc for realty with multiple region_id" in {
      val externalId = ExternalId("partner_1#1")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated =
        CoreGenerators
          .instanceGen(CoreGenerators.RealtyEssentialsGen.suchThat(_.geoInfo.toSeq.flatMap(_.regionId).nonEmpty))
          .next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          include(""""s_region_id":[{"type":"#l"""")
    }
    "build doc for realty with empty region_id" in {
      val externalId = ExternalId("partner_1#1")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated =
        CoreGenerators
          .instanceGen(CoreGenerators.RealtyEssentialsGen.suchThat(_.geoInfo.toSeq.flatMap(_.regionId).isEmpty))
          .next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          include(s""""s_region_id":[{"type":"#l","value":"${saas.EmptyFieldMarker}"""")
    }
    "build doc for realty with non empty sublocality_id" in {
      val externalId = ExternalId("partner_1#1")
      val instanceId = CoreGenerators.instanceIdGen(externalId).next
      val generated =
        CoreGenerators
          .instanceGen(CoreGenerators.RealtyEssentialsGen.suchThat(_.geoInfo.toSeq.flatMap(_.sublocalityId).nonEmpty))
          .next
      val instance = generated.copy(id = instanceId)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps)
        updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps) should
          include(""""s_sublocality_id":{"type":"#l"""")
    }
    "build doc for realty with area square metres" in {
      val generated = CoreGenerators.instanceGen(CoreGenerators.RealtyEssentialsGen).next
      val instance =
        generated.copy(essentials =
          generated.essentials
            .asInstanceOf[RealtyEssentials]
            .copy(
              areaInfo = Some(AreaInfo(33.1f, AreaUnit.SQUARE_METER)),
              livingAreaInfo = Some(AreaInfo(1, AreaUnit.ARE)),
              kitchenAreaInfo = Some(AreaInfo(3, AreaUnit.HECTARE))
            )
        )

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        val rq = updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps)
        rq should include(""""i_area":{"type":"#i","value":"33"}""")
        rq should include(""""i_living_area":{"type":"#i","value":"100"}""")
        rq should include(""""i_kitchen_area":{"type":"#i","value":"30000"}""")
      }
    }

    "build doc with several signals" in {
      val generatedInstance = CoreGenerators.InstanceGen.next
      val service = Essentials.getService(generatedInstance.essentials)
      val generatedSignalWarn = CoreGenerators.warnSignalGen(service).next.copy(detailedReason = DetailedReason.Sold)
      val generatedSignalBan = CoreGenerators.banSignalGen(service).next.copy(detailedReason = DetailedReason.BadPhoto)
      val instance = generatedInstance.copy(signals = SignalSet(generatedSignalWarn, generatedSignalBan))

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        val rq = updateRequestBuilder.build(doc, DateTimeUtil.now(), kps = kps)

        rq should include(
          """"s_reason_and_signal_type":[{"type":"#l","value":"SOLD,WARN"},{"type":"#l","value":"BAD_PHOTO,BAN"}]"""
        )
        rq should include(""""s_signal_type":[{"type":"#l","value":"WARN"},{"type":"#l","value":"BAN"}]""")
        rq should include(""""s_reason":[{"type":"#l","value":"SOLD"},{"type":"#l","value":"BAD_PHOTO"}]""")
      }
    }

    "build doc with empty fields replaced with moderation empty marker" in {
      val essentials =
        CoreGenerators.RealtyEssentialsGen.next
          .copy(description = None) // zone field
          .copy(cadastralNumber = None) // search literal
      val instance = CoreGenerators.InstanceGen.next.copy(essentials = essentials)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        val saasRequest = updateRequestBuilder.build(doc, DateTimeUtil.now(), kps)
        saasRequest should include(s""""z_description":{"type":"#z","value":"${saas.EmptyFieldMarker}"""")
        saasRequest should include(s""""s_cadastral_number":{"type":"#l","value":"${saas.EmptyFieldMarker}"""")
      }
    }

    "build doc with empty fields that should not be replaced with moderation empty marker" in {
      val essentials =
        CoreGenerators.RealtyEssentialsGen.next
          .copy(floor = None) // search int
          .copy(price = None) // group
      val instance = CoreGenerators.InstanceGen.next.copy(essentials = essentials)

      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        val saasRequest = updateRequestBuilder.build(doc, DateTimeUtil.now(), kps)
        (saasRequest should not).include("\"i_floor\"")
        (saasRequest should not).include("\"i_price_sort\"")
      }
    }

    "build doc with empty array replaced with array with one element - moderation empty marker" in {
      val instance = CoreGenerators.InstanceGen.next.copy(signals = SignalSet.Empty)
      val doc = documentBuilder(instance.service).build(instance)

      for (kps <- saasOptions.saasPrefixes.streamSubmitKps) {
        val saasRequest = updateRequestBuilder.build(doc, DateTimeUtil.now(), kps)
        saasRequest should include(s""""s_reason":[{"type":"#l","value":"${saas.EmptyFieldMarker}"}]""")
        saasRequest should include(s""""s_signal_type":[{"type":"#l","value":"${saas.EmptyFieldMarker}"}]""")
        saasRequest should include(s""""s_reason_and_signal_type":[{"type":"#l","value":"${saas.EmptyFieldMarker}"}]""")
      }
    }

  }

  "AutoruFieldBuilder.calcUserQuota" should {

    case class UserQuotaTestCase(description: String,
                                 signals: SignalSet,
                                 offerCategory: Category,
                                 expectedResult: UserQuota
                                )

    import CoreGenerators._

    def generateSignal(gen: Gen[Signal],
                       domain: Model.Domain.UsersAutoru = Model.Domain.UsersAutoru.CARS,
                       isInherited: Boolean = true,
                       switchOffed: Boolean = false
                      ): Signal =
      gen.next
        .withDomain(Domain.UsersAutoru(domain))
        .withMarker(if (isInherited) InheritedSourceMarkerGen.next else NoMarker)
        .withSwitchOff(if (switchOffed) Some(SignalSwitchOffGen.next) else None)

    val testCases: Seq[UserQuotaTestCase] =
      Seq(
        UserQuotaTestCase(
          description = "no unquote signals",
          signals =
            SignalSet(
              generateSignal(WarnSignalGen.map(_.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))))
            ),
          offerCategory = Category.CARS,
          expectedResult = UserQuota.HasQuota
        ),
        UserQuotaTestCase(
          description = "all unquote signals has switch offs",
          signals =
            SignalSet(
              generateSignal(
                BanSignalGen.map(_.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))),
                switchOffed = true
              ),
              generateSignal(
                IndexErrorSignalGen.map(_.copy(detailedReasons = Set(DetailedReason.UserReseller(None, Seq.empty)))),
                switchOffed = true
              )
            ),
          offerCategory = Category.CARS,
          expectedResult = UserQuota.QuotaReturned
        ),
        UserQuotaTestCase(
          description = "one unquote signal has no switch off",
          signals =
            SignalSet(
              generateSignal(
                BanSignalGen.map(_.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))),
                switchOffed = true
              ),
              generateSignal(
                IndexErrorSignalGen.map(_.copy(detailedReasons = Set(DetailedReason.UserReseller(None, Seq.empty))))
              )
            ),
          offerCategory = Category.CARS,
          expectedResult = UserQuota.NoQuota
        ),
        UserQuotaTestCase(
          description = "wrong category",
          signals =
            SignalSet(
              generateSignal(BanSignalGen.map(_.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))))
            ),
          offerCategory = Category.MOTORCYCLE,
          expectedResult = UserQuota.HasQuota
        ),
        UserQuotaTestCase(
          description = "wrong reason",
          signals =
            SignalSet(
              generateSignal(BanSignalGen.map(_.copy(detailedReason = DetailedReason.UserBanned)))
            ),
          offerCategory = Category.CARS,
          expectedResult = UserQuota.HasQuota
        ),
        UserQuotaTestCase(
          description = "signal is not inherited",
          signals =
            SignalSet(
              generateSignal(
                BanSignalGen.map(_.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty))),
                isInherited = false
              )
            ),
          offerCategory = Category.CARS,
          expectedResult = UserQuota.HasQuota
        )
      )

    testCases.foreach { case UserQuotaTestCase(description, signals, offerCategory, expectedResult) =>
      description in {
        val actualResult = AutoruFieldBuilder.calcUserQuota(signals, offerCategory)
        actualResult shouldBe expectedResult
      }
    }
  }
}

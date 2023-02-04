package ru.yandex.vertis.moderation.rule.view

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.meta.{OffersSummary, SignalsSummary}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType
import ru.yandex.vertis.moderation.proto.Model.{Reason, Service, Visibility}
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.OfferType
import ru.yandex.vertis.moderation.searcher.core.saas.search._
import ru.yandex.vertis.moderation.searcher.core.saas.search.metadata._
import ru.yandex.vertis.moderation.searcher.core.util.Range
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * Spec for [[SearchQueryParser]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class SearchQueryParserSpec extends SpecBase {

  "SearchQueryParser" should {
    "parse realty search query with unknown attributes correctly" in {

      val searchAttributes =
        Map(
          "opinion" -> "failed",
          "cluster_head" -> "true",
          "min_create_time" -> "0",
          "object_id" -> "0,1,2",
          "qwerty" -> "qwerty",
          "min_description_length" -> "1",
          "max_description_length" -> "2"
        )

      val expected =
        RealtySearchQuery(
          opinion = Seq("failed"),
          clusterHead = Some(true),
          createDates = Some(Range(min = Some(DateTimeUtil.fromMillis(0L)), max = None)),
          objectId = Seq("0", "1", "2"),
          descriptionLength = Some(Range(min = Some(1), max = Some(2)))
        )

      SearchQueryParser(searchAttributes, Service.REALTY) should smartEqual(expected)
    }

    "parse autoru search query with mark_model_nameplate correctly" in {

      val searchAttributes =
        Map(
          "opinion" -> "failed",
          "min_create_time" -> "0",
          "object_id" -> "0,1,2",
          "mark_model_nameplate" -> "audi:a1:21428669,toyota:corolla,bmw,there:are:too:many:fields"
        )

      val expected =
        AutoruSearchQuery(
          opinion = Seq("failed"),
          createDates = Some(Range(min = Some(DateTimeUtil.fromMillis(0L)), max = None)),
          objectId = Seq("0", "1", "2"),
          modelSearchQuery =
            Seq(
              ModelSearchQuery(mark = "audi", model = Some("a1"), superGen = Some("21428669")),
              ModelSearchQuery(mark = "toyota", model = Some("corolla"), superGen = None),
              ModelSearchQuery(mark = "bmw", model = None, superGen = None)
            )
        )
      SearchQueryParser(searchAttributes, Service.AUTORU) should smartEqual(expected)
    }

    "parse users_autoru metadata search query correctly" in {
      val searchAttributes =
        Map(
          "min_meta_signals_BAN_AD_ON_PHOTO_count" -> "10",
          "max_meta_signals_INDEXER_ANOTHER_max_time" -> "100",
          "meta_signals_maybe_inaccurate" -> "false",
          "meta_signals_existence" -> "BAN_AD_ON_PHOTO,INDEXER_ANOTHER",
          "min_meta_offers_BLOCKED_CARS_count" -> "10",
          "max_meta_offers_BLOCKED_CARS_count" -> "11",
          "min_meta_geo_info_123_count" -> "1",
          "meta_offers_maybe_inaccurate" -> "true",
          "meta_geobase_ip_vpn" -> "true",
          "meta_geobase_ip_proxy" -> "false",
          "meta_geobase_ip_hosting" -> "true",
          "meta_geobase_ip_tor" -> "false",
          "meta_geobase_ip_yandex_turbo" -> "true",
          "meta_geobase_ip_yandex_staff" -> "false",
          "meta_geobase_ip_yandex_net" -> "true",
          "min_meta_phones_ownership" -> "0",
          "max_meta_phones_ownership" -> "2",
          "max_meta_phones_communication" -> "3",
          "meta_yandex_money_phones_has_wallet" -> "true",
          "meta_yandex_money_phones_has_transactions" -> "true",
          "meta_yandex_money_phones_in_black_list" -> "true",
          "meta_techsupport_photo_type" -> "licence_plate,quota",
          "meta_is_quota" -> "false",
          "meta_is_returned_over_quota" -> "true"
        )

      val banAdOnPhotoQualifier = SignalsSummary.AutoruQualifier(SignalType.BAN, Reason.AD_ON_PHOTO)
      val indexerAnotherQualifier = SignalsSummary.AutoruQualifier(SignalType.INDEXER, Reason.ANOTHER)

      val expected =
        UserAutoruSearchQuery(
          metadata =
            MetadataSearchQuery(
              signalsMetadataSearchQuery =
                SignalsMetadataSearchQuery(
                  count = Map(banAdOnPhotoQualifier -> Range(Some(10), None)),
                  minTime = Map.empty,
                  maxTime = Map(indexerAnotherQualifier -> Range(None, Some(DateTimeUtil.fromMillis(100)))),
                  maybeInaccurate = Some(false),
                  hasSignalsWithQualifiers = Seq(banAdOnPhotoQualifier, indexerAnotherQualifier)
                ),
              offersMetadataSearchQuery =
                OffersMetadataSearchQuery(
                  count =
                    Map(OffersSummary.AutoruQualifier(Visibility.BLOCKED, Category.CARS) -> Range(Some(10), Some(11))),
                  maybeInaccurate = Some(true)
                ),
              geobaseIpSearchQuery =
                GeobaseIpSearchQuery(
                  isVpn = Some(true),
                  isProxy = Some(false),
                  isHosting = Some(true),
                  isTor = Some(false),
                  isYandexTurbo = Some(true),
                  isYandexStaff = Some(false),
                  isYandexNet = Some(true)
                ),
              phonesMetadataSearchQuery =
                PhonesMetadataSearchQuery(
                  ownershipFactor = Some(Range(Some(0), Some(2))),
                  communicationFactor = Some(Range(None, Some(3)))
                ),
              yandexMoneyPhonesMetadataSearchQuery =
                YandexMoneyPhonesMetadataSearchQuery(
                  hasWallet = Some(true),
                  hasTransactions = Some(true),
                  phoneNumberInBlackList = Some(true)
                ),
              techsupportPhotosMetadataSearchQuery =
                TechsupportPhotosMetadataSearchQuery(
                  photoTypes = Seq("licence_plate", "quota")
                ),
              geoInfoMetadataSearchQuery =
                GeoInfoMetadataSearchQuery(
                  count = Map(123 -> Range(Some(1), None)),
                  minTime = Map.empty,
                  maxTime = Map.empty,
                  maybeInaccurate = None
                ),
              quotaMetadataSearchQuery =
                QuotaMetadataSearchQuery(
                  isQuota = Some(false),
                  isReturnedQuota = None,
                  isOverQuota = None,
                  isReturnedOverQuota = Some(true)
                ),
              vinHistoryMetadataSearchQuery =
                VinHistoryMetadataSearchQuery(
                  previousVin = None
                )
            )
        )

      SearchQueryParser(searchAttributes, Service.USERS_AUTORU) should smartEqual(expected)
    }

    "parse users_realty metadata search query correctly" in {
      val searchAttributes =
        Map(
          "min_meta_signals_WARN_AD_ON_PHOTO_SELL_VOS_count" -> "10",
          "max_meta_signals_HOBO_ANOTHER_RENT_XML_max_time" -> "100",
          "meta_signals_maybe_inaccurate" -> "false",
          "meta_signals_existence" -> "WARN_AD_ON_PHOTO_SELL_VOS,HOBO_ANOTHER_RENT_XML",
          "min_meta_offers_BLOCKED_SELL_VOS_count" -> "10",
          "max_meta_offers_BLOCKED_SELL_VOS_count" -> "11",
          "min_meta_geo_info_123_count" -> "1",
          "meta_offers_maybe_inaccurate" -> "true",
          "meta_geobase_ip_vpn" -> "true",
          "meta_geobase_ip_proxy" -> "false",
          "meta_geobase_ip_hosting" -> "true",
          "meta_geobase_ip_tor" -> "false",
          "meta_geobase_ip_yandex_turbo" -> "true",
          "meta_geobase_ip_yandex_staff" -> "false",
          "meta_geobase_ip_yandex_net" -> "true",
          "min_meta_phones_ownership" -> "0",
          "max_meta_phones_ownership" -> "2",
          "max_meta_phones_communication" -> "3",
          "meta_yandex_money_phones_has_wallet" -> "false",
          "meta_yandex_money_phones_has_transactions" -> "false",
          "meta_yandex_money_phones_in_black_list" -> "false"
        )

      val warnAdOnPhotoSellVosQualifier =
        SignalsSummary.RealtyQualifier(SignalType.WARN, Reason.AD_ON_PHOTO, OfferType.SELL, isVos = true)
      val hoboAnotherRentNotVosQualifier =
        SignalsSummary.RealtyQualifier(SignalType.HOBO, Reason.ANOTHER, OfferType.RENT, isVos = false)

      val expected =
        UserRealtySearchQuery(
          metadata =
            MetadataSearchQuery(
              signalsMetadataSearchQuery =
                SignalsMetadataSearchQuery(
                  count = Map(warnAdOnPhotoSellVosQualifier -> Range(Some(10), None)),
                  minTime = Map.empty,
                  maxTime = Map(hoboAnotherRentNotVosQualifier -> Range(None, Some(DateTimeUtil.fromMillis(100)))),
                  maybeInaccurate = Some(false),
                  hasSignalsWithQualifiers = Seq(warnAdOnPhotoSellVosQualifier, hoboAnotherRentNotVosQualifier)
                ),
              offersMetadataSearchQuery =
                OffersMetadataSearchQuery(
                  count =
                    Map(
                      OffersSummary.RealtyQualifier(Visibility.BLOCKED, OfferType.SELL, isVos = true) ->
                        Range(Some(10), Some(11))
                    ),
                  maybeInaccurate = Some(true)
                ),
              geobaseIpSearchQuery =
                GeobaseIpSearchQuery(
                  isVpn = Some(true),
                  isProxy = Some(false),
                  isHosting = Some(true),
                  isTor = Some(false),
                  isYandexTurbo = Some(true),
                  isYandexStaff = Some(false),
                  isYandexNet = Some(true)
                ),
              phonesMetadataSearchQuery =
                PhonesMetadataSearchQuery(
                  ownershipFactor = Some(Range(Some(0), Some(2))),
                  communicationFactor = Some(Range(None, Some(3)))
                ),
              yandexMoneyPhonesMetadataSearchQuery =
                YandexMoneyPhonesMetadataSearchQuery(
                  hasWallet = Some(false),
                  hasTransactions = Some(false),
                  phoneNumberInBlackList = Some(false)
                ),
              techsupportPhotosMetadataSearchQuery = TechsupportPhotosMetadataSearchQuery.Empty,
              geoInfoMetadataSearchQuery =
                GeoInfoMetadataSearchQuery(
                  count = Map(123 -> Range(Some(1), None)),
                  minTime = Map.empty,
                  maxTime = Map.empty,
                  maybeInaccurate = None
                ),
              quotaMetadataSearchQuery = QuotaMetadataSearchQuery.Empty,
              vinHistoryMetadataSearchQuery = VinHistoryMetadataSearchQuery.Empty
            )
        )

      SearchQueryParser(searchAttributes, Service.USERS_REALTY) should smartEqual(expected)
    }

    "fail on unknown attributes if failOnUnknown=true" in {
      val searchAttributes =
        Map(
          "qwerty" -> "qwerty"
        )
      intercept[IllegalArgumentException] {
        SearchQueryParser(searchAttributes, Service.REALTY, failOnUnknown = true)
      }
    }

    "parse attributes by regex" in {
      val searchAttributes =
        Map(
          "min_meta_geo_info_123_count" -> "1",
          "max_meta_geo_info_123_count" -> "2",
          "min_meta_geo_info_321_count" -> "10",
          "max_meta_geo_info_321_count" -> "20",
          "min_meta_geo_info_999_count" -> "50",
          "min_meta_geo_info_999_min_time" -> "100",
          "max_meta_geo_info_999_min_time" -> "200"
        )

      val metaQuery =
        MetadataSearchQuery.Empty.copy(
          geoInfoMetadataSearchQuery =
            GeoInfoMetadataSearchQuery(
              count =
                Map(
                  123 -> Range(Some(1), Some(2)),
                  321 -> Range(Some(10), Some(20)),
                  999 -> Range(Some(50), None)
                ),
              minTime =
                Map(
                  999 -> Range(Some(DateTimeUtil.fromMillis(100)), Some(DateTimeUtil.fromMillis(200)))
                ),
              maxTime = Map.empty,
              maybeInaccurate = None
            )
        )
      val expected =
        RealtySearchQuery(
          metadata = metaQuery
        )

      SearchQueryParser(searchAttributes, Service.REALTY) should smartEqual(expected)
    }
  }

}

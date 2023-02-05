// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.yandex.direct.domain.RegionInfo
import ru.yandex.direct.domain.ShortCampaignInfo
import ru.yandex.direct.domain.banners.BannerGroup
import ru.yandex.direct.domain.banners.CoverageInfo
import ru.yandex.direct.domain.banners.ShortBannerInfo
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.domain.enums.Currency
import ru.yandex.direct.domain.statistics.Grouping
import ru.yandex.direct.domain.statistics.Metrics
import ru.yandex.direct.domain.statistics.Section
import ru.yandex.direct.loaders.impl.statistic.FullReportSettings
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.web.api5.ads.AdGetItem
import ru.yandex.direct.web.api5.bids.AuctionBid
import ru.yandex.direct.web.api5.campaign.CampaignGetItem
import ru.yandex.direct.web.api5.clients.ClientGetItem
import ru.yandex.direct.web.api5.clients.ClientResult
import ru.yandex.direct.web.api5.dictionary.CurrencyItem
import ru.yandex.direct.web.api5.dictionary.GeoRegionItem
import ru.yandex.direct.web.api5.result.BaseResult
import ru.yandex.direct.web.api5.result.CheckDictionariesResult
import ru.yandex.direct.web.report.request.DateRangeType

class ApiSampleData private constructor() {
    companion object {
        private val gson = Gson()

        val clientInfo = deserialize("client-info", ClientInfo::class.java)

        val subclientInfo = deserialize("client-info-subclient", ClientInfo::class.java)

        val clientGetItem = deserialize("client-get-item", ClientGetItem::class.java)

        val dictionaryResultEmpty = deserialize("changes-empty", CheckDictionariesResult::class.java)

        val dictionaryResultAllYes = deserialize("changes-yes-all", CheckDictionariesResult::class.java)

        val dictionaryResultAllNo = deserialize("changes-no-all", CheckDictionariesResult::class.java)

        val currencyItems = deserialize("currency", object : TypeToken<List<CurrencyItem>>() {})

        val regionItems = deserialize("regions", object : TypeToken<List<GeoRegionItem>>() {})

        val campaigns = deserialize("campaigns", object : TypeToken<List<CampaignGetItem>>() {})
            .map { ShortCampaignInfo(it) }

        val clientsResponse = BaseResult(ClientResult(listOf(clientGetItem)))

        val currency: List<Currency> = currencyItems.map(Currency::fromApi5)

        val regions: List<RegionInfo> = regionItems.map(RegionInfo::fromApi5)

        val bannerInfo = ShortBannerInfo(deserialize("banner-info", AdGetItem::class.java))

        val bannerGroup = deserialize("banner-group", BannerGroup::class.java).apply {
            banners = listOf(bannerInfo)
        }

        val campaignInfo = deserialize("campaign-info", ShortCampaignInfo::class.java)

        val reportByDay = getApiData("report-by-day.tsv")

        val reportByDaySettings = FullReportSettings.Builder()
                .setMetrics(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS, Metrics.COST))
                .setDateRange(DateRange.fromPreset(DateRangeType.LAST_7_DAYS))
                .setReportTargetInfo(ReportTargetInfo.overall())
                .setGrouping(Grouping.DATE)
                .setSection(Section.MOBILE_PLATFORM)
                .setIncludeVat(true)
                .build()

        val reportOverall = getApiData("report-overall.tsv")

        val reportOverallSettings = FullReportSettings.Builder()
                .setMetrics(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS, Metrics.COST))
                .setDateRange(DateRange.fromPreset(DateRangeType.LAST_7_DAYS))
                .setReportTargetInfo(ReportTargetInfo.overall())
                .setSection(Section.MOBILE_PLATFORM)
                .setGrouping(Grouping.SELECTED_RANGE)
                .setIncludeVat(true)
                .build()

        val auctionBidList = deserialize("auction-bid", object : TypeToken<List<AuctionBid>>() {})

        val coverageInfoList = deserialize("coverage-info", object : TypeToken<List<CoverageInfo>>() {})

        fun <T> errorFor(): BaseResult<T> {
            val type = object : TypeToken<BaseResult<T>>() {}.type
            return gson.fromJson<BaseResult<T>>(getJsonApiData("error"), type)!!
        }

        private fun <T> deserialize(filename: String, type: Class<T>): T {
            return gson.fromJson(getJsonApiData(filename), type)!!
        }

        private fun <T> deserialize(filename: String, typeToken: TypeToken<T>): T {
            return gson.fromJson<T>(getJsonApiData(filename), typeToken.type)!!
        }

        private fun getJsonApiData(filename: String) = getApiData("$filename.json")

        private fun getApiData(fileName: String): String {
            return ApiSampleData::class.java
                    .getResource(fileName)
                    .readText()
        }
    }
}
// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import retrofit2.Call
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.api5.adgroups.AdGroupGetItem
import ru.yandex.direct.web.api5.adimages.AdImageGetItem
import ru.yandex.direct.web.api5.ads.AdGetItem
import ru.yandex.direct.web.api5.audiencetargets.AudienceTargetGetItem
import ru.yandex.direct.web.api5.audiencetargets.AudienceTargetSetBidsItem
import ru.yandex.direct.web.api5.audiencetargets.SetBids
import ru.yandex.direct.web.api5.bidmodifiers.BidModifierGetItem
import ru.yandex.direct.web.api5.bids.KeywordBidGetItem
import ru.yandex.direct.web.api5.campaign.CampaignGetItem
import ru.yandex.direct.web.api5.clients.AgencyClientsResult
import ru.yandex.direct.web.api5.clients.ClientResult
import ru.yandex.direct.web.api5.creatives.CreativeGetItem
import ru.yandex.direct.web.api5.dictionary.DictionariesGetItem
import ru.yandex.direct.web.api5.keywords.KeywordGetItem
import ru.yandex.direct.web.api5.request.AdGroupsGetParams
import ru.yandex.direct.web.api5.request.AdImageGetParams
import ru.yandex.direct.web.api5.request.AdsGetParams
import ru.yandex.direct.web.api5.request.AgencyClientsGetParams
import ru.yandex.direct.web.api5.request.AudienceTargetsGetParams
import ru.yandex.direct.web.api5.request.BaseAction
import ru.yandex.direct.web.api5.request.BaseAdd
import ru.yandex.direct.web.api5.request.BaseDelete
import ru.yandex.direct.web.api5.request.BaseGet
import ru.yandex.direct.web.api5.request.BaseSet
import ru.yandex.direct.web.api5.request.BaseToggle
import ru.yandex.direct.web.api5.request.BaseUpdate
import ru.yandex.direct.web.api5.request.BidModifiersAddParams
import ru.yandex.direct.web.api5.request.BidModifiersDeleteParams
import ru.yandex.direct.web.api5.request.BidModifiersGetParams
import ru.yandex.direct.web.api5.request.BidModifiersSetParams
import ru.yandex.direct.web.api5.request.BidModifiersToggleParams
import ru.yandex.direct.web.api5.request.BidsSet
import ru.yandex.direct.web.api5.request.BidsSetAuto
import ru.yandex.direct.web.api5.request.CampaignGetParams
import ru.yandex.direct.web.api5.request.CampaignUpdateParams
import ru.yandex.direct.web.api5.request.CheckDictionaries
import ru.yandex.direct.web.api5.request.ClientGetParams
import ru.yandex.direct.web.api5.request.CreativeGetParams
import ru.yandex.direct.web.api5.request.DictionariesGetParams
import ru.yandex.direct.web.api5.request.KeywordBidsGetParams
import ru.yandex.direct.web.api5.request.KeywordsAddParams
import ru.yandex.direct.web.api5.request.KeywordsGetParams
import ru.yandex.direct.web.api5.request.KeywordsUpdateParams
import ru.yandex.direct.web.api5.request.RetargetingListsGetParams
import ru.yandex.direct.web.api5.request.VCardGetParams
import ru.yandex.direct.web.api5.result.ActionResult
import ru.yandex.direct.web.api5.result.BaseArrayResult
import ru.yandex.direct.web.api5.result.BaseResult
import ru.yandex.direct.web.api5.result.BidModifierToggleResult
import ru.yandex.direct.web.api5.result.BidsSetResult
import ru.yandex.direct.web.api5.result.CheckDictionariesResult
import ru.yandex.direct.web.api5.result.MultiIdsActionResult
import ru.yandex.direct.web.api5.retargetinglists.RetargetingListGetItem
import ru.yandex.direct.web.api5.vcard.VCardGetItem

open class MockedDirectApi5 : IDirectApi5 {
    protected val delegate = DelegateFactory.create(IDirectApi5::class.java)

    override fun setBids(body: SetBids<AudienceTargetSetBidsItem>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun getRetargetingLists(body: BaseGet<RetargetingListsGetParams>): Call<BaseArrayResult<RetargetingListGetItem>> {
        TODO("not implemented")
    }

    override fun getAudienceTargets(body: BaseGet<AudienceTargetsGetParams>): Call<BaseArrayResult<AudienceTargetGetItem>> {
        TODO("not implemented")
    }

    override fun getCampaigns(body: BaseGet<CampaignGetParams>): Call<BaseArrayResult<CampaignGetItem>> {
        TODO("not implemented")
    }

    override fun getAds(body: BaseGet<AdsGetParams>): Call<BaseArrayResult<AdGetItem>> {
        TODO("not implemented")
    }

    override fun getVCards(body: BaseGet<VCardGetParams>): Call<BaseArrayResult<VCardGetItem>> {
        TODO("not implemented")
    }

    override fun getAdGroups(body: BaseGet<AdGroupsGetParams>): Call<BaseArrayResult<AdGroupGetItem>> {
        TODO("not implemented")
    }

    override fun action(endpoint: String, body: BaseAction): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun checkDictionaries(body: CheckDictionaries): Call<BaseResult<CheckDictionariesResult>> {
        TODO("not implemented")
    }

    override fun getKeywords(body: BaseGet<KeywordsGetParams>): Call<BaseArrayResult<KeywordGetItem>> {
        TODO("not implemented")
    }

    override fun updateKeywords(body: BaseUpdate<KeywordsUpdateParams>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun addKeywords(body: BaseAdd<KeywordsAddParams>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun getBids(body: BaseGet<KeywordBidsGetParams>): Call<BaseArrayResult<KeywordBidGetItem>> {
        TODO("not implemented")
    }

    override fun setBids(body: BidsSet): Call<BaseArrayResult<BidsSetResult>> {
        TODO("not implemented")
    }

    override fun setBidsAuto(body: BidsSetAuto): Call<BaseArrayResult<BidsSetResult>> {
        TODO("not implemented")
    }

    override fun deleteBidModifiers(body: BaseDelete<BidModifiersDeleteParams>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun getBidModifiers(body: BaseGet<BidModifiersGetParams>): Call<BaseArrayResult<BidModifierGetItem>> {
        TODO("not implemented")
    }

    override fun setBidModifiers(body: BaseSet<BidModifiersSetParams>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun toggleBidModifiers(body: BaseToggle<BidModifiersToggleParams>): Call<BaseArrayResult<BidModifierToggleResult>> {
        TODO("not implemented")
    }

    override fun addBidModifiers(body: BaseAdd<BidModifiersAddParams>): Call<BaseArrayResult<MultiIdsActionResult>> {
        TODO("not implemented")
    }

    override fun getDictionaries(body: BaseGet<DictionariesGetParams>): Call<BaseResult<DictionariesGetItem>> {
        TODO("not implemented")
    }

    override fun getAgencyClients(body: BaseGet<AgencyClientsGetParams>): Call<BaseResult<AgencyClientsResult>> {
        TODO("not implemented")
    }

    override fun getClient(body: BaseGet<ClientGetParams>): Call<BaseResult<ClientResult>> {
        TODO("not implemented")
    }

    override fun updateCampaigns(body: BaseUpdate<CampaignUpdateParams>): Call<BaseArrayResult<ActionResult>> {
        TODO("not implemented")
    }

    override fun getCreatives(body: BaseGet<CreativeGetParams>): Call<BaseArrayResult<CreativeGetItem>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAdImages(body: BaseGet<AdImageGetParams>): Call<BaseArrayResult<AdImageGetItem>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.groups

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.db.group.BannerGroupDao
import ru.yandex.direct.domain.banners.BannerGroup

class BannerGroupsQueryTest {
    private lateinit var mDao: BannerGroupDao

    @Before
    fun runBeforeEachTest() {
        mDao = mock()
    }

    @Test
    fun emptySearchQuery_returnsEmptyList_ifHasNoData() {
        loadDaoWithData(emptyList())
        assertThat(BannerGroupsQuery.ofCampaignGroups(0).select(mDao)).isEmpty()
        assertThat(BannerGroupsQuery.ofCampaignGroups(0, "").select(mDao)).isEmpty()
    }

    @Test
    fun emptySearchQuery_returnsData_ifHasAny() {
        val data = listOf(ApiSampleData.bannerGroup)
        loadDaoWithData(data)
        assertThat(BannerGroupsQuery.ofCampaignGroups(0).select(mDao)).isEqualTo(data)
        assertThat(BannerGroupsQuery.ofCampaignGroups(0, "").select(mDao)).isEqualTo(data)
    }

    @Test
    fun nonEmptySearchQuery_returnsEmptyList_ifHasNoData() {
        loadDaoWithData(emptyList())
        assertThat(BannerGroupsQuery.ofCampaignGroups(0, "search query").select(mDao)).isEmpty()
    }

    @Test
    fun select_worksWithSearchQuery() {
        loadDaoWithData(emptyList())
        BannerGroupsQuery.ofCampaignGroups(0, "search query").select(mDao)
        verify(mDao).selectByCampaignId(0, "search query")
    }

    @Test
    fun toDirectApiQuery_worksCorrectly() {
        val apiQuery = BannerGroupsQuery.ofCampaignGroups(0, "search query").toDirectApiQuery()
        assertThat(apiQuery.selectionCriteria.campaignIds).isEqualTo(listOf(0L))
    }

    @Test
    fun toDirectApiQuery_ignoresSearchQuery() {
        assertThat(
                BannerGroupsQuery.ofCampaignGroups(0, "1").toDirectApiQuery()
        ).isEqualToComparingFieldByFieldRecursively(
                BannerGroupsQuery.ofCampaignGroups(0, "0").toDirectApiQuery()
        )
    }

    private fun loadDaoWithData(data: List<BannerGroup>) {
        mDao.stub {
            on { selectByCampaignId(anyLong(), anyString()) } doReturn data
        }
    }
}
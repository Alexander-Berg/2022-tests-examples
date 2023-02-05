// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.banners

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BannersQueryTest {
    @Test
    fun toApiParams_worksCorrectly() {
        assertThat(BannersQuery.ofAllCampaignBanners(0, null).toApiParams().selectionCriteria.campaignIds)
                .isEqualTo(listOf(0L))
    }
}
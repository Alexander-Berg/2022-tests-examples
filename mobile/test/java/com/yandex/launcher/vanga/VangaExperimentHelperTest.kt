package com.yandex.launcher.vanga

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.loaders.experiments.Experiment
import com.yandex.launcher.loaders.experiments.ExperimentManager
import com.yandex.launcher.statistics.ItemAnalyticsInfo.MAIN_CONTAINER_SHTORKA
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_RECENTLY
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_SEARCH
import com.yandex.launcher.statistics.ItemAnalyticsInfo.MAIN_CONTAINER_ALLAPPS
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_COLOR
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_CATEGORY
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_HEADER
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_LIST
import com.yandex.launcher.statistics.ItemAnalyticsInfo.MAIN_CONTAINER_HOMESCREENS
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_SCREEN
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_HOTSEAT
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_SIMPLE_FOLDER
import com.yandex.launcher.statistics.ItemAnalyticsInfo.PART_FULLSCREEN_FOLDER
import com.yandex.launcher.statistics.ItemAnalyticsInfo.MAIN_CONTAINER_ALICE
import com.natpryce.hamkrest.assertion.assertThat
import com.yandex.launcher.loaders.experiments.Experiments.VANGA_PLACES_BLACKLIST
import org.junit.Test

import org.mockito.Mockito
import org.mockito.kotlin.mock

class VangaExperimentHelperTest : BaseRobolectricTest() {

    private val experimentManager = mock<ExperimentManager>()

    private val helper = VangaExperimentHelper(experimentManager)

    @Test
    fun `should fallback to default black list`() {
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_SCREEN), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_SHTORKA, PART_SEARCH), equalTo(false))

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), equalTo(true))
    }

    @Test
    fun `should put all workspace containers to black list`() {
        setupExperimentManager(listOf(VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS)).toJson())

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_HEADER), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_CATEGORY), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_COLOR), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_SIMPLE_FOLDER), equalTo(true))

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_HEADER), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_LIST), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALICE, null), equalTo(false))
    }

    @Test
    fun `should put allapps header and hotset into black list`() {
        setupExperimentManager(
            listOf(
                VangaBlackListRule(MAIN_CONTAINER_ALLAPPS, PART_HEADER),
                VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT)
            ).toJson()
        )

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_HEADER), equalTo(true))

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_SCREEN), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_FULLSCREEN_FOLDER), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_COLOR), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALICE, null), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_SHTORKA, PART_RECENTLY), equalTo(false))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_SHTORKA, PART_SEARCH), equalTo(false))
    }

    @Test
    fun `should put all containers to black list`() {
        setupExperimentManager(
            listOf(
                VangaBlackListRule(MAIN_CONTAINER_ALLAPPS),
                VangaBlackListRule(MAIN_CONTAINER_ALICE),
                VangaBlackListRule(MAIN_CONTAINER_SHTORKA),
                VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS)
            ).toJson()
        )

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_HEADER), equalTo(true))

        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_SCREEN), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_HOMESCREENS, PART_FULLSCREEN_FOLDER), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALLAPPS, PART_COLOR), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_ALICE, null), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_SHTORKA, PART_RECENTLY), equalTo(true))
        assertThat(helper.isPlaceInBlackList(MAIN_CONTAINER_SHTORKA, PART_SEARCH), equalTo(true))
    }

    @Test
    fun `should be able to parse valid black list`() {
        val correctBlackList = listOf(
            VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT),
            VangaBlackListRule(MAIN_CONTAINER_ALICE)
        )

        assertThat(helper.parsePlacesBlackList(getExperiment(correctBlackList.toJson())), equalTo(correctBlackList))

        val correctBlackList2 = listOf(
            VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT),
            VangaBlackListRule(MAIN_CONTAINER_ALICE),
            VangaBlackListRule(MAIN_CONTAINER_ALLAPPS, PART_COLOR)
        )

        assertThat(helper.parsePlacesBlackList(getExperiment(correctBlackList2.toJson())), equalTo(correctBlackList2))

        assertThat(helper.parsePlacesBlackList(getExperiment("[]")) as Collection<Any>, isEmpty)
    }

    @Test
    fun `should fail on parsing invalid black list`() {

        assertThat(helper.parsePlacesBlackList(getExperiment("[xxxx]")), absent())

        assertThat(
            helper.parsePlacesBlackList(
                getExperiment(
                    listOf(VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), listOf("Unknown")).toJson()
                )
            ),
            absent()
        )

        assertThat(
            helper.parsePlacesBlackList(
                getExperiment(listOf(listOf(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), MAIN_CONTAINER_ALICE).toJson())
            ),
            absent()
        )

        assertThat(
            helper.parsePlacesBlackList(
                getExperiment(
                    listOf(VangaBlackListRule(MAIN_CONTAINER_HOMESCREENS, PART_HOTSEAT), MAIN_CONTAINER_ALICE).toJson()
                )
            ),
            absent()
        )
    }

    private fun setupExperimentManager(experimentContent: String) {
        Mockito.doReturn(
            getExperiment(experimentContent)
        ).`when`(experimentManager).getActualLocalizedExperiment(VANGA_PLACES_BLACKLIST)
    }


    private fun getExperiment(value: String) = Experiment("", 0, value, false, false)
}

private val gson =  GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
    .create()

private fun Any.toJson() = gson.toJson(this)

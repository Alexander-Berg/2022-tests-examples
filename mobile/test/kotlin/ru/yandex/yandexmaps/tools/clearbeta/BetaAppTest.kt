package ru.yandex.yandexmaps.tools.clearbeta

import kotlin.test.Test
import kotlin.test.assertEquals

internal class BetaAppTest {

    @Test
    fun isPreservedBranchForMaps() {
        testIsPreservedBranch(
            ClearBetaBuilds.mapsApp,
            listOf(
                "releases_9.3.2" to true,
                "tags_7.1" to true,
                "trunk" to false,
            )
        )
    }

    @Test
    fun isPreservedBranchForNavi() {
        testIsPreservedBranch(
            ClearBetaBuilds.naviApp,
            listOf(
                "release-6.55" to true,
                "release-6.55-stesting" to true,
                "release_6.55-store" to true,
                "release_6.55-stesting" to true,
                "release" to false,
                "dev" to false,
            )
        )
    }

    private fun testIsPreservedBranch(app: BetaApp, tests: List<Pair<String, Boolean>>) {
        for (test in tests) {
            val branch = test.first
            val expected = test.second
            val result = app.isPreservedBranch(branch)
            assertEquals(expected, result, "Failed test for app=${app.name}, branch='$branch'")
        }
    }

    @Test
    fun isPrBranch() {
        val app = ClearBetaBuilds.mapsPrs

        val tests = listOf(
            "1111" to 1111,
            "1234-navi-sample" to 1234,
            "" to null,
            "pr-1111" to 1111,
        )

        for (test in tests) {
            val branch = test.first
            val expected = test.second
            val result = app.isPrBranch(branch)

            assertEquals(expected, result, "Failed test for branch '$branch'")
        }
    }
}

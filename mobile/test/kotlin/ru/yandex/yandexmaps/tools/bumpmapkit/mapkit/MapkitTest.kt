package ru.yandex.yandexmaps.tools.bumpmapkit.mapkit

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MapkitTest {
    @Test
    fun getReleaseBranchUrl() {
        val tests = arrayOf(
            "2022012718.9100982" to "https://a.yandex-team.ru/arc/history/branches/maps-mobile-releases/2022012718",
            "2022031023.8-7f81f6ce2-1.6" to "https://a.yandex-team.ru/arc_vcs/history/?peg=releases%2Fmaps%2Fmobile%2Fmapkit-release-1",
        )

        for (test in tests) {
            val version = test.first
            val expected = test.second
            assertEquals(expected, Mapkit.getReleaseBranchUrl(MapkitVersion.parse(version)))
        }
    }

    @Test
    fun getReleaseBranch() {
        val tests = arrayOf(
            "2022012718.9100982" to "maps-mobile-releases/2022012718",
            "2022031023.8-7f81f6ce2-1.6" to "releases/maps/mobile/mapkit-release-1",
        )

        for (test in tests) {
            val version = test.first
            val expected = test.second
            assertEquals(expected, Mapkit.getReleaseBranch(MapkitVersion.parse(version)))
        }
    }

    @Test
    fun getReleaseCiUrl() {
        val tests = arrayOf(
            "2022012718.9100982" to null,
            "2022031023.8-7f81f6ce2-1.6" to "https://a.yandex-team.ru/projects/maps-core-mobile-arcadia-ci/ci/releases/timeline?branch=releases%2Fmaps%2Fmobile%2Fmapkit-release-1&dir=maps%2Fmobile&id=release-all",
        )

        for (test in tests) {
            val version = test.first
            val expected = test.second
            assertEquals(expected, Mapkit.getReleaseCiUrl(MapkitVersion.parse(version)))
        }
    }
}

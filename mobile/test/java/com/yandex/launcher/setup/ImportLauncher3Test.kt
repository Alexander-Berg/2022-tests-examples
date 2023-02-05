package com.yandex.launcher.setup

import com.android.launcher3.LauncherSettings
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test

class ImportLauncher3Test : BaseRobolectricTest() {

    @Test
    fun `screen values are missed for dock items, restored from cellX`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    cellX = index
                }
        }

        import.fixHotseat(items)

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
        }
    }

    @Test
    fun `cellX values are missed for dock items, restored from screen`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = index
                }
        }

        import.fixHotseat(items)

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
            assertThat(item.cellX, equalTo(index))
        }
    }

    @Test
    fun `screen values are 0 for dock items, restored from cellX`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = 0
                    cellX = index
                }
        }

        import.fixHotseat(items)

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
        }
    }

    @Test
    fun `first dock item has wrong cellX, screen value remain the same, cellX restored from screen`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = index
                    cellX = index
                }
        }

        items[0].cellX = 1 // set 1 instead of 0

        import.fixHotseat(items)

         assertThat(items[0].screen, equalTo(0))
         assertThat(items[0].cellX, equalTo(0))
    }

    @Test
    fun `cellY value missed for dock items, restored with 0`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = index
                    cellX = index
                }
        }

        import.fixHotseat(items)

        items.forEach { item ->
            assertThat(item.cellY, equalTo(0))
        }
    }

    @Test
    fun `two dock items placed at second position, they're separated`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = List(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = index
                    cellX = index
                }
        }

        items[0].screen = 1

        import.fixHotseat(items)

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
            assertThat(item.cellX, equalTo(index))
        }
    }

    @Test
    fun `first half of dock items has null screen and cellX, all items distributed`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = MutableList(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    if (index < 3) {
                        screen = null
                        cellX = null
                    } else {
                        screen = index
                        cellX = index
                    }
                }
        }

        import.fixHotseat(items)

        items.sortBy { it.screen }

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
            assertThat(item.cellX, equalTo(index))
        }
    }

    @Test
    fun `second half of dock items has null screen and cellX, all items distributed`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = MutableList(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    if (index >= 3) {
                        screen = null
                        cellX = null
                    } else {
                        screen = index
                        cellX = index
                    }
                }
        }

        import.fixHotseat(items)

        items.sortBy { it.screen }

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
            assertThat(item.cellX, equalTo(index))
        }
    }

    @Test
    fun `all dock items has null screen and cellX, all items distributed`() {
        val import = ImportLauncher3(appContext, "", "", false)

        val items = MutableList(6) { index ->
            ItemData()
                .apply {
                    container = LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    screen = null
                    cellX = null
                }
        }

        import.fixHotseat(items)

        items.sortBy { it.screen }

        items.forEachIndexed { index, item ->
            assertThat(item.screen, equalTo(index))
            assertThat(item.cellX, equalTo(index))
        }
    }
}
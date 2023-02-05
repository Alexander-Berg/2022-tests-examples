package com.yandex.mail.testopithecus.feature.impl

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.childAtPosition
import com.yandex.mail.testopithecus.steps.clickAtActionMenu
import com.yandex.xplat.testopithecus.AdvancedSearch
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.LabelName
import io.qameta.allure.kotlin.Allure
import org.hamcrest.Matchers

class AdvancedSearchImpl(private val device: UiDevice) : AdvancedSearch {
    override fun addLabelToSearch(labelName: LabelName) {
        TODO("Not yet implemented")
    }

    override fun addFolderToSearch(folderName: FolderName) {
        Allure.step("Поиск по папке $folderName") {
            onView(
                Matchers.allOf(
                    ViewMatchers.withId(R.id.search_filter),
                    childAtPosition(
                        ViewMatchers.withId(R.id.search_parent_fragment_container),
                        1
                    )
                )
            ).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
            clickAtActionMenu(folderName)
        }
    }

    override fun searchOnlyImportant() {
        Allure.step("Поиск по важным") {
            onView(
                Matchers.allOf(
                    ViewMatchers.withId(R.id.search_filter),
                    childAtPosition(
                        ViewMatchers.withId(R.id.search_parent_fragment_container),
                        1
                    )
                )
            ).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(5, click()))
        }
    }
}

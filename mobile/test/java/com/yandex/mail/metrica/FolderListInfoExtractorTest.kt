package com.yandex.mail.metrica

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.yandex.mail.R
import com.yandex.mail.containers_list.ContainersAdapter
import com.yandex.mail.message_container.CustomContainer.Type
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.storage.entities.IMPORTANT
import com.yandex.mail.storage.entities.USER
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class FolderListInfoExtractorTest {

    lateinit var context: Context

    lateinit var view: View

    val folder = EntitiesTestFactory.buildNanoFolder().copy(fid = 111, type = 1)

    val label = EntitiesTestFactory.buildNanoMailLabel().copy(lid = "222", type = USER)

    @Before
    fun beforeEachTest() {
        val app = IntegrationTestRunner.app()
        context = app
        view = spy(
            View(context).apply {
                id = R.id.list_item
            }
        )
    }

    @Test
    fun `returns empty map if no item for view`() {
        val extractInfo = extractInfo(null)
        assertThat(extractInfo).isEmpty()
    }

    @Test
    fun `should add folder tag`() {
        val extractInfo = extractInfo(ContainersAdapter.FolderItem(EntitiesTestFactory.buildNanoFolder(), mock(), emptyMap()))
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_FOLDER
            )
        )
    }

    @Test
    fun `should not add info for HeaderItem`() {
        val headerItem = ContainersAdapter.HeaderItem(
            context.resources,
            R.string.sidebar_headline_folders_title_caps,
            R.string.folders_settings_new_folder_title,
        )
        val extractInfo = extractInfo(headerItem)
        assertThat(extractInfo).doesNotContainKey(MetricaConstns.RawMetrics.TARGET_VIEW_TAG)
    }

    @Test
    fun `should not add info for ButtonItem`() {
        val buttonItem = ContainersAdapter.ButtonItem(context.resources, R.string.settings, mock(), null) {}
        val extractInfo = extractInfo(buttonItem)
        assertThat(extractInfo)
            .contains(Assertions.entry(MetricaConstns.RawMetrics.TARGET_VIEW_TAG, context.resources.getResourceName(R.string.settings)))
    }

    @Test
    fun `should add unread tag`() {
        val unreadItem = ContainersAdapter.CustomContainerItem(context.resources, Type.UNREAD, false)
        val extractInfo = extractInfo(unreadItem)
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_UNREAD
            )
        )
    }

    @Test
    fun `should add withAttach tag`() {
        val withAttachItem = ContainersAdapter.CustomContainerItem(context.resources, Type.WITH_ATTACHMENTS, false)
        val extractInfo = extractInfo(withAttachItem)
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_WITH_ATTACHEMENTS
            )
        )
    }

    @Test
    fun `should add ubox tag`() {
        val withAttachItem = ContainersAdapter.UboxItem(context.resources, emptyList())
        val extractInfo = extractInfo(withAttachItem)
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_UBOX
            )
        )
    }

    @Test
    fun `should add user label tag`() {
        val extractInfo = extractInfo(ContainersAdapter.LabelItem(context, label, false))
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_LABEL
            )
        )
    }

    @Test
    fun `should add important label tag`() {
        val important = EntitiesTestFactory.buildNanoMailLabel().copy(lid = "222", type = IMPORTANT)
        val extractInfo = extractInfo(ContainersAdapter.LabelItem(context, important, false))
        assertThat(extractInfo).contains(
            Assertions.entry(
                MetricaConstns.RawMetrics.TARGET_VIEW_TAG,
                MetricaConstns.EventMetrics.FoldersEventsMetrics.FOLDERS_CHOOSE_IMPORTANT
            )
        )
    }

    @Test
    fun `should not add for unknown label tag`() {
        val labelWithUnknownType = EntitiesTestFactory.buildNanoMailLabel().copy(lid = "222", type = Int.MAX_VALUE)
        val extractInfo = extractInfo(ContainersAdapter.LabelItem(context, labelWithUnknownType, false))
        assertThat(extractInfo).doesNotContainKey(MetricaConstns.RawMetrics.TARGET_VIEW_TAG)
    }

    @Test
    fun `should not add for unknown container without containerListItem`() {
        val extractInfo = extractInfo(null)
        assertThat(extractInfo).doesNotContainKey(MetricaConstns.RawMetrics.TARGET_VIEW_TAG)
    }

    @Test
    fun `should add fid`() {
        val extractInfo = extractInfo(ContainersAdapter.FolderItem(folder, mock(), emptyMap()))
        assertThat(extractInfo).contains(Assertions.entry(MetricaConstns.RawMetrics.TARGET_FOLDER_ID, "111"))
    }

    @Test
    fun `should add lid`() {
        val extractInfo = extractInfo(ContainersAdapter.LabelItem(context, label, false))
        assertThat(extractInfo).contains(Assertions.entry(MetricaConstns.RawMetrics.TARGET_LABEL_ID, "222"))
    }

    private fun extractInfo(listItem: ContainersAdapter.Item?): Map<String, Any> {
        val layoutManager = mock<LinearLayoutManager> {
            on { findFirstVisibleItemPosition() } doReturn 1
            on { findLastVisibleItemPosition() } doReturn 7
            on { itemCount } doReturn 200
        }

        val viewHolder = mock<ContainersAdapter.BaseViewHolder> {
            on { getItem<ContainersAdapter.Item>() } doReturn listItem
            on { bindingAdapterPosition } doReturn 3
        }

        val factory = FolderListInfoExtractor.Factory(layoutManager)
        return factory.create(viewHolder).extractInfo(view)
    }
}

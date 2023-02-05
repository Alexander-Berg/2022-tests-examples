package com.yandex.mail.metrica

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.ads.util.AdsContainer
import com.yandex.mail.maillist.EmailListFragment.Companion.LIST_TOP_POSITION
import com.yandex.mail.metrica.MailListInfoExtractor.Factory
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.CLICKED_ITEM_AD_POSITIONS
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.CLICKED_ITEM_COUNT
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.CLICKED_ITEM_FIRST
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.CLICKED_ITEM_LAST
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.CLICKED_ITEM_POSITION
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_FOLDER_ID
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_LABEL_ID
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_MESSAGE_ID
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_THREAD_ID
import com.yandex.mail.provider.Constants.NO_ADS_POSITION
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.ui.adapters.EmailsListAdapter
import com.yandex.mail.ui.adapters.EmailsListAdapter.EmailViewHolder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections.singletonList

@RunWith(IntegrationTestRunner::class)
class MailListInfoExtractorTest {

    lateinit var layoutManager: LinearLayoutManager

    lateinit var viewHolder: EmailViewHolder

    lateinit var view: View

    val messageContent = EntitiesTestFactory.buildMessageContent()

    @Before
    fun beforeEachTest() {
        layoutManager = mock {
            on { findFirstVisibleItemPosition() } doReturn 1
            on { findLastVisibleItemPosition() } doReturn 7
            on { itemCount } doReturn 200
        }

        viewHolder = mock {
            on { emailItem } doReturn EmailsListAdapter.EmailItem(messageContent)
            on { bindingAdapterPosition } doReturn 3
        }

        view = View(IntegrationTestRunner.app())
    }

    @Test
    fun `should extract base info`() {
        val adsContainer = mock<AdsContainer> {
            on { getAdsPositions() } doReturn singletonList(NO_ADS_POSITION)
        }
        val factory = Factory(layoutManager, true, adsContainer)
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(
            entry(CLICKED_ITEM_POSITION, 3),
            entry(CLICKED_ITEM_FIRST, 1),
            entry(CLICKED_ITEM_LAST, 7),
            entry(CLICKED_ITEM_COUNT, 200)
        )
    }

    @Test
    fun `adds ad position if in list`() {
        val testAdPositions = singletonList(10)

        val adsContainer = mock<AdsContainer> {
            on { getAdsPositions() } doReturn testAdPositions
        }
        val factory = Factory(layoutManager, true, adsContainer)
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(CLICKED_ITEM_AD_POSITIONS, testAdPositions))
    }

    @Test
    fun `adds ad positions if in list`() {
        val testAdPositions = listOf(10, 20)

        val adsContainer = mock<AdsContainer> {
            on { getAdsPositions() } doReturn testAdPositions
        }
        val factory = Factory(layoutManager, true, adsContainer)
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(CLICKED_ITEM_AD_POSITIONS, testAdPositions))
    }

    @Test
    fun `does not add ad position if no position`() {
        val adsContainer = mock<AdsContainer> {
            on { getAdsPositions() } doReturn mutableListOf<Int>()
        }
        val factory = Factory(layoutManager, true, adsContainer)
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).doesNotContainKey(CLICKED_ITEM_AD_POSITIONS)
    }

    @Test
    fun `adds top ad position if no position`() {
        val adsContainer = mock<AdsContainer> {
            on { getAdsPositions() } doReturn mutableListOf(LIST_TOP_POSITION)
        }
        val factory = Factory(layoutManager, true, adsContainer)
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(CLICKED_ITEM_AD_POSITIONS, listOf(LIST_TOP_POSITION)))
    }

    @Test
    fun `adds thread id if thread mode`() {
        val factory = Factory(layoutManager, true, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(TARGET_THREAD_ID, messageContent.id))
        assertThat(result).doesNotContainKey(TARGET_MESSAGE_ID)
    }

    @Test
    fun `adds message id if not thread mode`() {
        val factory = Factory(layoutManager, false, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(TARGET_MESSAGE_ID, messageContent.id))
        assertThat(result).doesNotContainKey(TARGET_THREAD_ID)
    }

    @Test
    fun `adds folder id`() {
        val factory = Factory(layoutManager, true, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(TARGET_FOLDER_ID, messageContent.folderId))
    }

    @Test
    fun `adds label ids if not empty`() {
        val message = EntitiesTestFactory.buildMessageContent()
            .copy(labelIds = listOf("label1", "label2"))
        doReturn(EmailsListAdapter.EmailItem(message)).whenever(viewHolder).emailItem

        val factory = Factory(layoutManager, true, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).contains(entry(TARGET_LABEL_ID, "[label1, label2]"))
    }

    @Test
    fun `does not add label ids if empty`() {
        val message = EntitiesTestFactory.buildMessageContent().copy(labelIds = emptyList())
        doReturn(EmailsListAdapter.EmailItem(message)).whenever(viewHolder).emailItem

        val factory = Factory(layoutManager, true, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).doesNotContainKey(TARGET_LABEL_ID)
    }

    @Test
    fun `does not add message info if email item is null`() {
        doReturn(null).whenever(viewHolder).emailItem

        val factory = Factory(layoutManager, true, mock())
        val result = factory.create(viewHolder).extractInfo(view)

        assertThat(result).doesNotContainKeys(TARGET_MESSAGE_ID, TARGET_THREAD_ID, TARGET_FOLDER_ID, TARGET_LABEL_ID)
    }
}

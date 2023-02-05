package com.yandex.mail.metrica

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.yandex.mail.metrica.MetricaConstns.RawMetrics
import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class ListInfoExtractorTest {

    @Test
    fun `extracts info from layout manager and view holder`() {
        val layoutManager = mock<LinearLayoutManager> {
            on { findFirstVisibleItemPosition() } doReturn 1
            on { findLastVisibleItemPosition() } doReturn 7
            on { itemCount } doReturn 200
        }

        val viewHolder = mock<RecyclerView.ViewHolder> {
            on { bindingAdapterPosition } doReturn 3
        }

        val extractor = ListInfoExtractor(layoutManager, viewHolder)

        val result = extractor.extractInfo(View(IntegrationTestRunner.app()))

        assertThat(result).containsOnly(
            entry(RawMetrics.CLICKED_ITEM_POSITION, 3),
            entry(RawMetrics.CLICKED_ITEM_FIRST, 1),
            entry(RawMetrics.CLICKED_ITEM_LAST, 7),
            entry(RawMetrics.CLICKED_ITEM_COUNT, 200)
        )
    }
}

package ru.auto.ara.test.listing.minifilter

import org.junit.Test
import ru.auto.ara.consts.Filters
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.data.entities.form.Field
import ru.auto.ara.data.models.FormState
import ru.auto.ara.data.models.form.state.SimpleState
import ru.auto.ara.network.State
import ru.auto.data.model.filter.StateGroup

class MinifilterStateTest : BaseMinifilterListingTest(
    FormState.withDefaultCategory().apply {
        put(SimpleState().apply {
            type = Field.TYPES.segment
            fieldName = Filters.STATE_FIELD
            value = State.NEW
        })
    }
) {
    @Test
    fun shouldShowNewStateSegmentChecked() {
        performSearchFeed().checkResult {
            isStateSelectorChecked(StateGroup.NEW)
            watcher.checkRequestBodyParameter(STATE_REQUEST_PARAM, "NEW")
        }
    }

    @Test
    fun shouldChangeStateForUsedSegmentOnClick() {
        performSearchFeed {
            waitFirstPageLoaded(1)
            scrollToTop()
            selectState(StateGroup.USED)
            waitSearchFeed()
        }
        performSearchFeed().checkResult {
            isStateSelectorChecked(StateGroup.USED)
        }
        watcher.checkRequestBodyParameter(STATE_REQUEST_PARAM, "USED")
    }

    @Test
    fun shouldChangeStateForALLSegmentOnClick() {
        performSearchFeed {
            waitFirstPageLoaded(1)
            scrollToTop()
            selectState(StateGroup.ALL)
            waitSearchFeed()
        }
        performSearchFeed().checkResult {
            isStateSelectorChecked(StateGroup.ALL)
        }
        watcher.checkRequestBodyParameter(STATE_REQUEST_PARAM, "ALL")
    }
}

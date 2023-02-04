package ru.auto.ara.viewmodel.search

import android.os.Parcel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.RobolectricTest
import ru.auto.ara.data.models.form.state.RangeState
import ru.auto.test.runner.AllureRobolectricRunner


@RunWith(AllureRobolectricRunner::class) class SearchFeedContextParcelTest : RobolectricTest() {

    @Test
    fun `should parcel and restore RangeState correctly`() {
        val state = RangeState()
        val stateFromParcel = Parcel.obtain().let { parcel ->
            state.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            RangeState.CREATOR.createFromParcel(parcel)
        }
        assertThat(stateFromParcel).isEqualToComparingFieldByField(state)
    }
}

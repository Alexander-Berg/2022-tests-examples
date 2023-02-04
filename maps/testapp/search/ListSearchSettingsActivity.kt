package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity

class ListSearchSettingsActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val filtersLayout by lazy { find<LinearLayout>(R.id.search_settings_filters_layout) }
    private val sortByDistance by lazy { find<CheckBox>(R.id.search_settings_sort_by_distance) }

    private val checkBoxControls = mutableListOf<CheckBoxFilterControl>()
    private val rangeFilterControls = mutableListOf<RangeFilterControl>()
    private val dateFilterControls = mutableListOf<DateFilterControl>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_list_search_settings)

        initializeView()
    }

    private fun initializeView() {
        sortByDistance.isChecked = (intent.getBooleanExtra("sortByDistance", false))
        (intent.getSerializableExtra("filters") as? Filters)?.run {
            dateFilters.forEach { addDateFilter(it) }
            rangeFilters.forEach { addRangeFilter(it) }
            filters.forEach { addCheckBox(it) }
        }
    }

    private fun addDateFilter(f: DateFilter) {
        val dateFilter = DateFilterControl(this, f)
        dateFilterControls.add(dateFilter)
        filtersLayout.addView(dateFilter)
    }

    private fun addRangeFilter(f: RangeFilter) {
       val rangeFilter = RangeFilterControl(this, f)
       rangeFilterControls.add(rangeFilter)
       filtersLayout.addView(rangeFilter)
    }

    private fun addCheckBox(f: Filter) {
        val checkBox = CheckBoxFilterControl(this, f)
        checkBox.setOnCheckedChangeListener { _, value ->
            if (value) {
                checkBoxControls
                    .filter { it != checkBox }
                    .filter { it.filter.singleSelect }
                    .filter { it.filter.parentId == f.parentId }
                    .forEach { it.isChecked = false }
            }
        }

        checkBoxControls.add(checkBox)
        filtersLayout.addView(checkBox)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSettingsApplyTap(view: View) {
        onComplete()
        finish()
    }

    private fun onComplete() {
        val selectedFilters = Filters(
            checkBoxControls.mapNotNull { it.makeResultFilter() },
            rangeFilterControls.mapNotNull { it.makeResultFilter() },
            dateFilterControls.mapNotNull { it.makeResultFilter() }
        )
        setResult(Activity.RESULT_OK, Intent()
            .putExtra("filters", selectedFilters)
            .putExtra("sortByDistance", sortByDistance.isChecked))
        finish()
    }
}

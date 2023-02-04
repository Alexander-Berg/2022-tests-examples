package com.yandex.maps.testapp.search

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.BookingSearchSession.BookingSearchListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.Utils
import com.yandex.runtime.Error
import java.util.*


class BookingOffersActivity : TestAppActivity() {
    private val searchBox by lazy { find<SearchBox>(R.id.card_search_view) }
    private val searchResults by lazy { find<SectionedListView>(R.id.card_search_results) }
    private val datePicker by lazy { find<Button>(R.id.booking_info_date) }
    private val nightsSpinner by lazy { find<SpinnerWithPrompt>(R.id.booking_info_nights) }
    private val personsSpinner by lazy { find<SpinnerWithPrompt>(R.id.booking_info_persons) }
    private val datePickerFragment by lazy {
        DatePickerFragment().apply {
            onDateSetCallback = { year, month, day ->
                (activity as? BookingOffersActivity)?.let {
                    it.setDate(year, month, day)
                    it.findBookingOffers()
                }
            }
        }
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var session: BookingSearchSession? = null
    private var selectedDate: String? = null

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) { requestDefaultBookingOffers() }
    }

    private val bookingResponseListener: BookingSearchListener = object : BookingSearchListener {
        override fun onBookingSearchError(error: Error) {
            searchBox.setProgress(false)
            showErrorMessage(this@BookingOffersActivity, error)
        }

        override fun onBookingSearchResponse(response: BookingResponse) {
            searchBox.setProgress(false)
            fillResponse(response)
        }
    }

    override fun onStopImpl(){}
    override fun onStartImpl(){}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_booking_info)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        setupSpinner(nightsSpinner, R.array.search_booking_nights)
        setupSpinner(personsSpinner, R.array.search_booking_persons)
        setupDatePicker()
        setupSearchBox()
    }

    private fun setupSearchBox() {
        searchBox.text = "167972107609"
        find<View>(R.id.search_options).hide()
        searchBox.setListener(searchBoxListener)
        searchBoxListener.onSubmit(searchBox.text)
    }

    private fun uri() = "ymapsbm1://org?oid=${searchBox.text}"
    private fun date() = selectedDate!!
    private fun nights() = nightsSpinner.selectedItemPosition + 1
    private fun persons() = personsSpinner.selectedItemPosition + 1

    private fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        selectedDate = String.format(
            "%04d-%02d-%02d",
            year, month + 1, dayOfMonth
        )
        datePicker.text = String.format(
            getString(R.string.booking_info_check_in_template),
            year, month + 1, dayOfMonth
        )
    }

    private fun fillResponse(bookingResponse: BookingResponse) {
        bookingResponse.params?.let {
            nightsSpinner.setSelection(minOf(it.nights, 5) - 1)
            personsSpinner.setSelection(minOf(it.persons, 5) - 1)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.checkIn.value * 1000L
            calendar.timeZone = TimeZone.getTimeZone("UTC")
            setDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        if (bookingResponse.offers.isEmpty()) {
            Utils.showMessage(this, "No offers for selected params")
        }

        bookingResponse.offers.forEach { offer ->
            val items = mutableListOf(
                ItemWithDetails(offer.favicon?.urlTemplate, "favicon")
            )
            offer.bookingLinks.forEach {
                val suffix = if (it.type == "unknown") ""
                    else " [type='${it.type}']"
                items.add(ItemWithDetails(it.uri, "booking link$suffix"))
            }
            searchResults.addSection(
                "offer | ${offer.partnerName} | ${offer.price?.text}",
                items
            )
        }
    }

    private fun setupDatePicker() {
        datePicker.setOnClickListener {
            datePickerFragment.show(fragmentManager, "date-picker")
        }
    }

    private fun setupSpinner(spinner: SpinnerWithPrompt, resourceId: Int) {
        spinner.adapter = SpinnerWithPrompt.Adapter(this, resourceId)
        spinner.resetToPrompt()
        spinner.onItemSelectedCallback = { findBookingOffers() }
    }

    private fun requestDefaultBookingOffers() {
        onSearchStart()
        nightsSpinner.resetToPrompt()
        personsSpinner.resetToPrompt()
        datePicker.text = getString(R.string.booking_info_check_in_prompt)
        session = searchManager.findBookingOffers(
            uri(),
            null,
            bookingResponseListener
        )
    }

    private fun findBookingOffers() {
        val hasRequiredFields = selectedDate != null &&
            nightsSpinner.hasValidSelection() &&
            personsSpinner.hasValidSelection()
        if (!hasRequiredFields) { return }

        onSearchStart()
        session = searchManager.findBookingOffers(
            uri(),
            BookingRequestParams(date(), nights(), persons()),
            bookingResponseListener
        )
    }

    private fun onSearchStart() {
        searchBox.setProgress(true)
        searchResults.clearItems()

        session?.cancel()
    }
}

package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import ru.beru.android.R
import ru.yandex.market.mocks.model.checkout.DateItemMock
import ru.yandex.market.mocks.model.checkout.ScheduleItemMock
import ru.yandex.market.test.kakao.gridlayout.KGridLayout
import ru.yandex.market.test.util.DateUtils
import ru.yandex.market.test.util.DeliveryDateUtils
import ru.yandex.market.test.util.PriceUtils.toRubPriceString
import ru.yandex.market.test.util.WeekDayCase
import ru.yandex.market.utils.Characters

class KPickupPointInformationView : KBaseView<KPickupPointInformationView> {

    private val function: ViewBuilder.() -> Unit

    private val outletNameView: KTextView
        get() {
            return KTextView {
                withParent(this@KPickupPointInformationView.function)
                withId(R.id.nameView)
            }
        }

    private val outletAddressView: KView
        get() {
            return KView {
                withParent(this@KPickupPointInformationView.function)
                withId(R.id.address)
            }
        }
    private val scheduleGridLayout: KGridLayout
        get() {
            return KGridLayout {
                withParent(this@KPickupPointInformationView.function)
                withId(R.id.workScheduleView)
            }
        }

    private val descriptionView: KTextView
        get() {
            return KTextView {
                withParent(this@KPickupPointInformationView.function)
                withId(R.id.conditions)
            }
        }

    private val storagePeriodView: KTextView
        get() {
            return KTextView {
                isDescendantOfA(this@KPickupPointInformationView.function)
                withId(R.id.storagePeriodView)
            }
        }

    private val renewButton: KTextView
        get() {
            return KTextView {
                isDescendantOfA(this@KPickupPointInformationView.function)
                withId(R.id.renew_storage_limit_button)
            }
        }

    private val legalInfoView: KTextView
        get() {
            return KTextView {
                withParent(this@KPickupPointInformationView.function)
                withId(R.id.legalInfoTextView)
            }
        }

    constructor(function: ViewBuilder.() -> Unit) : super(function) {
        this@KPickupPointInformationView.function = function
    }

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function) {
        this@KPickupPointInformationView.function = {
            isDescendantOfA { withMatcher(parent) }
            function()
        }
    }

    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function) {
        this@KPickupPointInformationView.function = function
    }

    fun hasOutletName(text: String) {
        outletNameView {
            isVisible()
            hasText(text)
        }
    }

    fun hasAddress(address: String) {
        outletAddressView {
            isVisible()
            hasDescendant {
                withText(address)
            }
        }
    }

    fun hasClickAndCollectDescription(
        beginDate: DateItemMock,
        endDate: DateItemMock,
        deliveryPrice: Int,
        isPriceVisible: Boolean = true
    ) {
        val date = DeliveryDateUtils.getDeliveryDateText(beginDate, endDate, WeekDayCase.ACCUSATIVE)
        descriptionView {
            isVisible()
            if (isPriceVisible) {
                hasText(BUY_IN_SHOP.format(date, deliveryPrice.toRubPriceString()))
            } else {
                hasText(BUY_IN_SHOP_WITHOUT_PRICE.format(date, deliveryPrice.toRubPriceString()))
            }
        }
    }

    fun hasDescription(beginDate: DateItemMock, endDate: DateItemMock, deliveryPrice: Int) {
        descriptionView.isVisible()
        val date = DeliveryDateUtils.getDeliveryDateText(beginDate, endDate, WeekDayCase.ACCUSATIVE)
        val description = if (beginDate.day == endDate.day) {
            PICKUP_DELIVERY_DESCRIPTION_SINGLE_DAY_TEXT
        } else {
            PICKUP_DELIVERY_DESCRIPTION_INTERVAL_TEXT
        }
        descriptionView.hasText(String.format(description, date, deliveryPrice.toRubPriceString()))
    }

    fun hasStoragePeriod(storagePeriod: Int) {
        storagePeriodView {
            isVisible()
            val text = when (storagePeriod) {
                1 -> STORAGE_PERIOD.format(storagePeriod)
                2, 3, 4 -> STORAGE_PERIOD_SOME_DAYS.format(storagePeriod)
                else -> STORAGE_PERIOD_MANY_DAYS.format(storagePeriod)
            }
            hasText(text)
        }
    }

    fun hasStorageLimitDate(date: String) {
        storagePeriodView {
            isVisible()
            hasText("Срок хранения до $date")
        }
    }

    fun checkRenewButton(isVisible: Boolean) {
        renewButton {
            if (isVisible) isVisible() else isGone()
        }
    }

    fun clickRenew() {
        renewButton {
            isVisible()
            click()
        }
    }

    fun hasSchedule(schedule: List<ScheduleItemMock>) {
        scheduleGridLayout.isVisible()
        if (schedule.isEmpty()) {
            return
        }

        val matchers = ArrayList<Matcher<Iterable<View>>>()

        schedule.forEach { item ->
            val dayInterval = item.dayInterval
            val timeInterval = item.timeInterval

            val timeIntervalText = String.format(
                PICKUP_DELIVERY_TIME_INTERVAL_TEXT,
                timeInterval.from.hours,
                timeInterval.from.minutes,
                timeInterval.to.hours,
                timeInterval.to.minutes
            )

            if (dayInterval.from == 1 && dayInterval.to == 7) {
                matchers.add(
                    Matchers.contains(
                        ViewMatchers.withText("Ежедневно"),
                        ViewMatchers.withText(timeIntervalText)
                    )
                )
            } else if (dayInterval.from == 1 && dayInterval.to == 5) {
                matchers.add(
                    Matchers.contains(
                        ViewMatchers.withText("Будни"),
                        ViewMatchers.withText(timeIntervalText)
                    )
                )
            } else {
                for (day in dayInterval.from..dayInterval.to) {
                    matchers.add(
                        Matchers.contains(
                            ViewMatchers.withText(DateUtils.getWeekDayNameNominative(day).capitalize()),
                            ViewMatchers.withText(timeIntervalText)
                        )
                    )
                }
            }
        }

        scheduleGridLayout.hasRows(matchers)
    }

    fun checkJuridicalInfo(
        type: String,
        outletName: String,
        postalAddress: String,
        licenseNumber: String,
        licenseStartDate: String,
        ogrn: String
    ) {
        val legalInfo = StringBuilder()
            .append(type)
            .append(" ")
            .append(outletName)
            .append(", ")
            .append("юр.адрес: ")
            .append(postalAddress)
            .append(", ")
            .append("ОГРН ")
            .append(ogrn)
            .append(". ")
            .append("Лицензия № ")
            .append(licenseNumber)
            .append(" от ")
            .append(licenseStartDate)
            .toString()
        legalInfoView {
            isVisible()
            hasText(legalInfo)
        }
    }

    companion object {
        private const val BUY_IN_SHOP = "Доставка в${Characters.NON_BREAKING_SPACE}торговый зал %s, доставка стоит %s"
        private const val BUY_IN_SHOP_WITHOUT_PRICE = "Доставка в${Characters.NON_BREAKING_SPACE}торговый зал %s"
        private const val PICKUP_DELIVERY_DESCRIPTION_SINGLE_DAY_TEXT = "Ближайшая доставка в %s, %s"
        private const val PICKUP_DELIVERY_DESCRIPTION_INTERVAL_TEXT = "Ближайшая доставка %s, %s"
        private const val STORAGE_PERIOD = "Срок хранения заказа  %d день"
        private const val STORAGE_PERIOD_SOME_DAYS = "Срок хранения заказа  %d дня"
        private const val STORAGE_PERIOD_MANY_DAYS = "Срок хранения заказа  %d дней"
        private const val PICKUP_DELIVERY_TIME_INTERVAL_TEXT = "%02d:%02d – %02d:%02d"
    }
}

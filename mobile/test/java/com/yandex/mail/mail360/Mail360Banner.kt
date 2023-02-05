package com.yandex.mail.mail360

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.yandex.mail.R
import com.yandex.mail.runners.UnitTestRunner
import com.yandex.mail360.Mail360BannerType
import com.yandex.mail360.ServiceListBaseDialogFragment
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.Period

@RunWith(UnitTestRunner::class)
class Mail360Banner {

    private lateinit var prefs: SharedPreferences
    private val docType = Mail360BannerType.Documents()
    private val scansType = Mail360BannerType.Scans(R.string.mail360_promo_title_scan_mail)
    private val mail360Type = Mail360BannerType.Mail360()
    private var instant = Instant.now()

    @Before
    fun beforeEachTest() {
        prefs = UnitTestRunner.app().getSharedPreferences("pref", Context.MODE_PRIVATE)
        prefs.edit { clear() }
        instant = Instant.now()
    }

    @Test
    fun chooseBannerChangingProperly() {
        val result1 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result1?.analyticsName).isEqualTo(docType.analyticsName)

        instant = instant.plus(Period.ofWeeks(1)).plus(Period.ofDays(1))
        val result2 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result2?.analyticsName).isEqualTo(docType.analyticsName)

        instant = instant.plus(Period.ofWeeks(1))
        val result3 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result3?.analyticsName).isEqualTo(scansType.analyticsName)

        prefs.edit { putBoolean(scansType.settingsKeyName, false) }
        prefs.edit { remove(ServiceListBaseDialogFragment.PREF_ARG_FIRST_SHOWN_TIME) }
        val result4 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result4?.analyticsName).isEqualTo(mail360Type.analyticsName)

        instant = instant.plus(Period.ofWeeks(1))
        val result5 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result5?.analyticsName).isEqualTo(mail360Type.analyticsName)

        instant = instant.plus(Period.ofWeeks(1)).plus(Period.ofDays(1))
        val result6 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result6?.analyticsName).isEqualTo(docType.analyticsName)
    }

    @Test
    fun chooseBannerChangingProperlyAfterClose() {
        val result1 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result1?.analyticsName).isEqualTo(docType.analyticsName)

        instant = instant.plus(Period.ofDays(13))
        val result2 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result2?.analyticsName).isEqualTo(docType.analyticsName)

        prefs.edit { putBoolean(docType.settingsKeyName, false) }
        prefs.edit { remove(ServiceListBaseDialogFragment.PREF_ARG_FIRST_SHOWN_TIME) }
        val result3 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result3?.analyticsName).isEqualTo(scansType.analyticsName)

        instant = instant.plus(Period.ofDays(1))
        val result4 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result4?.analyticsName).isEqualTo(scansType.analyticsName)

        instant = instant.plus(Period.ofDays(1))
        val result5 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result5?.analyticsName).isEqualTo(scansType.analyticsName)

        instant = instant.plus(Period.ofWeeks(2))
        val result6 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOfBanners(),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result6?.analyticsName).isEqualTo(mail360Type.analyticsName)
    }

    @Test
    fun chooseBannerProperlyIfOnlyOneBanner() {
        val result1 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOf(mail360Type),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result1?.analyticsName).isEqualTo(mail360Type.analyticsName)

        instant = instant.plus(Period.ofWeeks(2)).plus(Period.ofDays(1))
        val result2 = ServiceListBaseDialogFragment.chooseBanner(
            prefs,
            listOf(mail360Type),
            instant,
            ServiceListBaseDialogFragment.TOTAL_BANNER_COUNT,
        )
        Assertions.assertThat(result2?.analyticsName).isEqualTo(mail360Type.analyticsName)
    }

    private fun listOfBanners() = listOf(mail360Type, docType, scansType)
}

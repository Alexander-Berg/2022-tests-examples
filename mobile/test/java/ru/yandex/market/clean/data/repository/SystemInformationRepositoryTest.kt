package ru.yandex.market.clean.data.repository

import com.annimon.stream.Exceptional
import com.annimon.stream.Optional
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.asOptional
import ru.yandex.market.clean.data.mapper.LanguageCodeMapper
import ru.yandex.market.clean.data.mapper.LocaleMapper
import ru.yandex.market.clean.data.store.SystemInformationDataSource
import ru.yandex.market.clean.domain.model.LanguageCode
import ru.yandex.market.internal.PreferencesDataStore
import java.util.Locale

class SystemInformationRepositoryTest {

    private val systemInformationDataSource = mock<SystemInformationDataSource>()

    private val languageCodeMapper = mock<LanguageCodeMapper>()

    private val preferencesDataStore = mock<PreferencesDataStore>()

    private val localeMapper = mock<LocaleMapper>()

    private val repository = SystemInformationRepository(
        systemInformationDataSource,
        languageCodeMapper, preferencesDataStore, localeMapper
    )

    @Test
    fun `Reads system locale from preferences and map it to language code`() {
        val languageTag = "en-US"
        val locale = Locale.ENGLISH
        val languageCode = LanguageCode.UNKNOWN
        whenever(preferencesDataStore.systemLocale)
            .thenReturn(Observable.just(languageTag.asOptional()))
        whenever(localeMapper.fromString(any())).thenReturn(locale.asExceptional())
        whenever(languageCodeMapper.map(any())).thenReturn(languageCode)

        repository.systemLanguage
            .test()
            .assertValue(languageCode)

        verify(localeMapper).fromString(languageTag)
        verify(languageCodeMapper).map(locale)
    }

    @Test
    fun `Reads system locale from system information when failed to get from preferences`() {
        val languageCode = LanguageCode.UNKNOWN
        whenever(preferencesDataStore.systemLocale).thenReturn(Observable.just(Optional.empty()))
        whenever(localeMapper.fromString(any())).thenReturn(Exceptional.of(RuntimeException()))
        whenever(systemInformationDataSource.locale).thenReturn(Single.just(Locale.ENGLISH))
        whenever(languageCodeMapper.map(any())).thenReturn(languageCode)

        repository.systemLanguage.subscribe()

        verify(systemInformationDataSource).locale
    }

    @Test
    fun `Save non-null locale to preferences`() {
        whenever(localeMapper.toString(any())).thenReturn("Locale")
        whenever(preferencesDataStore.setSystemLocale(any())).thenReturn(Completable.complete())

        repository.setSystemLocale(Locale.ENGLISH)
            .test()
            .assertComplete()
    }

    @Test
    fun `Remove locale from preferences when passed locale is null`() {
        whenever(preferencesDataStore.removeSystemLocale()).thenReturn(Completable.complete())

        repository.setSystemLocale(null)
            .test()
            .assertComplete()
    }
}
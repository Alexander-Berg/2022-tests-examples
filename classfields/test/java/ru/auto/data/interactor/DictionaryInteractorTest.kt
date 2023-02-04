package ru.auto.data.interactor

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.listeners.VerificationListener
import org.mockito.verification.VerificationEvent
import ru.auto.data.model.dictionary.COMMON
import ru.auto.data.model.dictionary.Dictionary
import ru.auto.data.model.dictionary.GEO_SUGGEST_LISTING
import ru.auto.data.repository.IDictionaryRepository
import ru.auto.data.repository.IGeoRepository
import rx.Completable
import rx.Single
import java.util.concurrent.TimeUnit

/**
 * @author themishkun on 20/06/2018.
 */
@RunWith(AllureRunner::class)
class DictionaryInteractorTest {

    private val CATEGORY = "category"
    private val DICTIONARY = "dictionary"
    private val DICTIONARY_2 = "dictionary2"
    private val VERSION_1 = "1.0"
    private val VERSION_2 = "1.1"

    private val dictionaryRepository: IDictionaryRepository = mock()
    private val geoRepository: IGeoRepository = mock()
    private val oldDictionary = Dictionary(VERSION_1, emptyMap())

    private val listener = object : VerificationListener {
        override fun onVerification(verificationEvent: VerificationEvent) {
            step("Verify method invocation") {
                parameter("invocation", verificationEvent.data.target)
            }
        }
    }

    @Before
    fun setupMockito() {
        Mockito.framework().addListener(listener)
    }

    @After
    fun disposeMockito() {
        Mockito.framework().removeListener(listener)
    }

    @Test
    fun `given different version it should update dictionary`() {
        val interactor = step("Given dictionary interactor") {
            val dictToNewVersion = mapOf(CATEGORY to mapOf(DICTIONARY to VERSION_2))
            val oldDictionary = mapOf(DICTIONARY to oldDictionary)
            prepareRepoMock(dictToNewVersion, oldDictionary)
            DictionaryInteractor(setOf(CATEGORY), dictionaryRepository, geoRepository)
        }

        val completable = step("When updating dictionaries") {
            interactor.updateDictionaries().test().apply {
                awaitTerminalEvent()
            }
        }

        step("Then") {
            step("Update should complete successfully") {
                completable.assertCompleted()
            }
            step("Should called ru.auto.data.repository.IDictionaryRepository.getActualVersions") {
                verify(dictionaryRepository).getActualVersions()
            }
            step("Then should complete") {
                verify(dictionaryRepository).updateDictionary(eq(CATEGORY), eq(DICTIONARY))
            }
        }
    }

    @Test
    fun `given same version it should skip update`() {
        val interactor = step("Given dictionary interactor") {
            val dictToSameVersion = mapOf(CATEGORY to mapOf(DICTIONARY to VERSION_1))
            val oldDictionary = mapOf(DICTIONARY to oldDictionary)
            prepareRepoMock(dictToSameVersion, oldDictionary)
            DictionaryInteractor(setOf(CATEGORY), dictionaryRepository, geoRepository)
        }

        val completable = step("When updating dictionaries") {
            interactor.updateDictionaries().test().apply {
                awaitTerminalEvent()
            }
        }

        completable.assertCompleted()
        verify(dictionaryRepository).getActualVersions()
        verify(dictionaryRepository, times(0)).updateDictionary(any(), any())
    }

    @Test
    fun `given one dictionary version differ and another same it should update 1st and skip 2nd`() {
        val dictToMixedVersion = mapOf(CATEGORY to mapOf(DICTIONARY to VERSION_2, DICTIONARY_2 to VERSION_1))
        val oldDicts = mapOf(DICTIONARY to oldDictionary, DICTIONARY_2 to oldDictionary)
        prepareRepoMock(dictToMixedVersion, oldDicts)
        val interactor = DictionaryInteractor(setOf(CATEGORY), dictionaryRepository, geoRepository)

        val completable = interactor.updateDictionaries().test()
        completable.awaitTerminalEvent()

        completable.assertCompleted()
        verify(dictionaryRepository).getActualVersions()
        verify(dictionaryRepository).updateDictionary(eq(CATEGORY), eq(DICTIONARY))
        verify(dictionaryRepository, times(0)).updateDictionary(eq(CATEGORY), eq(DICTIONARY_2))
    }

    @Test
    fun `given new version of geo_suggests_listing it should try to update geo`() {
        val dictToVersion = mapOf(COMMON to mapOf(GEO_SUGGEST_LISTING to VERSION_1))
        prepareRepoMock(dictToVersion, emptyMap())
        whenever(geoRepository.updateCache(any())).thenReturn(Completable.complete())
        val interactor = DictionaryInteractor(setOf(), dictionaryRepository, geoRepository)

        val test = interactor.updateDictionaries().test()
        test.awaitTerminalEvent(1, TimeUnit.SECONDS)

        test.assertCompleted()
        verify(geoRepository).updateCache(eq(VERSION_1))
    }

    private fun prepareRepoMock(
        version: Map<String, Map<String, String>>,
        dictionary: Map<String, Dictionary>
    ) = step("prepare repo mock") {
        parameter("Version", version)
        parameter("Dictionary", dictionary)
        whenever(dictionaryRepository.getActualVersions()).thenReturn(Single.just(version))
        whenever(dictionaryRepository.getDictionariesForCategory(eq(CATEGORY))).thenReturn(Single.just(dictionary))
        whenever(dictionaryRepository.updateDictionary(eq(CATEGORY), any())).thenReturn(Completable.complete())
    }
}

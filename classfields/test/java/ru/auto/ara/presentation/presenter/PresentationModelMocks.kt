package ru.auto.ara.presentation.presenter

import org.mockito.Mockito
import org.mockito.kotlin.mock
import ru.auto.ara.router.navigator.BaseNavigator
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.error.ErrorFactory
import ru.auto.ara.util.stubIt

/**
 * @author dumchev on 04/12/2018.
 */
object PresentationModelMocks {
    val router: BaseNavigator = mock()
    val errorFactory: ErrorFactory = mock()
    val stringsProvider: StringsProvider = Mockito.mock(StringsProvider::class.java)
        .apply { stubIt() }
}

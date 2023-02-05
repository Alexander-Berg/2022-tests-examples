@file:Suppress("ArchitectureLayersRule", "AndroidImportInDomainLayer")

package ru.yandex.market.clean.domain.usecase.ar

import android.os.Build
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.feature.ar.IsDeviceArCompatibleUseCase
import ru.yandex.market.util.manager.InstalledApplicationManager

class IsDeviceArCompatibleUseCaseTest {

    private val installedApplicationManager = mock<InstalledApplicationManager>()

    @Test
    fun `AR app is installed`() {
        whenever(installedApplicationManager.isApplicationInstalled(any<Int>())).thenReturn(Single.just(true))
        val useCase = IsDeviceArCompatibleUseCase(installedApplicationManager)

        useCase.execute().test().assertNoErrors().assertValue(IS_AR_SUPPORTED_API)
    }

    @Test
    fun `AR app is not installed`() {
        whenever(installedApplicationManager.isApplicationInstalled(any<Int>())).thenReturn(Single.just(false))
        val useCase = IsDeviceArCompatibleUseCase(installedApplicationManager)

        useCase.execute().test().assertNoErrors().assertValue(false)
    }

    companion object {
        val IS_AR_SUPPORTED_API = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

}

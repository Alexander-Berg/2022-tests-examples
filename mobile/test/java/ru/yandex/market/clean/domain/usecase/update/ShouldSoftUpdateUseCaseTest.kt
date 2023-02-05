package ru.yandex.market.clean.domain.usecase.update

import android.os.Build
import com.annimon.stream.OptionalLong
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.data.repository.PreferencesRepository
import ru.yandex.market.common.featureconfigs.managers.SoftUpdateConfigManager
import ru.yandex.market.common.featureconfigs.models.SoftUpdateConfiguration
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.common.schedulers.DataSchedulers
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.createDate
import java.util.Date

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ShouldSoftUpdateUseCaseTest(
    private val today: Date,
    private val updateAskTime: Date?,
    private val updateFailedTime: Date?,
    private val isUpdateInfoExpected: Boolean
) {

    private val preferencesRepository = mock<PreferencesRepository>()
    private val dateTimeProvider = mock<DateTimeProvider>()
    private val softUpdateConfigManager = mock<SoftUpdateConfigManager>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.softUpdateConfigManager) doReturn softUpdateConfigManager
    }
    private val dataSchedulers = mock<DataSchedulers> {
        on { worker } doReturn Schedulers.trampoline()
    }

    private val configuration =
        SoftUpdateConfiguration(
            askForUpdateAfterFailIntervalDays = 3,
            askForUpdateAfterLastAttemptIntervalDays = 6
        )

    private val updateInfo = mock<AppUpdateInfo>()

    private val useCase = ShouldSoftUpdateUseCase(
        preferencesRepository,
        dateTimeProvider,
        featureConfigsProvider,
        dataSchedulers,
    )

    @Test
    fun `Properly emit update info`() {
        whenever(updateInfo.updateAvailability())
            .thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
            .thenReturn(true)

        whenever(dateTimeProvider.currentUtcTimeInMillis).thenReturn(today.time)
        whenever(preferencesRepository.getUpdateAskTime())
            .thenReturn(
                Single.just(updateAskTime?.let { OptionalLong.of(it.time) } ?: OptionalLong.empty())
            )
        whenever(preferencesRepository.getUpdateFailedTime())
            .thenReturn(
                Single.just(updateFailedTime?.let { OptionalLong.of(it.time) } ?: OptionalLong.empty())
            )
        whenever(softUpdateConfigManager.get())
            .thenReturn(configuration)

        val testObserver = useCase.shouldAskForUpdate().test()
        if (isUpdateInfoExpected) {
            testObserver.assertValue(true)
        } else {
            testObserver.assertValue(false)
        }
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            //Return nothing if updateFailedTime is empty and updateAskTime is 4 day ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 11),
                null,
                false
            ),

            //1
            //Return update info if updateFailedTime is empty and updateAskTime is 10 day ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 5),
                null,
                true
            ),

            //2
            //Return nothing if updateFailedTime and updateAskTime was 1 day ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 14),
                createDate(2019, 8, 14),
                false
            ),

            //3
            //Return update info if updateFailedTime is 1 day ago and updateAskTime was 4 days ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 11),
                createDate(2019, 8, 11),
                true
            ),

            //4
            //Return update info if updateFailedTime is 4 day ago and updateAskTime was 10 days ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 11),
                createDate(2019, 8, 5),
                true
            ),

            //5
            //Return update info if updateFailedTime is 1 day ago and updateAskTime was 10 days ago
            arrayOf(
                createDate(2019, 8, 15),
                createDate(2019, 8, 14),
                createDate(2019, 8, 5),
                true
            )
        )
    }
}
package ru.auto.ara.core.di.module

import com.yandex.updater.lib.Updater
import ru.auto.ara.TestObjectsProvider
import ru.auto.ara.core.TestExperimentsManager
import ru.auto.ara.core.TestImagePreviewLoader
import ru.auto.ara.core.TestLoanCabinetFragmentFactory
import ru.auto.ara.core.TestLocationAutoDetectInteractor
import ru.auto.ara.core.TestPanoramaNamePickerRepository
import ru.auto.ara.core.TestPanoramasListRepository
import ru.auto.ara.core.TestScreenVisibilityRepository
import ru.auto.ara.core.TestSearchGeoService
import ru.auto.ara.core.TestUidInterceptor
import ru.auto.ara.core.feature.calls.TestPushTokenRepository
import ru.auto.ara.core.feature.calls.TestVoxRepository
import ru.auto.ara.core.feature.mic_promo.TestMicPromoArgsHolder
import ru.auto.ara.core.feature.mic_promo.TestMicPromoDomainInteractor
import ru.auto.ara.core.mocks_and_stubbs.TestExteriorPanoramaPlayerRepository
import ru.auto.ara.core.mocks_and_stubbs.TestNetworkInfoRepository
import ru.auto.ara.core.mocks_and_stubbs.TestSafeDealPromoOfferOverlayRepository
import ru.auto.ara.core.mocks_and_stubbs.TestSafeDealSellerOnboardingRepository
import ru.auto.ara.core.mocks_and_stubbs.camera.TestCameraDelegateFactory
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentControllerFactory
import ru.auto.ara.core.mocks_and_stubbs.trust.TestTrustPaymentRepository
import ru.auto.ara.core.routing.LocalHostRoutingInterceptor
import ru.auto.ara.core.rules.TestLoanWizardStepProviderHolder
import ru.auto.ara.core.rules.di.TestAppStoreChecker
import ru.auto.ara.core.rules.di.TestGeoProxyRepo
import ru.auto.ara.core.rules.di.TestObjectsHolder
import ru.auto.ara.core.rules.di.TestRandomProvider
import ru.auto.ara.core.rules.di.TestSystemInfoRepository
import ru.auto.ara.core.rules.di.TestWizardStepProviderHolder
import ru.auto.ara.core.rules.di.TestYaPlusRepo
import ru.auto.ara.core.rules.di.VoxRepoArgsHolder
import ru.auto.ara.core.utils.TestRandom
import ru.auto.ara.data.feed.loader.ISoldItemFeedPositionProvider
import ru.auto.ara.data.feed.loader.ISoldOfferResponsePositionProvider
import ru.auto.ara.di.module.ApiProvider
import ru.auto.ara.di.module.MainProvider
import ru.auto.ara.feature.drivepromo.DriveResourcesRepository
import ru.auto.ara.network.interceptor.IHelloInterceptor
import ru.auto.ara.network.interceptor.UidInterceptor
import ru.auto.ara.presentation.presenter.wizard.DevWizardStepTestProvider
import ru.auto.ara.presentation.presenter.wizard.IWizardStepTestProvider
import ru.auto.ara.screenshotTests.onboarding.TestOnboardingShowingRepository
import ru.auto.ara.ui.adapter.main.ILoanCabinetFragmentFactory
import ru.auto.ara.util.payment.trust.ITrustPaymentController
import ru.auto.ara.util.statistics.MetricaAnalyst
import ru.auto.ara.util.statistics.MetricaAnalystMock
import ru.auto.util.IRandom
import ru.auto.core_ui.ui.camera.ICameraDelegateFactory
import ru.auto.core_ui.util.IAppStoreChecker
import ru.auto.core_ui.util.accelerometer.ISystemOrientationAbilityChecker
import ru.auto.core_ui.util.image.IImagePreviewLoader
import ru.auto.core_ui.util.image.ImagePreviewLoaderFactory
import ru.auto.core_ui.util.panorama.IPanoramaFramesLoader
import ru.auto.core_ui.util.panorama.PanoramaFrames
import ru.auto.data.model.dadata.Suggest
import ru.auto.data.model.wizard.MarkStep
import ru.auto.data.network.scala.DaDataApi
import ru.auto.data.repository.DrivePromocodesRepository
import ru.auto.data.repository.EventRepository
import ru.auto.data.repository.IDownloadReportRepository
import ru.auto.data.repository.IDrivePromocodesRepository
import ru.auto.data.repository.IEventRepository
import ru.auto.data.repository.IGeoRepository
import ru.auto.data.repository.IGooglePayRepository
import ru.auto.data.repository.INetworkInfoRepository
import ru.auto.data.repository.ISafeDealSellerOnboardingRepository
import ru.auto.data.repository.IScreenVisibilityRepository
import ru.auto.data.repository.ISystemInfoRepository
import ru.auto.data.repository.ITrustPaymentRepository
import ru.auto.data.repository.IVasEventRepository
import ru.auto.data.repository.IYaPlusRepository
import ru.auto.data.repository.VasEventRepository
import ru.auto.data.repository.dadata.DaDataRepository
import ru.auto.data.repository.dadata.IDaDataRepository
import ru.auto.data.repository.push_token.IPushTokenRepository
import ru.auto.data.storage.frontlog.FrontlogStorage
import ru.auto.data.storage.frontlog.VasFrontlogStorage
import ru.auto.data.util.Optional
import ru.auto.feature.calls.cross_concern.App2AppAgent
import ru.auto.feature.calls.cross_concern.IApp2AppAgent
import ru.auto.feature.calls.data.IVoxRepository
import ru.auto.feature.carfax.repository.DownloadReportRepositoryMock
import ru.auto.feature.carfax.repository.RandomProvider
import ru.auto.feature.loans.personprofile.wizard.presentation.IWizardStepsProvider
import ru.auto.feature.loans.personprofile.wizard.presentation.WizardStepsProvider
import ru.auto.feature.maps.mapkit.ISearchGeoService
import ru.auto.feature.mic_promo.domain.MicPromoDialogOpener
import ru.auto.feature.mic_promo_api.IMicPromoDialogOpener
import ru.auto.feature.mic_promo_api.IMicPromoDomainInteractor
import ru.auto.feature.mic_promo_api.IMicPromoInteractor
import ru.auto.feature.onboarding.data.repository.OnboardingShowingRepository
import ru.auto.feature.panorama.exteriorplayer.data.IExteriorPanoramaPlayerRepository
import ru.auto.feature.panorama.list.data.IPanoramasListRepository
import ru.auto.feature.panorama.namepicker.data.IPanoramaNamePickerRepository
import ru.auto.feature.panorama.recorder.feature.PanoramaRecorderSettings
import ru.auto.feature.recognizer.textrecognizer.IOnDeviceTextRecognizer
import ru.auto.feature.recognizer.textrecognizer.TestOnDeviceTextRecognizer
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers

class TestMainProvider(deps: Dependencies) : MainProvider(deps) {


    override val wizardStepsProvider: IWizardStepsProvider get() =
        TestLoanWizardStepProviderHolder.apply {
            if (stepsProvider == null) stepsProvider = WizardStepsProvider
        }

    override val testObjectsProvider: Optional<TestObjectsProvider> by lazy {
        Optional.of(TestObjectsHolder)
    }

    override val apiProvider: ApiProvider by lazy {
        object : ApiProvider(this@TestMainProvider, deps) {
            override val uiTestRoutingInterceptor = LocalHostRoutingInterceptor
        }
    }

    override val eventRepository: IEventRepository by lazy {
        val storage = FrontlogStorage(deps.databaseCompartment)
        EventRepository(
            api = scalaApi,
            storage = storage,
            legacyNetworkInfoRepository = legacyNetworkInfoRepository,
            bufferScheduler = Schedulers.computation(),
            bufferSize = 1,
            experimentsRepository = experimentsRepository,
            gmsAdvertisingRepository = gmsAdvertisingRepository,
            hmsAdvertisingRepository = hmsAdvertisingRepository
        )
    }

    override val vasFrontlogRepo: IVasEventRepository by lazy {
        val storage = VasFrontlogStorage(deps.databaseCompartment)
        VasEventRepository(
            api = scalaApi,
            storage = storage,
            legacyNetworkInfoRepository = legacyNetworkInfoRepository,
            bufferScheduler = Schedulers.computation(),
            bufferSize = 1,
            experimentsRepository = experimentsRepository,
            gmsAdvertisingRepository = gmsAdvertisingRepository,
            hmsAdvertisingRepository = hmsAdvertisingRepository
        )
    }

    override val uidInterceptor: UidInterceptor = TestUidInterceptor()

    override val helloInterceptor: IHelloInterceptor = uidInterceptor

    override val screenVisibilityRepository: IScreenVisibilityRepository by lazy {
        TestScreenVisibilityRepository()
    }

    override val drivePromocodesRepository: IDrivePromocodesRepository by lazy {
        DrivePromocodesRepository(context) { 0 }
    }

    override val driveResourcesRepository: DriveResourcesRepository by lazy {
        DriveResourcesRepository(context) { 0 }
    }

    override val loaderFactory by lazy {
        object : ImagePreviewLoaderFactory {
            override fun invoke(): IImagePreviewLoader = TestImagePreviewLoader()
        }
    }

    override val locationDetectInteractor by lazy {
        TestLocationAutoDetectInteractor(geoRepository)
    }

    override val soldItemFeedPositionProvider by lazy {
        object : ISoldItemFeedPositionProvider {
            override fun getNextPosition(size: Int): Int = TestRandomProvider(size)
        }
    }

    override val soldOfferResponsePositionProvider: ISoldOfferResponsePositionProvider by lazy {
        object : ISoldOfferResponsePositionProvider {
            override fun getNextPosition(size: Int): Int = 0
        }
    }

    override val daDataRepository: IDaDataRepository by lazy { provideDaDataRepository(apiProvider.daDataApi) }
    fun provideDaDataRepository(api: DaDataApi): IDaDataRepository =
        @Suppress("NotImplementedDeclaration") // This object used only in some tests
        object : IDaDataRepository by DaDataRepository(api) {
            override fun getFioSuggest(fio: String): Single<List<Suggest>> = Single.just(
                listOf(
                    Suggest(
                        "Тестов Тест Тестович",
                        "Тестов Тест Тестович",
                        Suggest.Data(
                            "Тестов",
                            "Тест",
                            "Тестович",
                            Suggest.Data.Gender.UNKNOWN,
                            Suggest.Data.QCDetect.KNOWN
                        )
                    )
                )
            )
        }

    override val searchGeoService: ISearchGeoService by lazy { TestSearchGeoService() }

    override val random: IRandom
        get() = testRandom

    override val randomProvider by lazy {
        RandomProvider { TestRandomProvider(it) }
    }

    override val systemInfoRepository: ISystemInfoRepository by lazy { TestSystemInfoRepository }

    override val googlePayRepository: IGooglePayRepository = object : IGooglePayRepository {
        override fun checkGooglePayAvailable(): Single<Boolean> = Single.just(true)
    }

    override val wizardStepTestProvider: IWizardStepTestProvider by lazy {
        TestWizardStepProviderHolder.apply {
            if (wizardStepProvider == null) wizardStepProvider = DevWizardStepTestProvider(MarkStep)
        }
    }
    override val textRecognizer: IOnDeviceTextRecognizer by lazy { TestOnDeviceTextRecognizer() }


    override val panoramasListRepository: IPanoramasListRepository by lazy {
        TestPanoramasListRepository()
    }


    override val panoramaNamePickerRepository: IPanoramaNamePickerRepository by lazy {
        TestPanoramaNamePickerRepository()
    }


    override val geoRepository: IGeoRepository by lazy { TestGeoProxyRepo(super.geoRepository) }

    override val voxRepository: IVoxRepository get() = TestVoxRepository(args = VoxRepoArgsHolder.args)

    override val appStoreChecker: IAppStoreChecker = TestAppStoreChecker

    override val app2appAgent: IApp2AppAgent by lazy {
        App2AppAgent(
            repo = voxRepository,
            pushTokenRepository = pushTokenRepository,
            userRepository = userRepository,
            isDevOrDebug = true,
            legacyNetworkInfoRepository = legacyNetworkInfoRepository,
            phoneRepo = phoneRepository,
            voxPublicApiRepository = voxPublicApiRepository,
            app2AppAnalyst = app2AppAnalyst,
            systemInfoRepository = systemInfoRepository
        )
    }

    override val pushTokenRepository: IPushTokenRepository by lazy {
        TestPushTokenRepository()
    }

    override val panoramaFrameLoader: IPanoramaFramesLoader by lazy {
        object : IPanoramaFramesLoader {
            override fun loadPanoramaFrames(url: String): Observable<PanoramaFrames> =
                Observable.just(PanoramaFrames(url, emptyList()))
        }
    }

    override val sellerOnboardingRepository: ISafeDealSellerOnboardingRepository by lazy {
        testSafeDealSellerOnboardingRepository
    }


    override val safeDealPromoOverlayRepository = testSafeDealPromoOfferOverlayRepository

    override val loanCabinetFragmentFactory: ILoanCabinetFragmentFactory by lazy { TestLoanCabinetFragmentFactory() }

    override val onboardingShowingRepository by lazy {
        val delegate = OnboardingShowingRepository(preferences)
        TestOnboardingShowingRepository(delegate)
    }

    override val exteriorPanoramaPlayerRepository: IExteriorPanoramaPlayerRepository = testExteriorPanoramaPlayerRepository

    override val panoramaRecorderSettings: PanoramaRecorderSettings
        get() = testPanoramaRecorderSettings

    override val cameraDelegateFactory: ICameraDelegateFactory by lazy { TestCameraDelegateFactory() }


    override val systemOrientationAbilityChecker: ISystemOrientationAbilityChecker by lazy {
        object : ISystemOrientationAbilityChecker {
            override fun isSystemOrientationEnabled(): Boolean = testIsSystemOrientationEnabled
        }
    }

    override val yaPlusRepository: IYaPlusRepository by lazy { TestYaPlusRepo() }

    override val metricaAnalyst: MetricaAnalyst = MetricaAnalystMock()

    override val downloadReportRepository: IDownloadReportRepository by lazy { DownloadReportRepositoryMock() }

    override val micPromoInteractor: IMicPromoInteractor by lazy {
        val domainInteractor: IMicPromoDomainInteractor = TestMicPromoDomainInteractor(TestMicPromoArgsHolder.args)
        val dialogOpener: IMicPromoDialogOpener = MicPromoDialogOpener(domainInteractor)
        object : IMicPromoInteractor, IMicPromoDialogOpener by dialogOpener, IMicPromoDomainInteractor by domainInteractor {}
    }

    override val yandexUpdater: Updater
        get() = testYandexUpdater ?: super.yandexUpdater

    override val networkInfoRepository: INetworkInfoRepository = TestNetworkInfoRepository

    override val trustPaymentRepository: ITrustPaymentRepository = TestTrustPaymentRepository

    override val trustPaymentControllerFactory: ITrustPaymentController.Factory = TestTrustPaymentControllerFactory

    companion object {
        val testExteriorPanoramaPlayerRepository = TestExteriorPanoramaPlayerRepository()

        var testPanoramaRecorderSettings = PanoramaRecorderSettings(
            maxVideoDurationInMillis = 1000L,
            minVideoDurationInMillis = 400L,
        )

        var testIsSystemOrientationEnabled = false

        var testYandexUpdater: Updater? = null

        val testSafeDealSellerOnboardingRepository = TestSafeDealSellerOnboardingRepository()
        val testSafeDealPromoOfferOverlayRepository = TestSafeDealPromoOfferOverlayRepository()

        val testRandom = TestRandom()
    }

}

data class TestMainModuleArguments(
    val testExperiments: TestExperimentsManager = TestExperimentsManager(),
)

package ru.yandex.market.toxin

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.Player
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.github.classgraph.ClassGraph
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.activity.order.RepeatOrderUseCase
import ru.yandex.market.analitycs.AnalyticsService
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.base.database.SQLiteDatabaseWrapper
import ru.yandex.market.base.network.common.proxy.AbstractSSLSocketFactoryProvider
import ru.yandex.market.base.network.common.proxy.AbstractTrustManagerFactory
import ru.yandex.market.base.network.fapi.contract.processor.FapiHealthReporter
import ru.yandex.market.base.network.fapi.contract.processor.FapiPerformanceReporter
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestLogger
import ru.yandex.market.base.presentation.core.mvp.MvpDelegate
import ru.yandex.market.checkout.data.mapper.BucketInfo2Mapper
import ru.yandex.market.clean.data.fapi.FrontApiDataSource
import ru.yandex.market.clean.data.fapi.source.region.RegionFapiClient
import ru.yandex.market.clean.data.fapi.source.regions.delivery.RegionsDeliveryFapiClient
import ru.yandex.market.clean.data.fapi.source.regions.suggestion.RegionSuggestionsFapiClient
import ru.yandex.market.clean.data.repository.RegionsRepository
import ru.yandex.market.clean.data.repository.cashback.GetCashbackBalanceJobExecutor
import ru.yandex.market.clean.data.repository.cashback.WelcomeCashbackInfoJobExecutor
import ru.yandex.market.clean.data.repository.order.OrderConsultationRepository
import ru.yandex.market.clean.data.store.StartupConfigDataStore
import ru.yandex.market.clean.data.store.product.WishListItemFromSkuDataStore
import ru.yandex.market.clean.domain.usecase.GetCurrentRegionUseCase
import ru.yandex.market.clean.domain.usecase.cart.GetOfferFromCacheOrRemoteUseCase
import ru.yandex.market.clean.domain.usecase.order.ValidateOrderUseCase
import ru.yandex.market.clean.domain.usecase.realtimesignal.RealtimeUserSignalLoggingUseCase
import ru.yandex.market.clean.presentation.feature.cartbutton.home.CartButtonUseCases
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.cache.registry.CacheRegistryFacade
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.common.network.fapi.FapiEndpoints
import ru.yandex.market.common.toxin.app.scopes.coreScope
import ru.yandex.market.data.deeplinks.DeeplinkParser
import ru.yandex.market.di.toxin.setupToxinAppScope
import ru.yandex.market.di.toxin.setupToxinCoreScope
import ru.yandex.market.di.toxin.setupToxinLaunchScope
import ru.yandex.market.domain.auth.repository.MarketUidRepository
import ru.yandex.market.domain.auth.repository.YandexUidRepository
import ru.yandex.market.domain.cashback.repository.CashBackAboutRepository
import ru.yandex.market.domain.catalog.usecase.CheckIsLeafCategoryUseCase
import ru.yandex.market.domain.order.usecase.ObserveAnyOrderChangedUseCase
import ru.yandex.market.domain.questions.repository.QuestionsRepository
import ru.yandex.market.domain.reviews.repository.ReviewsRepository
import ru.yandex.market.domain.user.video.repository.ProductUserVideoRepository
import ru.yandex.market.fragment.main.profile.AuthStateController
import ru.yandex.market.repository.OrderRepository
import ru.yandex.market.toxin.report.ToxinConsistencyReportFormatter
import ru.yandex.market.toxin.report.ToxinOverrideReportFormatter
import ru.yandex.market.toxin.stubbing.ToxinConstructorStubbing
import ru.yandex.market.toxin.stubbing.ToxinGetterStubbing
import ru.yandex.market.ui.view.mvp.cartcounterbutton.hyperlocal.CartCounterHyperlocalUseCases
import ru.yandex.market.ui.view.mvp.cartcounterbutton.order.CartCounterOrderUseCases
import ru.yandex.video.player.YandexPlayer
import toxin.Component
import toxin.Injector
import toxin.Instrumentation
import toxin.definition.DefinitionRegistrar
import toxin.middleware.FlexibleInterceptor
import toxin.module
import toxin.tools.verifiers.ConsistencyVerifier
import toxin.tools.verifiers.OverrideVerifier
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MarketToxinTest {

    @Before
    fun setUp() {
        Instrumentation.interceptor = FlexibleInterceptor.Builder()
            // Требует присоединенного контекста
            .interceptType<RequestManager> { mock() }
            // Требует запуск жизненного цикла фрагмента
            .interceptType<MvpDelegate<*>> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<DeeplinkParser> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<FrontApiDataSource> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<AnalyticsService> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<CheckIsLeafCategoryUseCase> { mock() }
            // TODO Венести из даггера и сделать мосты https://st.yandex-team.ru/BLUEMARKETAPPS-40096
            .interceptType<WishListItemFromSkuDataStore> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<ObserveAnyOrderChangedUseCase> { mock() }
            // Не вынести из-за завязок на капи
            .interceptType<GetCurrentRegionUseCase> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<CartButtonUseCases> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<CartCounterHyperlocalUseCases> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<CartCounterOrderUseCases> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<GetOfferFromCacheOrRemoteUseCase> { mock() }
            // Падает код в конструкторе
            .interceptType<CacheRegistryFacade> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<YandexPlayer<Player>> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<OrderRepository> { mock() }
            // Роболектрик не может создать базу на MacBook с процессором M1
            .interceptType<SQLiteDatabaseWrapper> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<RegionsRepository> { mock() }
            // Не вынести из-за разной реализации в main и androidTest
            .interceptType<RegionsDeliveryFapiClient> { mock() }
            // Не вынести из-за разной реализации в main и androidTest
            .interceptType<RegionSuggestionsFapiClient> { mock() }
            // Не вынести из-за разной реализации в main и androidTest
            .interceptType<RegionFapiClient> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<ValidateOrderUseCase> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<BucketInfo2Mapper> { mock() }
            // Не вынести из-за большого количества зависимостей
            .interceptType<YandexUidRepository> { mock() }
            // TODO: Техдолг https://st.yandex-team.ru/BLUEMARKETAPPS-43714
            .interceptType<RepeatOrderUseCase> { mock() }
            // MessengerSdk начинает создавать MessengerHost, в который пытаются заинжектиться зависимости из даггера
            .interceptType<OrderConsultationRepository> { mock() }
            .build()
        setupToxinCoreScope(ApplicationProvider.getApplicationContext())
        setupToxinLaunchScope()
        setupToxinAppScope(
            coreScope,
            overridesModule = module {
                techDebtMocks()
                testRuntimeMocks()
            }
        )
    }

    @Test
    fun `Check all component methods are resolvable`() {
        val components = collectAllComponentAndInjectors()
        val consistencyVerifier = ConsistencyVerifier()
        val constructorStubbing = ToxinConstructorStubbing()
        val getterStubbing = ToxinGetterStubbing()
        val errors = components.flatMap { component ->
            consistencyVerifier.verify(
                componentClass = component,
                getterFilter = { method -> !isJacocoInit(method) },
                constructorStubbing = constructorStubbing,
                getterStubbing = getterStubbing,
            )
        }

        if (errors.isNotEmpty()) {
            System.err.println(ToxinConsistencyReportFormatter().format(errors))
            throw RuntimeException("Verification failed with ${errors.size} errors (see details above)")
        }
    }

    @Test
    fun `Check all overrides are allowed`() {
        val components = collectAllComponentAndInjectors()
        val overrideVerifier = OverrideVerifier()
        val constructorStubbing = ToxinConstructorStubbing()
        val errors = components.flatMap { component ->
            overrideVerifier.verify(
                componentClass = component,
                constructorStubbing = constructorStubbing
            )
        }

        if (errors.isNotEmpty()) {
            System.err.println(ToxinOverrideReportFormatter().format(errors))
            throw RuntimeException("Verification failed with ${errors.size} errors (see details above)")
        }
    }

    private fun collectAllComponentAndInjectors(): List<Class<out Component>> {
        val scanResult = ClassGraph()
            .enableClassInfo()
            .ignoreClassVisibility()
            .acceptPackages("ru.yandex.market", "flex")
            .filterClasspathElements { element -> isReclaimedClasspath(element) }
            .setMaxBufferedJarRAMSize(BUFFERED_JAR_SIZE)
            .scan()

        val components = scanResult.getSubclasses(Component::class.java).loadClasses()
        val injectors = scanResult.getSubclasses(Injector::class.java).loadClasses()
        return (components + injectors)
            .map { clazz -> clazz as Class<Component> }
            .also { require(it.isNotEmpty()) }
    }

    private fun isReclaimedClasspath(element: String): Boolean {
        return isJarClasspath(element)
            && !isResClasspath(element)
            && !isExternalDependencyClasspath(element)
            && !isJetifiedClasspath(element)
    }

    private fun isExternalDependencyClasspath(element: String): Boolean {
        return element.contains("/.gradle/")
    }

    private fun isJarClasspath(element: String): Boolean {
        return element.endsWith(".jar")
    }

    private fun isJetifiedClasspath(element: String): Boolean {
        return element.contains("/jetified-")
    }

    private fun isResClasspath(element: String): Boolean {
        return element.endsWith("/res.jar") || element.endsWith("/R.jar")
    }

    private fun isJacocoInit(method: Method): Boolean {
        return method.name.contains("jacocoInit")
    }

    /**
     * Моки связанные с техническим долгом, предполагается что будут удалены со временем.
     */
    private fun DefinitionRegistrar.techDebtMocks() {
        // Легаси фапи не позволяет вынести из dagger-графа
        factory<QuestionsRepository>(allowOverride = true) { mock() }
        // Легаси фапи не позволяет вынести из dagger-графа
        factory<ReviewsRepository>(allowOverride = true) { mock() }
        // Прямое использование okhttp под капотом, надо переписывать
        factory<ProductUserVideoRepository>(allowOverride = true) { mock() }
        // Использование legacy AuthenticationUseCase
        factory<AuthStateController>(allowOverride = true) { mock() }
        // Использование капи под капотом
        factory<StartupConfigDataStore>(allowOverride = true) { mock() }
        // Использование флейворов, можно выпилить после отказа от них
        factory<AbstractTrustManagerFactory>(allowOverride = true) { mock() }
        // Использование флейворов, можно выпилить после отказа от них
        factory<AbstractSSLSocketFactoryProvider>(allowOverride = true) { mock() }
        // Легаси здоровье не позволяет вынести из dagger-графа
        factory<MarketUidRepository>(allowOverride = true) { mock() }
        // Легаси здоровье не позволяет вынести из dagger-графа
        factory<MetricaSender>(allowOverride = true) { mock() }
        // Навигация не до конца вынесена из dagger-графа
        factory<FapiRequestLogger>(allowOverride = true) { mock() }
        // Использование флейворов, можно выпилить после отказа от них
        factory<FapiEndpoints>(allowOverride = true) { mock() }
        // Использование старого подхода к здоровью
        factory<FapiPerformanceReporter>(allowOverride = true) { mock() }
        // Использование старого подхода к аналитике
        factory<FapiHealthReporter>(allowOverride = true) { mock() }
        // Использование старого FrontApiDataSource, удалить после переезда на cashback-data модуль
        factory<GetCashbackBalanceJobExecutor>(allowOverride = true) { mock() }
        factory<WelcomeCashbackInfoJobExecutor>(allowOverride = true) { mock() }
        factory<CashBackAboutRepository>(allowOverride = true) { mock() }

        factory<ResourcesManager>(allowOverride = true) { mock() }

        factory<DateFormatter>(allowOverride = true) { mock() }
        // Зависит от RegionsRepository, которые все еще не в toxin
        factory<RealtimeUserSignalLoggingUseCase>(allowOverride = true) { mock() }
    }

    /**
     * Моки связанные со спецификой рантайма тестов, не предполагается что будут удалены.
     */
    private fun DefinitionRegistrar.testRuntimeMocks() {
        // Требует инициализацию приложения
        factory<FirebaseRemoteConfig>(allowOverride = true) { mock() }
        // Выкидывает исключение в тестах
        factory<OkHttpClient>(allowOverride = true) { mock() }
        factory<ResourcesManager>(allowOverride = true) { mock() }
        factory<DateFormatter>(allowOverride = true) { mock() }
    }

    companion object {
        private const val BUFFERED_JAR_SIZE = 16 * 1024 * 1024
    }
}

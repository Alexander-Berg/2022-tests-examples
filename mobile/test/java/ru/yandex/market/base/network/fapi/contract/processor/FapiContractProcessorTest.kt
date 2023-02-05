package ru.yandex.market.base.network.fapi.contract.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.base.network.common.exception.NetworkUnavailableException
import ru.yandex.market.base.network.fapi.FapiEndpoint
import ru.yandex.market.base.network.fapi.FapiVersion
import ru.yandex.market.base.network.fapi.cache.FapiCacheManager
import ru.yandex.market.base.network.fapi.cache.FapiContractCachePolicy
import ru.yandex.market.base.network.fapi.connection.FapiConnectionChecker
import ru.yandex.market.base.network.fapi.contract.FapiContract
import ru.yandex.market.base.network.fapi.request.FapiRequestMeta
import ru.yandex.market.base.network.fapi.request.executor.FapiJsonContractSplitter
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestContext
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestContextProvider
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestExecutorImpl
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestResult
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.minutes
import java.lang.RuntimeException

class FapiContractProcessorTest {

    private val fapiEndpoint = FapiEndpoint { "" }
    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentUtcTimeInMillis } doReturn TIMESTAMP
    }
    private val contextProvider = mock<FapiRequestContextProvider>()
    private val requestExecutor = mock<FapiRequestExecutorImpl>()
    private val connectionChecker = mock<FapiConnectionChecker>()
    private val cacheManager = mock<FapiCacheManager>()
    private val healthReporter = mock<FapiHealthReporter>()
    private val performanceReporter = mock<FapiPerformanceReporter>()
    private val contractSplitter = FapiJsonContractSplitter()
    private val processor = FapiContractProcessor(
        dateTimeProvider = dateTimeProvider,
        requestContextProvider = contextProvider,
        requestExecutor = requestExecutor,
        connectionChecker = connectionChecker,
        cacheManager = cacheManager,
        healthReporter = healthReporter,
        performanceReporter = performanceReporter,
        contractSplitter = contractSplitter
    )

    @Test
    fun `Processor checks cache for contract if cache enabled`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("someResolver")
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.Success(
                requestMeta = requestMeta,
                data = "Hello, world!"
            )
        )

        processor.process(fapiEndpoint, contract)

        verify(cacheManager).readFromCache(contract, policy, context)
    }

    @Test
    fun `Processor does not check cache for contract if cache disabled`() {
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()

        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.Success(
                requestMeta = requestMeta,
                data = "Hello, world!"
            )
        )
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )

        processor.process(fapiEndpoint, contract)

        verify(cacheManager, never()).readFromCache(any(), any(), any())
    }

    @Test
    fun `Processor returns cached value if exists`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("someResolver")
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val cachedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = "Hello, world!"
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(cacheManager.readFromCache(contract, policy, context)) doReturn cachedResult

        val result = processor.process(fapiEndpoint, contract)

        assertThat(result).isEqualTo(cachedResult)
    }

    @Test
    fun `Processor do not call request executor if cached value exists`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("someResolver")
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val cachedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = "Hello, world!"
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(cacheManager.readFromCache(contract, policy, context)) doReturn cachedResult

        processor.process(fapiEndpoint, contract)

        verify(requestExecutor, never()).executeJsonRequest(any(), any(), any(), any())
    }

    @Test
    fun `Processor returns value from request executor if cache disabled`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = disabledCachePolicy()
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val resultValue = "Hello, world!"
        val requestedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = resultValue
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(requestExecutor.executeJsonRequest(context, fapiEndpoint, version, listOf(contract))) doReturn mapOf(
            contract to requestedResult
        )

        val result = processor.process(fapiEndpoint, contract)

        assertThat(result).isEqualTo(resultValue)
        verify(requestExecutor).executeJsonRequest(context, fapiEndpoint, version, listOf(contract))
    }

    @Test
    fun `Processor returns value from request if cache missing`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("someResolver")
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val resultValue = "Hello, world!"
        val requestedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = resultValue
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(requestExecutor.executeJsonRequest(context, fapiEndpoint, version, listOf(contract))) doReturn mapOf(
            contract to requestedResult
        )
        whenever(cacheManager.readFromCache(contract, policy, context)) doReturn null

        val result = processor.process(fapiEndpoint, contract)

        assertThat(result).isEqualTo(resultValue)
        verify(requestExecutor).executeJsonRequest(context, fapiEndpoint, version, listOf(contract))
    }

    @Test
    fun `Processor updates cache after request if cache enabled`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("someResolver")
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val resultValue = "Hello, world!"
        val requestedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = resultValue
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(requestExecutor.executeJsonRequest(context, fapiEndpoint, version, listOf(contract))) doReturn mapOf(
            contract to requestedResult
        )

        processor.process(fapiEndpoint, contract)

        verify(cacheManager).writeToCache(contract, policy, context, resultValue)
    }

    @Test
    fun `Processor do not updates cache after request if cache disabled`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = disabledCachePolicy()
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()
        val resultValue = "Hello, world!"
        val requestedResult = FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = resultValue
        )

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(requestExecutor.executeJsonRequest(context, fapiEndpoint, version, listOf(contract))) doReturn mapOf(
            contract to requestedResult
        )

        processor.process(fapiEndpoint, contract)

        verify(cacheManager, never()).writeToCache(any(), any(), any(), any())
    }

    @Test
    fun `Processor do not updates cache if result exists in cache`() {
        val context = FapiRequestContext(emptyMap(), emptyMap())
        val policy = enabledCachePolicy("somResolver")
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn policy
        }
        val requestMeta = FapiRequestMeta()

        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn context
        whenever(cacheManager.readFromCache(contract, policy, context)) doReturn FapiRequestResult.Success(
            requestMeta = requestMeta,
            data = "Hello, world!"
        )

        processor.process(fapiEndpoint, contract)

        verify(cacheManager, never()).writeToCache(any(), any(), any(), any())
    }

    @Test
    fun `Processor reports performance metrics if result requested`() {
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.Success(
                requestMeta = requestMeta,
                data = "Hello, world!"
            )
        )

        processor.process(fapiEndpoint, contract)

        verify(performanceReporter).reportPerformanceMetrics(contract, TIMESTAMP, TIMESTAMP, false, requestMeta)
    }

    @Test
    fun `Processor reports performance metrics if result from cache`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn enabledCachePolicy("someResolver")
        }
        whenever(cacheManager.readFromCache(any(), any(), any())) doReturn "Hello, world!"
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        processor.process(fapiEndpoint, contract)

        verify(performanceReporter).reportPerformanceMetrics(contract, TIMESTAMP, TIMESTAMP, true, null)
    }

    @Test
    fun `Connection checks if executor returns client error`() {
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(connectionChecker.hasConnection(any())) doReturn true
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ClientError(
                requestMeta = requestMeta,
                exception = RuntimeException()
            )
        )
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(connectionChecker, times(1)).hasConnection(any())
    }

    @Test
    fun `Connection don't checks if executor returns server error`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(connectionChecker.hasConnection(any())) doReturn true
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ServerError(
                requestMeta = requestMeta,
                message = null
            )
        )

        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(connectionChecker, never()).hasConnection(any())
    }

    @Test
    fun `Connection don't checks if executor returns success`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(connectionChecker.hasConnection(any())) doReturn true
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ServerError(
                requestMeta = requestMeta,
                message = null
            )
        )

        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(connectionChecker, never()).hasConnection(any())
    }

    @Test
    fun `Processor don't reports performance metrics if connection is not available`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(connectionChecker.hasConnection(any())) doReturn false
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ClientError(
                requestMeta = requestMeta,
                exception = RuntimeException()
            )
        )

        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(performanceReporter, never()).reportPerformanceMetrics(any(), any(), any(), any(), any())
    }

    @Test(expected = FapiProcessingException::class)
    fun `Processor propagates client errors`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        val exception = RuntimeException()
        whenever(connectionChecker.hasConnection(any())) doReturn true
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ClientError(
                requestMeta = requestMeta,
                exception = exception
            )
        )

        processor.process(fapiEndpoint, contract)
    }

    @Test(expected = NetworkUnavailableException::class)
    fun `Processor throws network errors is connection is not available`() {
        val version = "v1"
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { version }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        val exception = RuntimeException()
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        whenever(connectionChecker.hasConnection(any())) doReturn false
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ClientError(
                requestMeta = requestMeta,
                exception = exception
            )
        )

        processor.process(fapiEndpoint, contract)
    }

    @Test(expected = FapiProcessingException::class)
    fun `Processor propagates server errors`() {
        val contract = mock<FapiContract<Any>> {
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ServerError(
                requestMeta = requestMeta,
                message = null
            )
        )

        processor.process(fapiEndpoint, contract)
    }

    @Test
    fun `Processor reports client errors`() {
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        val exception = RuntimeException()
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ClientError(
                requestMeta = requestMeta,
                exception = exception
            )
        )
        whenever(connectionChecker.hasConnection(any())) doReturn true
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(healthReporter).reportClientFailure(contract, requestMeta, exception, true)
    }

    @Test
    fun `Processor reports server errors`() {
        val contract = mock<FapiContract<Any>> {
            on { apiVersion } doReturn FapiVersion { "v1" }
            on { cachePolicy } doReturn disabledCachePolicy()
        }
        val requestMeta = FapiRequestMeta()
        val message = "Oops!"
        whenever(requestExecutor.executeJsonRequest(any(), any(), any(), any())) doReturn mapOf(
            contract to FapiRequestResult.ServerError(
                requestMeta = requestMeta,
                message = message
            )
        )
        whenever(contextProvider.getRequestContext(listOf(contract))) doReturn FapiRequestContext(
            emptyMap(),
            emptyMap()
        )
        try {
            processor.process(fapiEndpoint, contract)
        } catch (e: Exception) {
            // skip exception
        }

        verify(healthReporter).reportServerFailure(contract, requestMeta, message)
    }

    companion object {

        private const val TIMESTAMP = 274892L

        private fun enabledCachePolicy(resolver: String): FapiContractCachePolicy.Enabled {
            return FapiContractCachePolicy.Enabled(
                resolver = resolver,
                customResolverParameters = emptyMap(),
                includeContextParams = emptySet(),
                includeContextHeaders = emptySet(),
                lifeTime = 10.minutes,
                resultType = String::class.java
            )
        }

        private fun disabledCachePolicy(): FapiContractCachePolicy.Disabled {
            return FapiContractCachePolicy.Disabled()
        }
    }
}
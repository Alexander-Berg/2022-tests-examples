package com.yandex.mail.fakeserver

import com.yandex.mail.LoginData
import com.yandex.mail.tools.MockNetworkTools
import com.yandex.passport.api.exception.PassportAccountNotFoundException
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.net.HttpURLConnection

class FakeServer private constructor() {

    companion object {
        private val internalInstance: FakeServer by lazy {
            FakeServer()
        }

        @JvmStatic
        fun getInstance(): FakeServer {
            return internalInstance
        }

        @JvmStatic
        fun reset() = internalInstance.apply {
            accountsMap.clear()
            files.reset()
        }
    }

    val files = ServerFilesWrapper()

    lateinit var baseUrl: HttpUrl // should be set via setBaseUrl

    private val accountsMap = mutableMapOf<LoginData, AccountWrapper>()

    private var unauthorizedRequestsDispatcher: Dispatcher

    val handledRequests = mutableListOf<RecordedRequest>()

    init {
        unauthorizedRequestsDispatcher = buildApiCallsMapDispatcher(
            // add unauthorized calls here
        )
    }

    // Expects format "OAuth: token"
    // must be a file request
    @Suppress("ThrowRuntimeException") // We should handle all exceptions to translate it into HTTP_INTERNAL_ERROR.
    val dispatcher: Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            handledRequests += request
            try {
                return when (request.requestUrl!!.pathSegments.first()) {
                    "api" -> {
                        val authorizationString = request.getHeader("Authorization")
                        if (authorizationString != null) {
                            handleAuthorizedRequest(request, authorizationString)
                        } else {
                            handleUnauthorizedRequest(request)
                        }
                    }
                    else -> files.fileResponseRule.getResponse(request)
                }
            } catch (e: Exception) {
                // using System.err is deliberate here! Using Logger.e leads to some crazy shit like hanging tests.
                System.err.println("Unexpected exception while handling request")
                e.printStackTrace()
                return MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            }
        }
    }

    private fun handleAuthorizedRequest(request: RecordedRequest, authorizationString: String): MockResponse {
        val token = authorizationString.split(" ")[1]
        val foundKey = accountsMap.keys.find { it.token == token }

        return accountsMap[foundKey]
            ?.dispatchNetworkRequest(request)
            ?: throw RuntimeException("No account corresponding to token $token")
    }

    private fun handleUnauthorizedRequest(request: RecordedRequest) =
        unauthorizedRequestsDispatcher.dispatch(request)

    fun createAccountWrapper(loginData: LoginData): AccountWrapper {
        return createAccountWrapper(loginData, false)
    }

    fun createAccountWrapper(loginData: LoginData, areTabsEnabled: Boolean): AccountWrapper {
        val wrapper = AccountWrapper(this, loginData, areTabsEnabled)
        accountsMap.put(loginData, wrapper)
        return wrapper
    }

    fun getAccountWrapperByName(name: String): AccountWrapper {
        val found = accountsMap.entries.find { (key, _) -> key.name == name }
        return found?.value ?: throw IllegalArgumentException("No account wrapper found for name $name")
    }

    fun getAccountWrapperByUid(uid: Long): AccountWrapper {
        val found = accountsMap.entries.find { (key, _) -> key.uid == uid }
        return found?.value
            ?: throw PassportAccountNotFoundException("No account wrapper found for uid $uid")
    }

    fun wrapPath(path: String) = baseUrl.newBuilder().addPathSegment(path).build()

    private fun buildApiCallsMapDispatcher(vararg entries: Pair<String, MockWebServerResponseRule>): Dispatcher {
        val rulesMap = HashMap<String, MockWebServerResponseRule>()
        for (entry in entries) {
            val method = entry.first
            val rule = entry.second
            rulesMap.put(method, rule)
        }

        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val method = MockNetworkTools.getMethod(request)

                val rule = rulesMap[method] ?: throw RuntimeException("No response rule found for method $method")
                return rule.getResponse(request)
            }
        }
    }
}

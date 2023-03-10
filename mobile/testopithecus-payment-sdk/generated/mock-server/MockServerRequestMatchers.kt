// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mock-server/mock-server-request-matchers.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public interface MockServerRequestMatcher: HttpRequestHandler {
    fun canHandleRequest(request: HttpRequest): Boolean
}

public open class MockServerRequestMatchers {
    companion object {
        @JvmStatic
        open fun path(path: String, handler: HttpRequestHandler): MockServerPathRequestMatcher {
            return MockServerPathRequestMatcher(path, handler)
        }

        @JvmStatic
        open fun pathPrefix(pathPrefix: String, handler: HttpRequestHandler): MockServerPathPrefixRequestMatcher {
            return MockServerPathPrefixRequestMatcher(pathPrefix, handler)
        }

    }
}

public abstract class MockServerAbstractRequestMatcher(private val handler: HttpRequestHandler): MockServerRequestMatcher {
    abstract override fun canHandleRequest(request: HttpRequest): Boolean
    open override fun handleRequest(request: HttpRequest): HttpResponse {
        return this.handler.handleRequest(request)
    }

}

public open class MockServerPathRequestMatcher(private val path: String, handler: HttpRequestHandler): MockServerAbstractRequestMatcher(handler) {
    open override fun canHandleRequest(request: HttpRequest): Boolean {
        val requestPath = nullthrows(Uris.fromString(request.url)?.getPath())
        return this.dropLeadingSlash(this.path) == this.dropLeadingSlash(requestPath)
    }

    private fun dropLeadingSlash(path: String): String {
        return if (path.startsWith("/")) path.slice(1) else path
    }

}

public open class MockServerPathPrefixRequestMatcher(private val pathPrefix: String, handler: HttpRequestHandler): MockServerAbstractRequestMatcher(handler) {
    open override fun canHandleRequest(request: HttpRequest): Boolean {
        val pathComponents = nullthrows(Uris.fromString(request.url)?.getPathSegments())
        return pathComponents.size > 0 && pathComponents[0] == this.pathPrefix
    }

}


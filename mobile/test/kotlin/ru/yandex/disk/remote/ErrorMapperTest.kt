package ru.yandex.disk.remote

import com.yandex.disk.rest.exceptions.http.HttpCodeException
import com.yandex.disk.rest.json.ApiError
import org.junit.Test
import ru.yandex.disk.remote.exceptions.BadCarmaException

class ErrorMapperTest {

    @Test(expected = BadCarmaException::class)
    fun `should convert HttpCodeException 403 BadCarma to BadCarmaException`() {
        val httpException = HttpCodeException(403, object : ApiError() {
            override fun getError(): String {
                return "BadKarmaError"
            }
        })
        ErrorMapper.rethrowRemoteExecutionException<Any>(httpException)
    }

}
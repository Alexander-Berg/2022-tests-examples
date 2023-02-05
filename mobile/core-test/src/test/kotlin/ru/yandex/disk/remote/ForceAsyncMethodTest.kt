package ru.yandex.disk.remote

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.hamcrest.core.AnyOf.anyOf
import org.junit.Test
import ru.yandex.disk.remote.MockResponses.RESPONSE_IN_PROGRESS
import ru.yandex.util.Path


class ForceAsyncMethodsTest : BaseRemoteRepoMethodTest() {

    override fun setUp() {
        super.setUp()
        fakeOkHttpInterceptor.addResponse(202, RESPONSE_IN_PROGRESS)
    }

    @Test
    fun `should send correct delete request`() {
        remoteRepo.delete(FILE_1, true)
        checkThatQueryIsAsync()
    }

    @Test
    fun `should send correct copy request`() {
        remoteRepo.copy(PATH_1, PATH_2, true)
        checkThatQueryIsAsync()
    }

    @Test
    fun `should send correct move request`() {
        remoteRepo.move(PATH_1, PATH_2, true)
        checkThatQueryIsAsync()
    }

    @Test
    fun `should not force async in default move call`() {
        remoteRepo.move(PATH_1, PATH_2)
        checkThatQueryIsNotAsync()
    }

    @Test
    fun `should not force async in default copy call`() {
        remoteRepo.copy(PATH_1, PATH_2)
        checkThatQueryIsNotAsync()
    }

    private fun checkThatQueryIsAsync() {
        val query = fakeOkHttpInterceptor.requestQuery
        assertThat(query[FORCE_ASYNC], equalTo("true"))
    }

    private fun checkThatQueryIsNotAsync() {
        val query = fakeOkHttpInterceptor.requestQuery
        assertThat(query[FORCE_ASYNC], anyOf(nullValue(), equalTo("false")))
    }

    companion object {

        private val FORCE_ASYNC = "force_async"
        private val FILE_1 = "/disk/testFile.txt"
        private val FILE_2 = "/disk/testFile2.txt"
        private val PATH_1 = Path(FILE_1)
        private val PATH_2 = Path(FILE_2)
    }

}

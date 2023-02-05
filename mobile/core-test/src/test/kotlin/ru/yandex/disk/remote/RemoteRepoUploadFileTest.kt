package ru.yandex.disk.remote

import org.mockito.kotlin.mock
import com.yandex.disk.rest.json.Link
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.remote.exceptions.BadCarmaException
import ru.yandex.disk.remote.exceptions.ConflictException
import ru.yandex.disk.remote.exceptions.FileTooBigServerException
import ru.yandex.disk.remote.exceptions.NotAuthorizedException
import ru.yandex.disk.remote.exceptions.NotFoundException
import ru.yandex.disk.remote.exceptions.PreconditionFailedException
import ru.yandex.disk.remote.exceptions.RemoteExecutionException
import ru.yandex.disk.remote.exceptions.ServerUnavailableException
import ru.yandex.disk.remote.webdav.InsufficientStorageException

import java.io.File
import java.io.IOException

import org.mockito.Mockito.*

@Config(manifest = Config.NONE)
class RemoteRepoUploadFileTest : BaseRemoteRepoMethodTest() {

    @Test(expected = ConflictException::class)
    @Throws(Exception::class)
    fun shouldThrowConflictException() {
        fakeOkHttpInterceptor.addResponse(409)
        performMockedRequest()
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun shouldThrowNotFoundException() {
        fakeOkHttpInterceptor.addResponse(404)
        performMockedRequest()
    }

    @Test(expected = PreconditionFailedException::class)
    @Throws(Exception::class)
    fun shouldThrowPreconditionFailedException() {
        fakeOkHttpInterceptor.addResponse(412)
        performMockedRequest()
    }

    @Test(expected = FileTooBigServerException::class)
    @Throws(Exception::class)
    fun shouldThrowFileTooBigServerException() {
        fakeOkHttpInterceptor.addResponse(413)
        performMockedRequest()
    }

    @Test(expected = ServerUnavailableException::class)
    @Throws(Exception::class)
    fun shouldThrowServerUnavailableException() {
        fakeOkHttpInterceptor.addResponse(503)
        performMockedRequest()
    }

    @Test(expected = InsufficientStorageException::class)
    @Throws(Exception::class)
    fun shouldThrowInsufficientStorageException() {
        fakeOkHttpInterceptor.addResponse(507)
        performMockedRequest()
    }

    @Test(expected = NotAuthorizedException::class)
    @Throws(Exception::class)
    fun shouldThrowNotAuthorizedException() {
        fakeOkHttpInterceptor.addResponse(401)
        performMockedRequest()
    }

    @Test(expected = BadCarmaException::class)
    @Throws(Exception::class)
    fun shouldThrowBadCarmaExceptionException() {
        fakeOkHttpInterceptor.addResponse(402)
        performMockedRequest()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        File(TEST_FILE_NAME).delete()
    }

    @Throws(IOException::class, RemoteExecutionException::class)
    private fun performMockedRequest() {
        val link = mock(Link::class.java)
        `when`(link.href).thenReturn("https://test.url")
        val testFile = File(TEST_FILE_NAME)
        if (!testFile.exists()) {
            testFile.createNewFile()
        }
        remoteRepo.uploadFile(testFile, link, mock())
    }

    companion object {

        private val TEST_FILE_NAME = "testFile"
    }
}

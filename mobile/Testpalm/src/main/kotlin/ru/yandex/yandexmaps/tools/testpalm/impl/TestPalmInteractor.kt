package ru.yandex.yandexmaps.tools.testpalm.impl

import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface TestPalmInteractor {

    sealed class DownloadResult {
        class Success(val case: TestCase) : DownloadResult()
        class Error(val message: String) : DownloadResult()
    }

    fun downloadTestCase(id: Int): DownloadResult
}

class TestPalmInteractorImpl(
    private val token: String,
    private val projectId: String,
    testPalmHost: String
) : TestPalmInteractor {

    private val getTestCaseUrl = "$testPalmHost/testcases/$projectId?id=%d"

    private val client = HttpClient {
        install(DefaultRequest) {
            header("Authorization", "OAuth $token")
        }
    }

    override fun downloadTestCase(id: Int): TestPalmInteractor.DownloadResult {
        return runBlocking {
            val url = getTestCaseUrl.format(id)
            val response = client.request<HttpResponse>(url)

            return@runBlocking if (!response.status.isSuccess()) {
                TestPalmInteractor.DownloadResult.Error("status: ${response.status}, body: ${response.readText()}")
            } else {
                try {
                    val list = Json { encodeDefaults = false; ignoreUnknownKeys = true }
                        .decodeFromString<List<RawTestCase>>(ListSerializer(RawTestCase.serializer()), response.readText())

                    list.firstOrNull()?.let { TestPalmInteractor.DownloadResult.Success(it.toTestCase(projectId)) }
                        ?: TestPalmInteractor.DownloadResult.Error("Test case $id wasn't found")
                } catch (e: SerializationException) {
                    TestPalmInteractor.DownloadResult.Error("Testcase json serialization error")
                }
            }
        }
    }
}

@Serializable
data class RawTestCase(
    val id: Int,
    val name: String,
    @SerialName("description") val preconditions: String? = null,
    @SerialName("stepsExpects") val steps: List<TestCase.Step>
) {
    private val preconditionsPrefix = "##### Information:\n- "

    fun toTestCase(projectId: String) = TestCase(id, projectId, name, preconditions?.removePrefix(preconditionsPrefix), steps)
}

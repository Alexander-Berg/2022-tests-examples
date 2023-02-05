package ru.yandex.market.base.network.fapi.request.executor

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.junit.Test
import ru.yandex.market.base.network.fapi.FapiVersion
import ru.yandex.market.base.network.fapi.contract.FapiContract
import ru.yandex.market.base.network.fapi.extractor.FapiCollectionPromise
import ru.yandex.market.base.network.fapi.extractor.FapiExtractException
import ru.yandex.market.base.network.fapi.extractor.FapiExtractorContext
import ru.yandex.market.base.network.fapi.extractor.FapiExtractorDeclaration
import ru.yandex.market.base.network.fapi.extractor.extractResult
import ru.yandex.market.base.network.fapi.extractor.extractor
import ru.yandex.market.base.network.fapi.extractor.strategy
import ru.yandex.market.base.network.fapi.parser.FapiParserResult
import ru.yandex.market.base.network.fapi.parser.FapiResponseParser
import ru.yandex.market.base.network.fapi.request.FapiRequestMeta
import ru.yandex.market.datetime.DateTimeProvider

class FapiJsonResponseParserTest {

    private val dateTimeProvider = mock<DateTimeProvider>()
    private val requestMeta = mock<FapiRequestMeta>()
    private val parser = FapiResponseParser(dateTimeProvider)
    private val gson = Gson()

    @Test(expected = FapiExtractException::class)
    fun `Throws exception when contract result parser is not set`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor { strategy { "Hello, world!" } }
            }
        }
        parser.parse(
            requestMeta,
            listOf(contract),
            "".trimIndent().byteInputStream().reader()
        )
    }

    @Test
    fun `Correctly parses single resolver result`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val greeting = "Hello, world!"
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    {
                        "result": "$greeting"
                    }
                ]
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isEqualTo(FapiParserResult.Success(data = greeting))
    }

    @Test
    fun `Correctly parses multiple resolver result`() {
        val firstContract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val secondContract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val greeting1 = "Hello, world!1"
        val greeting2 = "Hello, world!2"
        val outputs = parser.parse(
            requestMeta,
            listOf(firstContract, secondContract),
            """{
                "results": [
                    {
                        "result": "$greeting1"
                    },
                    {
                        "result": "$greeting2"
                    }
                ]
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[firstContract]).isEqualTo(FapiParserResult.Success(data = greeting1))
        assertThat(outputs[secondContract]).isEqualTo(FapiParserResult.Success(data = greeting2))
    }

    @Test
    fun `Returns parse error if response contains more results then contracts passed`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val greeting1 = "Hello, world!1"
        val greeting2 = "Hello, world!2"
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    {
                        "result": "$greeting1"
                    },
                    {
                        "result": "$greeting2"
                    }
                ]
            }""".trimIndent().byteInputStream().reader()
        )

        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.ParseError::class.java)
    }

    @Test
    fun `Returns parse error if response contains less results then contracts passed`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    
                ]
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.ParseError::class.java)
    }

    @Test
    fun `Returns parse error if response does not contains results object`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.ParseError::class.java)
    }

    @Test
    fun `Returns resolver error if resolver result contains error key`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    {
                        "result": "Hi, im result!",
                        "error": "Hi, im error!"
                    }
                ]
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.ResolverError::class.java)
        assertThat((outputs[contract] as FapiParserResult.ResolverError).message).isEqualTo("\"Hi, im error!\"")
    }

    @Test
    fun `Returns parse error if json is not valid`() {
        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    strategy {
                        requireNotNull(resultPromise.get().result)
                    }
                }
            }
        }
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    {
                        "result": "Hi, im result!",
                        "error": "Hi, im error!"
                    }
                ]
            """.trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.ParseError::class.java)
    }

    @Test
    fun `Correctly parse two collections with same type items`() {
        fun FapiExtractorContext.extractUsers1(gson: Gson): FapiCollectionPromise<Map<String, TestUserDto>> {
            return extractCollection("users1", gson)
        }

        fun FapiExtractorContext.extractUsers2(gson: Gson): FapiCollectionPromise<Map<String, TestUserDto>> {
            return extractCollection("users2", gson)
        }

        val contract = object : TestFapiContract() {
            override fun createExtractor(): FapiExtractorDeclaration<String> {
                return extractor {
                    val resultPromise = extractResult<ResolverResult>(gson)
                    val users1Promise = extractUsers1(gson)
                    val users2Promise = extractUsers2(gson)
                    strategy {
                        val users1 = users1Promise.get()
                        val users2 = users2Promise.get()
                        assertThat(users1["1"]).usingRecursiveComparison().isEqualTo(
                            TestUserDto("1", "foo")
                        )
                        assertThat(users2["2"]).usingRecursiveComparison().isEqualTo(
                            TestUserDto("2", "bar")
                        )
                        ""
                    }
                }
            }
        }
        val outputs = parser.parse(
            requestMeta,
            listOf(contract),
            """{
                "results": [
                    {
                        "result": ""
                    }
                ],
                "collections": {
                    "users1": {
                        "1": {
                            "id": "1",
                            "name": "foo"
                        }
                    },
                    "users2": {
                        "2": {
                            "id": "2",
                            "name": "bar"
                        }
                    }
                }
            }""".trimIndent().byteInputStream().reader()
        )
        assertThat(outputs[contract]).isInstanceOf(FapiParserResult.Success::class.java)
    }

    private abstract class TestFapiContract : FapiContract<String>() {
        override val resolverName: String get() = "testResolver"
        override val apiVersion: FapiVersion get() = FapiVersion { "v0" }
    }

    private class ResolverResult(
        @SerializedName("result") val result: String?
    )

    private class TestUserDto(
        @SerializedName("id") val id: String?,
        @SerializedName("name") val name: String?
    )
}
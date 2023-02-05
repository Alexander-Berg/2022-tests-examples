package com.edadeal.android.data

import com.edadeal.android.model.OffersQuery
import com.edadeal.android.model.entity.Compilation
import com.edadeal.android.model.entity.Segment
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class OffersSelectCompilationDelegateTest(
    private val subCompilations: List<Compilation>,
    private val query: OffersQuery,
    private val expected: OffersSelectCompilationDelegate.Result?
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> = listOf(
            arrayOf(
                listOf(compilation(id = "1A2A", parentId = "1A"), compilation(id = "1B2A", parentId = "1B")),
                query(compilationId = "1A"),
                result(subCompilation = compilation(id = "1A2A", parentId = "1A"))
            ),
            arrayOf(
                listOf(compilation(id = "1B2A", parentId = "1B"), compilation(id = "1A2A", parentId = "1A")),
                query(compilationId = "1A2A"),
                result(subCompilation = compilation(id = "1A2A", parentId = "1A"))
            ),
            arrayOf(
                listOf(compilation(id = "1A2A", parentId = "1A"), compilation(id = "1B2A", parentId = "1B")),
                query(compilationId = "1A2A3A"),
                result(
                    subCompilation = compilation(id = "1A2A", parentId = "1A"),
                    selectedCompilation3 = compilation(id = "1A2A3A", parentId = "1A2A")
                )
            ),
            arrayOf(
                listOf(compilation(id = "1B2A", parentId = "1B"), compilation(id = "1A2A", parentId = "1A")),
                query(segmentId = "1B2C"),
                result()
            ),
            arrayOf(
                listOf(compilation(id = "1A2A", parentId = "1A"), compilation(id = "1B2B", parentId = "1B")),
                query(segmentId = "1B2B"),
                result(subCompilation = compilation(id = "1B2B", parentId = "1B"))
            )
        )

        private val compilations = listOf(
            compilation(id = "1A"),
            compilation(id = "1A2A", parentId = "1A"), compilation(id = "1A2B", parentId = "1A"),
            compilation(id = "1A2A3A", parentId = "1A2A"),
            compilation(id = "1B"),
            compilation(id = "1B2A", parentId = "1B"), compilation(id = "1B2B", parentId = "1B")
        )
        private val segments = listOf(
            segment(id = "1A"),
            segment(id = "1A2A", parentId = "1A"), segment(id = "1A2B", parentId = "1A"),
            segment(id = "1B"),
            segment(id = "1B2B", parentId = "1B"), segment(id = "1B2C", parentId = "1B")
        )

        private fun compilation(id: String, parentId: String? = null): Compilation {
            val level = id.length / 2L
            return when (parentId) {
                null -> Compilation.EMPTY.copy(id = id.encodeUtf8(), level = level)
                else -> Compilation.EMPTY.copy(id = id.encodeUtf8(), parentId = parentId.encodeUtf8(), level = level)
            }
        }

        private fun segment(id: String, parentId: String? = null): Segment {
            val level = id.length / 2L
            return when (parentId) {
                null -> Segment.EMPTY.copy(id = id.encodeUtf8(), level = level)
                else -> Segment.EMPTY.copy(id = id.encodeUtf8(), parentId = parentId.encodeUtf8(), level = level)
            }
        }

        private fun query(compilationId: String? = null, segmentId: String? = null): OffersQuery {
            return when {
                compilationId != null -> OffersQuery.INITIAL_QUERY.copy(compilationId = compilationId.encodeUtf8())
                segmentId != null -> OffersQuery.INITIAL_QUERY.copy(segmentId = segmentId.encodeUtf8())
                else -> throw IllegalArgumentException()
            }
        }

        private fun result(
            subCompilation: Compilation? = null,
            selectedCompilation3: Compilation? = null
        ): OffersSelectCompilationDelegate.Result? {
            return subCompilation?.let { OffersSelectCompilationDelegate.Result(subCompilation, selectedCompilation3) }
        }
    }

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Mock
    private lateinit var contentRepository: ContentRepository

    @BeforeTest
    fun prepare() {
        whenever(contentRepository.getCompilation(any())).then { invocation ->
            val id = invocation.arguments.first() as ByteString
            compilations.find { it.id == id }
        }
        whenever(contentRepository.getCompilationsByParentId(any())).then { invocation ->
            val parentId = invocation.arguments.first() as ByteString
            compilations.filter { it.parentId == parentId }
        }
        whenever(contentRepository.getSegment(any())).then { invocation ->
            val id = invocation.arguments.first() as ByteString
            segments.find { it.id == id }
        }
        whenever(contentRepository.getSegmentsByParentId(any())).then { invocation ->
            val parentId = invocation.arguments.first() as ByteString
            segments.filter { it.parentId == parentId }
        }
    }

    @Test
    fun `should select expected compilation`() {
        val delegate = OffersSelectCompilationDelegate(contentRepository)
        assertEquals(expected, delegate.selectCompilationByAppLinkOrDeepLink(subCompilations, query))
    }
}

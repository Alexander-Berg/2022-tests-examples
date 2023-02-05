package io.ktor.utils.io

import kotlin.test.*

class ReadRemainingTest : ByteChannelTestBase() {

    @Test
    fun testImmediateFailurePropagation() = runTest {
        ch.close(ExpectedFailureException())
        assertFailsWith<ExpectedFailureException> {
            ch.readRemaining()
        }
    }

    @Test
    fun testFailureAfterSuspend() = runTest {
        launch {
            expect(1)
            assertFailsWith<ExpectedFailureException> {
                ch.readRemaining()
            }
            expect(3)
        }

        yield()

        expect(2)

        ch.close(ExpectedFailureException())
    }

    @Test
    fun testFailureAfterFewBytes() = runTest {
        launch {
            expect(1)
            assertFailsWith<ExpectedFailureException> {
                ch.readRemaining()
            }
            expect(4)
        }

        yield()

        expect(2)

        ch.writeFully(byteArrayOf(1, 2, 3))
        ch.flush()

        yield()

        expect(3)

        ch.close(ExpectedFailureException())
    }

    @Test
    fun testFailureAfterFewBytesNonFlushed() = runTest {
        launch {
            expect(1)
            assertFailsWith<ExpectedFailureException> {
                ch.readRemaining()
            }
            expect(4)
        }

        yield()

        expect(2)

        ch.writeFully(byteArrayOf(1, 2, 3))

        yield()

        expect(3)

        ch.close(ExpectedFailureException())
    }

    class ExpectedFailureException : Exception()
}

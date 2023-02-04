package ru.auto.data.util

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author dumchev on 31.05.2018.
 */
@RunWith(AllureRunner::class)
class ReorderedStackSetTest {

    private val stack = ReorderedStackSet<Int>()

    @Before
    fun before() {
        stack.clear()
        stack.pushAll(1, CHECKED_NUM)
    }

    @Test
    fun push() {
        val oldList = stack.toList()
        stack.push(CHECKED_NUM)
        check(oldList.size == stack.toList().size) { "reorderedStackSet should've removed $CHECKED_NUM" }
    }

    @Test
    fun pop() {
        val oldSize = stack.size
        val popped = stack.pop()
        check(popped == CHECKED_NUM) { "pop() wrong number: expected $CHECKED_NUM, but was $popped" }
        check(stack.size == oldSize - 1) { "pop() didn't removed item from top" }
    }

    @Test
    fun peek() {
        val oldSize = stack.size
        val peeked = stack.peek()
        check(peeked == CHECKED_NUM) { "peek() wrong number: expected $CHECKED_NUM, but was ${peeked}" }
        check(stack.size == oldSize) { "peed() did removed item from top, sorry :(" }
    }

    @Test
    fun `pushAll | addAll`() {
        checkPushing { addAll(it) }
        checkPushing { pushAll(*it) }
    }

    private fun checkPushing(addAll: ReorderedStackSet<Int>.(Array<Int>) -> Unit) {
        stack.clear()
        val elements = arrayOf(1, 2, 3, 1)
        val expectedElements = elements.toList().subList(1, elements.size)
        stack.addAll(elements)
        val result = stack.toList()
        check(result == expectedElements) { "expected $expectedElements, but was $result" }
    }

    companion object {
        private const val CHECKED_NUM: Int = 0
    }
}

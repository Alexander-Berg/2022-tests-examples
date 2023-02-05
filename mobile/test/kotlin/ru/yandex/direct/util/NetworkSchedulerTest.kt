// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util

import org.junit.Test
import ru.yandex.direct.web.api5.IDirectApi5
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkSchedulerTest {

    private val testDelayMs = 100L

    private fun delay() = Thread.sleep(testDelayMs)

    @Test
    fun networkScheduler_should_allowUpToFiveConnections() {
        val scheduler = CustomSchedulers.network()

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        // These guys are lucky, because they can make their work simultaneously.
        val luckyWorkers = (1..IDirectApi5.API_REQUEST_LIMIT).map { Worker(lock, condition) }

        // This guy has started too late to finish its work in time.
        val unluckyOne = Worker(lock, condition)

        // Schedule workers to work on a scheduler. Unlucky guy is scheduled last.
        for (worker in luckyWorkers) {
            scheduler.scheduleDirect(worker)
        }
        scheduler.scheduleDirect(unluckyOne)

        delay()

        // At this point all lucky guys should await condition.
        assertTrue(luckyWorkers.all { it.isWorking })
        assertFalse(luckyWorkers.any { it.isDone })

        // Ensure that unlucky guy hasn't started yet.
        assertFalse(unluckyOne.isWorking)
        assertFalse(unluckyOne.isDone)

        // Signal for all lucky guys to finish their work.
        lock.lock()
        condition.signalAll()
        lock.unlock()

        delay()

        // All lucky workers should finish their work after signal.
        assertFalse(luckyWorkers.any { it.isWorking })
        assertTrue(luckyWorkers.all { it.isDone })

        // Pool is empty now, and the last guy is able to begin.
        assertTrue(unluckyOne.isWorking)
        assertFalse(unluckyOne.isDone)

        // Signal the last guy to finish work.
        lock.lock()
        condition.signalAll()
        lock.unlock()

        delay()

        // Check that the last worker has also finished the work.
        assertFalse(unluckyOne.isWorking)
        assertTrue(unluckyOne.isDone)
    }

    private class Worker(val lock: Lock, val condition: Condition) : () -> Unit {

        var isWorking = false

        var isDone = false

        override fun invoke() {
            isWorking = true
            lock.lock()
            try {
                condition.await()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                lock.unlock()
            }
            isWorking = false
            isDone = true
        }

    }

}
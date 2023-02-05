package com.edadeal.android.util

import org.junit.Test
import kotlin.test.assertEquals

class SamplerTest {

    @Test
    fun testSampler() {
        val results = arrayListOf<Float>()
        val samples = floatArrayOf(0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .42f, .1f)
        with(Sampler(
            periodMillis = 1000L,
            intervalMillis = 500L,
            settlingMillis = 3000L,
            onPeriodDone = { results.add(it) }
        )) {
            clock(1)
            for ((i, sample) in samples.withIndex()) {
                val now = i * 500L + 501
                sample(sample)
                clock(now)
            }
            assertEquals(results, arrayListOf(.3f, .5f, .6f))
        }
        results.clear()
        with(Sampler(
            periodMillis = 1000L,
            intervalMillis = 500L,
            settlingMillis = 3000L,
            onPeriodDone = { results.add(it) }
        )) {
            clock(1)
            for (i in 0 until samples.size) {
                val now = i * 500L + 501
                clock(now)
            }
            assertEquals(results, emptyList<Float>())
        }
    }
}

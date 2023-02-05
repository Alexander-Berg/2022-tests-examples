package com.yandex.teamcity.arc.grpc

import com.yandex.teamcity.arc.FakeBridge
import org.apache.commons.lang3.time.StopWatch
import org.junit.Test
import java.util.concurrent.TimeUnit

class SimpleTest {

    @Test
    fun listRefs() {
        val client = ArcGrpcApiCache(FakeBridge()).getArcGrpcApi()
        val allRefsWatch = StopWatch.createStarted()
        val allRefMap = client.listRefs(listOf("trunk", "tags/groups/sdg/pipeline"))
        allRefsWatch.stop()
        println(allRefsWatch.getTime(TimeUnit.MILLISECONDS))
        println(allRefMap.size)
        println(allRefMap.entries.first())
    }
}

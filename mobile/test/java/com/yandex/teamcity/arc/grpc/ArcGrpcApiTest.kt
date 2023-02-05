package com.yandex.teamcity.arc.grpc

import com.yandex.teamcity.arc.FakeBridge
import com.yandex.teamcity.arc.TeamcityPropertiesBridgeImpl
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.teamcity.arc.client.ArcClientImpl
import ru.yandex.teamcity.arc.client.ArcConfig

class ArcGrpcApiTest {
    private val client: ArcGrpcApi = ArcGrpcApiCache(FakeBridge()).getArcGrpcApi()

    @Test
    fun listBranches() {
        val result: Map<String, String> = client.listRefs(listOf("trunk", "tags/groups/sdg/pipeline"))
        Assert.assertTrue(result.size > 2)
        Assert.assertTrue(result.containsKey("arcadia/trunk"))
    }

    @Test
    fun listCommits() {
        val hideRevisionLog = client.getChanges("6f21a9313613d1e1e15c7f87c1eace73182d1b1f", "5b0a00c16d93d052518ae83581ddfda17d76c203", 20)
        assertEquals(4, hideRevisionLog.size.toLong())
        assertEquals("6f21a9313613d1e1e15c7f87c1eace73182d1b1f", hideRevisionLog.first().hash)

        val limitedLog = client.getChanges("6f21a9313613d1e1e15c7f87c1eace73182d1b1f", null, TeamcityPropertiesBridgeImpl.DEFAULT_LOG_LIMIT)
        // max history for new branch equals limit
        assertEquals(TeamcityPropertiesBridgeImpl.DEFAULT_LOG_LIMIT, limitedLog.size)
        assertEquals("6f21a9313613d1e1e15c7f87c1eace73182d1b1f", limitedLog.first().hash)
        assertEquals(hideRevisionLog.first(), limitedLog.first())
    }

    @Test
    fun changeList() {
        val changeList = client.getChangeList("bf5d876bc8529844f404f36ef89d463a2cc469ee")
        assertEquals("yweb/antispam/clean_web/text_cache/lib/text_cache.py", changeList.list.first().path)
    }

    @Test
    fun cachedChangeList() {
        val client = Mockito.spy(ArcClientImpl(ArcConfig(ArcGrpcApiCache(FakeBridge()).findToken())))
        val arcApi = ArcGrpcApi(client)
        val changeList = arcApi.getChangeList("bf5d876bc8529844f404f36ef89d463a2cc469ee")
        assertEquals("yweb/antispam/clean_web/text_cache/lib/text_cache.py", changeList.list.first().path)
        verify(client, times(1)).changeList("bf5d876bc8529844f404f36ef89d463a2cc469ee")
        arcApi.getChangeList("bf5d876bc8529844f404f36ef89d463a2cc469ee")
        verify(client, times(1)).changeList("bf5d876bc8529844f404f36ef89d463a2cc469ee")
    }
}

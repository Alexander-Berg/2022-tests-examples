package com.yandex.teamcity.arc.grpc

import com.yandex.teamcity.arc.FakeBridge
import org.mockito.Mockito
import ru.yandex.teamcity.arc.client.ArcClientImpl
import ru.yandex.teamcity.arc.client.ArcConfig

class ArcMockedGrpcApiCache : ArcGrpcApiCache(FakeBridge()) {
    private val arcGrpcApi: ArcGrpcApi = Mockito.spy(ArcGrpcApi(ArcClientImpl(ArcConfig(findToken()))))

    override fun getArcGrpcApi(): ArcGrpcApi = arcGrpcApi
}

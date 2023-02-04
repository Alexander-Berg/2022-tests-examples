package ru.auto.ara.core.mocks_and_stubbs

import ru.auto.data.repository.INetworkInfoRepository

object TestNetworkInfoRepository : INetworkInfoRepository {

    var isConnectedValue: Boolean = true

    override fun isConnectedToInternet(): Boolean = isConnectedValue

}

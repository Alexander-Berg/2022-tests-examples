package com.yandex.mail.test

import com.yandex.mail.proxy.BlockManager

open class TestBlockManager(val original: BlockManager) : BlockManager by original {

    var testState: BlockManager.State? = null

    override fun getBlockedState(): BlockManager.State {
        return testState ?: original.getBlockedState()
    }
}

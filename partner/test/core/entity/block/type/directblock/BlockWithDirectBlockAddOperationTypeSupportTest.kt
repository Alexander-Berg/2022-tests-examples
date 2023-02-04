package ru.yandex.partner.core.entity.block.type.directblock

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.yandex.partner.core.block.MobileBlockType
import ru.yandex.partner.core.block.direct.DirectBlocks

internal class BlockWithDirectBlockAddOperationTypeSupportTest {

    @Test
    fun resolveDirectBlock() {
        MobileBlockType.values().forEach {
            assertNotNull(
                DirectBlocks.getDefault(it.literal),
                "Should add default direct_block for block_type " + it.literal
            )
        }
    }
}

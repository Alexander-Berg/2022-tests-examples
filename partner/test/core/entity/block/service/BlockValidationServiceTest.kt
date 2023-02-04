package ru.yandex.partner.core.entity.block.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.partner.core.CoreTest
import ru.yandex.partner.core.entity.block.model.RtbBlock
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository

@CoreTest
class BlockValidationServiceTest(
    @Autowired
    val repository: BlockTypedRepository) : BaseValidationTest() {

    @Test
    fun `validate correct block`() {
        val block = repository.getBlockByCompositeId(347649081345L)
        Assertions.assertThat(block).isInstanceOf(RtbBlock::class.java)
        val vr = validate(listOf(block))
        val defectInfos = vr.flattenErrors()
        Assertions.assertThat(defectInfos).isEmpty()
    }

}

package ru.yandex.partner.core.bs

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.partner.core.CoreTest
import ru.yandex.partner.core.entity.block.model.BaseBlock
import ru.yandex.partner.test.utils.TestUtils
import java.nio.file.Files
import java.nio.file.Path

@CoreTest
internal class BkDataFacadeTest(
    @Autowired val bkDataFacade: BkDataFacade<BaseBlock, *, *, *>,
    @Autowired val enrichConfig: BkEnrichConfig
) {

    @Test
    internal fun blockBkDataFillersOrderTest() {
        val referenceDataPath = TestUtils.getAbsolutePath("block-bk-data-fillers-ordered.txt")
        val expected = Files.readAllLines(Path.of(referenceDataPath))
        val actual = bkDataFacade.orderedMessageFillers.stream()
            .map { "${it.javaClass.simpleName}<${it.typeClass.simpleName}>" }.toList()

        if (TestUtils.needSelfUpdate()) {
            Files.write(Path.of(referenceDataPath), actual)
            Assertions.fail("Resource self_updated. Path = $referenceDataPath")
        }
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    internal fun testEnrichSupportedBlockClassesResolvable() {
        assertThat(enrichConfig.supportedBlockTypes).allSatisfy {
            assertThat(it.constructor).isNotNull
            assertThat(it.constructor!!.get()).isNotNull
        }
    }
}

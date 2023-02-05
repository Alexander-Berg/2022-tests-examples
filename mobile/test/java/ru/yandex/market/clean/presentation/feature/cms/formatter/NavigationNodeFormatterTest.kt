package ru.yandex.market.clean.presentation.feature.cms.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.cms.CmsNavigationNode
import ru.yandex.market.clean.domain.model.navigationNodeTestInstance
import ru.yandex.market.clean.presentation.feature.cms.model.CmsNavigationNodeVo
import ru.yandex.market.util.extensions.convertToString

class NavigationNodeFormatterTest {

    @Test
    fun `check correct formatting`() {
        val navigationNode = navigationNodeTestInstance()
        val actualResult = NavigationNodeFormatter()(CmsNavigationNode(navigationNode, false))
        val expectedResult = CmsNavigationNodeVo(navigationNode.name, navigationNode.image, navigationNode, false)

        assertThat(actualResult.convertToString()).isEqualTo(expectedResult.convertToString())
    }
}
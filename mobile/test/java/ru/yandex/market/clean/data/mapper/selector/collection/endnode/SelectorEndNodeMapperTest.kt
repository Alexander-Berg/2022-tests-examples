package ru.yandex.market.clean.data.mapper.selector.collection.endnode

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.model.dto.cms.selector.content.node.selectorEndNodeDtoTestInstance
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.response.imagePostTestInstance

class SelectorEndNodeMapperTest {

    private val imagePostMapperMock = mock<SelectorImagePostMapper>() {
        on { map(any(), any()) } doReturn imagePostTestInstance()
    }
    private val mapper = SelectorEndNodeMapper(imagePostMapperMock)

    @Test
    fun map() {
        val endNode = SelectorNode.EndNode(
            discardButtonText = "denyRetryButtonText",
            finishPost = imagePostTestInstance(),
            retryButtonText = "confirmRetryButtonText"
        )
        val readyNode = SelectorNode.ReadyNode(
            next = endNode,
            successPost = imagePostTestInstance(),
            navigateHid = "hid"
        )
        val pendingNode = SelectorNode.PendingNode(
            next = readyNode,
            pendingPost = imagePostTestInstance()
        )

        val actualResult = mapper.map(selectorEndNodeDtoTestInstance())

        Assertions.assertThat(actualResult).isEqualTo(pendingNode)
    }
}
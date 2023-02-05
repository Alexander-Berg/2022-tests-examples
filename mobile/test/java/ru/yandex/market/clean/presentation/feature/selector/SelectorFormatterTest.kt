package ru.yandex.market.clean.presentation.feature.selector

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.selector.response.ImagePost
import ru.yandex.market.clean.domain.model.selector.collection.SelectorNode
import ru.yandex.market.clean.domain.model.selector.collection.SelectorCollection
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.media.model.EmptyImageReference

class SelectorFormatterTest {

    private val resourceManagerMock = mock<ResourcesManager>() {
        on { getString(any()) } doReturn SOME_RESOURCE_STRING
        on { getFormattedQuantityString(any(), any(), any()) } doReturn SOME_RESOURCE_STRING
    }
    private val formatter = SelectorFormatter(resourceManagerMock)

    private val image = EmptyImageReference()
    private val imagePost = ImagePost(
        image = image,
        label = LABEL
    )
    private val endNode = SelectorNode.EndNode(
        discardButtonText = SOME_STRING_2,
        finishPost = imagePost,
        retryButtonText = SOME_STRING
    )
    private val readyNode = SelectorNode.ReadyNode(
        next = endNode,
        successPost = imagePost,
        navigateHid = SOME_STRING
    )
    private val pendingNode = SelectorNode.PendingNode(
        next = readyNode,
        pendingPost = imagePost
    )

    private val chip = SelectorNode.Chip(
        filters = emptyList(),
        hashCodeId = CHIP_HASH,
        isSelected = false,
        label = LABEL,
        nextNode = null,
        hid = HID,
        nid = NID
    )
    private val flowNode = SelectorNode.FlowNode(
        chips = listOf(chip),
        label = LABEL,
        next = pendingNode
    )

    private val startNode = SelectorNode.StartNode(
        label = LABEL,
        negativeButtonText = SOME_STRING_2,
        next = flowNode,
        picture = image,
        positiveButtonText = SOME_STRING
    )

    @Test
    fun `test format start node linear`() {
        val expectedResult = SelectorStateVo.TwoButtonsWithPicture(
            image = image,
            negativeButtonText = SOME_STRING_2,
            positiveButtonText = SOME_STRING,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.LINEAR, startNode)
        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format start node tree`() {
        val expectedResult = SelectorStateVo.TwoButtonsWithPicture(
            image = image,
            negativeButtonText = SOME_STRING_2,
            positiveButtonText = SOME_STRING,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.TREE, startNode)
        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format flow node linear`() {
        val expectedResult = SelectorStateVo.SelectorChipsVo(
            items = listOf(
                ChipStateVo(
                    filterIds = emptyList(),
                    id = CHIP_HASH,
                    isSelected = false,
                    text = LABEL
                )
            ),
            subtitle = SOME_RESOURCE_STRING,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.LINEAR, flowNode)
        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format flow node tree`() {
        val expectedResult = SelectorStateVo.SelectorChipsVo(
            items = listOf(
                ChipStateVo(
                    filterIds = emptyList(),
                    id = CHIP_HASH,
                    isSelected = false,
                    text = LABEL
                )
            ),
            subtitle = null,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.TREE, flowNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format pending node linear`() {
        val expectedResult = SelectorStateVo.SingleProgressButtonWithPicture(
            buttonText = SOME_RESOURCE_STRING,
            image = image,
            title = LABEL
        )

        //Check linear
        val actualResult = formatter.format(SelectorCollection.Type.LINEAR, pendingNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format pending node tree`() {
        val expectedResult = SelectorStateVo.SingleProgressButtonWithPicture(
            buttonText = SOME_RESOURCE_STRING,
            image = image,
            title = LABEL
        )

        //Check tree
        val actualResult = formatter.format(SelectorCollection.Type.TREE, pendingNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format ready node linear`() {
        val expectedResult = SelectorStateVo.SingleProgressButtonWithPicture(
            buttonText = SOME_RESOURCE_STRING,
            image = image,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.LINEAR, readyNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format ready node tree`() {
        val expectedResult = SelectorStateVo.SingleProgressButtonWithPicture(
            buttonText = SOME_RESOURCE_STRING,
            image = image,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.TREE, readyNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format end node linear`() {
        val expectedResult = SelectorStateVo.TwoButtonsWithPicture(
            image = image,
            negativeButtonText = SOME_STRING_2,
            positiveButtonText = SOME_STRING,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.LINEAR, endNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `test format end node tree`() {
        val expectedResult = SelectorStateVo.TwoButtonsWithPicture(
            image = image,
            negativeButtonText = SOME_STRING_2,
            positiveButtonText = SOME_STRING,
            title = LABEL
        )

        val actualResult = formatter.format(SelectorCollection.Type.TREE, endNode)

        Assertions.assertThat(actualResult).isEqualTo(expectedResult)
    }

    private companion object {
        const val LABEL = "label"
        const val SOME_STRING = "some string"
        const val SOME_STRING_2 = "some string 2"
        const val SOME_RESOURCE_STRING = "some resource"
        const val CHIP_HASH = 0
        const val HID = "123456"
        const val NID = "654321"
    }
}
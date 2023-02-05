package ru.yandex.market.clean.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.ActualizedCartItem
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance

class ActualizedCartItemMapperTest {

    private val mapper = ActualizedCartItemMapper()

    @Test
    fun `check the mapping of a complete match`() {

        val cartItem1 = cartItemTestInstance(matchingKey = "matchingKey1")
        val cartItem2 = cartItemTestInstance(matchingKey = "matchingKey2")
        val cartItem3 = cartItemTestInstance(matchingKey = "matchingKey3")
        val cartItems = listOf(cartItem1, cartItem2, cartItem3)

        val orderItem1 = orderItemTestInstance(matchingKey = "matchingKey1")
        val orderItem2 = orderItemTestInstance(matchingKey = "matchingKey2")
        val orderItem3 = orderItemTestInstance(matchingKey = "matchingKey3")
        val orderItems = listOf(orderItem1, orderItem2, orderItem3)

        val actualResult = mapper.map(cartItems, orderItems)

        val expectedResult = ActualizedCartItemMapper.Result(

            matchedItems = listOf(

                ActualizedCartItem(cartItem1, orderItem1),
                ActualizedCartItem(cartItem2, orderItem2),
                ActualizedCartItem(cartItem3, orderItem3)
            ),
            missingCartItems = emptyList(),
            missingOrderItems = emptyList()
        )

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `check the mapping for a complete mismatch`() {

        val cartItem1 = cartItemTestInstance(matchingKey = "matchingKey1")
        val cartItem2 = cartItemTestInstance(matchingKey = "matchingKey2")
        val cartItem3 = cartItemTestInstance(matchingKey = "matchingKey3")
        val cartItems = listOf(cartItem1, cartItem2, cartItem3)

        val orderItem1 = orderItemTestInstance(matchingKey = "matchingKey34")
        val orderItem2 = orderItemTestInstance(matchingKey = "matchingKey71")
        val orderItem3 = orderItemTestInstance(matchingKey = "matchingKey720")
        val orderItems = listOf(orderItem1, orderItem2, orderItem3)

        val actualResult = mapper.map(cartItems, orderItems)

        val expectedResult = ActualizedCartItemMapper.Result(

            matchedItems = emptyList(),
            missingCartItems = cartItems,
            missingOrderItems = orderItems
        )

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `check the mapping with a partial match`() {

        val cartItem1 = cartItemTestInstance(matchingKey = "matchingKey1")
        val cartItem2 = cartItemTestInstance(matchingKey = "matchingKey4")
        val cartItem3 = cartItemTestInstance(matchingKey = "matchingKey32")
        val cartItem4 = cartItemTestInstance(matchingKey = "matchingKey001")
        val cartItems = listOf(cartItem1, cartItem2, cartItem3, cartItem4)

        val orderItem1 = orderItemTestInstance(matchingKey = "matchingKey3")
        val orderItem2 = orderItemTestInstance(matchingKey = "matchingKey5")
        val orderItem3 = orderItemTestInstance(matchingKey = "matchingKey4")
        val orderItem4 = orderItemTestInstance(matchingKey = "matchingKey1")
        val orderItems = listOf(orderItem1, orderItem2, orderItem3, orderItem4)

        val actualResult = mapper.map(cartItems, orderItems)

        val expectedResult = ActualizedCartItemMapper.Result(

            matchedItems = listOf(

                ActualizedCartItem(cartItem2, orderItem3),
                ActualizedCartItem(cartItem1, orderItem4)
            ),
            missingCartItems = listOf(cartItem3, cartItem4),
            missingOrderItems = listOf(orderItem1, orderItem2)
        )

        assertThat(actualResult).isEqualTo(expectedResult)
    }
}
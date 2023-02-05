package ru.yandex.market.kadavr.state

import com.google.gson.JsonElement
import ru.yandex.market.utils.Json

class CartKadavrState(private val items: List<CartKadavrStateItem>) : KadavrState() {

    override fun getPath(): String {
        return CART_PATH
    }

    override fun getRequestDto(): JsonElement {
        return Json.buildArray(items.map { item ->
            Json.buildObject {
                with(item) {
                    id?.let { "id" to it }
                    price?.let { "price" to it }
                    count?.let { "count" to it }
                }
            }
        })
    }

    data class CartKadavrStateItem(
        val id: String? = null,
        val price: Int? = null,
        val count: Int? = null
    )

    companion object {

        private const val CART_PATH = "Carter.items"
    }
}
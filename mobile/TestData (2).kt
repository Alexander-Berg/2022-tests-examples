package ru.yandex.supercheck.daos

import ru.yandex.supercheck.model.data.persistence.entities.basket.entity.base.BasketInfoEntity
import ru.yandex.supercheck.model.data.persistence.entities.basket.entity.base.CategoryEntity
import ru.yandex.supercheck.model.data.persistence.entities.basket.entity.base.ProductEntity

object TestData {

    const val UNSEEN_PRODUCTS_COUNT = 2
    const val INITIAL_BASKET_INFO_ENTITIES_COUNT = 3

    val products = listOf(
        ProductEntity(
            id = "1",
            modelId = "12",
            name = "LENTA",
            imageUrl = "some/url",
            galleryUrls = "/../../",
            productTextProperties = "qwerty",
            productTableProperties = "qweasd",
            reviewCount = 123,
            markCount = 4,
            averageMark = 4.5f,
            categoryId = "1",
            parentCategoryId = "1",
            hasAlcoOffer = false
        ),

        ProductEntity(
            id = "2",
            modelId = "123",
            name = "LENTA",
            imageUrl = "some/url",
            galleryUrls = "/../../",
            productTextProperties = "qwerty",
            productTableProperties = "qweasd",
            reviewCount = 123,
            markCount = 4,
            averageMark = 4.5f,
            categoryId = "2",
            parentCategoryId = "1",
            hasAlcoOffer = false
        ),

        ProductEntity(
            id = "3",
            modelId = "1234",
            name = "LENTA",
            imageUrl = "some/url",
            galleryUrls = "/../../",
            productTextProperties = "qwerty",
            productTableProperties = "qweasd",
            reviewCount = 123,
            markCount = 4,
            averageMark = 4.5f,
            categoryId = "2",
            parentCategoryId = "1",
            hasAlcoOffer = true
        ),

        ProductEntity(
            id = "4",
            modelId = "12345",
            name = "LENTA",
            imageUrl = "some/url",
            galleryUrls = "/../../",
            productTextProperties = "qwerty",
            productTableProperties = "qweasd",
            reviewCount = 123,
            markCount = 4,
            averageMark = 4.5f,
            categoryId = "2",
            parentCategoryId = "1",
            hasAlcoOffer = true
        )
    )

    val categories = listOf(
        CategoryEntity(
            categoryId = "1",
            categoryName = "First category",
            categoryHid = 1L
        ),

        CategoryEntity(
            categoryId = "2",
            categoryName = "Second category",
            categoryHid = 2L
        )
    )

    val smartListInfos = listOf(
        BasketInfoEntity(
            productId = "1",
            count = 10,
            hasBeenSeen = false,
            addedToShopId = null,
            addedToOutletId = "1"
        ),

        BasketInfoEntity(
            productId = "3",
            count = 3,
            hasBeenSeen = true,
            addedToShopId = null,
            addedToOutletId = "1"
        ),

        BasketInfoEntity(
            productId = "2",
            count = 2,
            hasBeenSeen = false,
            addedToShopId = null,
            addedToOutletId = "1"
        )
    )
}
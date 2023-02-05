package ru.yandex.market.clean.data.mapper

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.data.fapi.dto.FrontApiMergedProductDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiMergedSkuDto
import ru.yandex.market.clean.data.fapi.dto.comparisons.comparisonEntitiesMergedDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.frontApiMergedProductDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.frontApiMergedSkuDtoTestInstance
import ru.yandex.market.clean.data.fapi.mapper.ComparisonProductMapper
import ru.yandex.market.clean.data.fapi.mapper.ComparisonSpecificationGroupMapper
import ru.yandex.market.clean.data.fapi.mapper.ProductsWithComparisonMapper
import ru.yandex.market.clean.data.model.dto.ComparisonProductIdsDto
import ru.yandex.market.clean.data.model.dto.comparisonProductIdsDtoTestInstance
import ru.yandex.market.clean.domain.model.comparisons.ComparedItem
import ru.yandex.market.clean.domain.model.comparisons.ComparisonParam
import ru.yandex.market.clean.domain.model.comparisons.ComparisonSpecificationGroup
import ru.yandex.market.clean.domain.model.comparisons.ComparisonValue
import ru.yandex.market.domain.product.model.ModelId
import ru.yandex.market.domain.product.model.SkuId
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.net.sku.fapi.dto.specs.frontApiFullModelSpecificationsDtoTestInstance
import ru.yandex.market.net.sku.fapi.dto.specs.frontApiModelSpecificationDtoTestInstance
import ru.yandex.market.net.sku.fapi.dto.specs.frontApiShortModelSpecificationsDtoTestInstance

/**
 * При написании тестов следует учитывать логику упорядочивания значений характеристик маппером:
 * сначала идут значения для всех SKU, затем значения для моделей */
class ProductsWithComparisonMapperTest {

    private val resourcesDataStore = mock<ResourcesManager>()
    private val productMapper = mock<ComparisonProductMapper>()
    private val comparisonMapper = ComparisonSpecificationGroupMapper(resourcesDataStore)
    private val productsWithComparisonMapper = ProductsWithComparisonMapper(productMapper, comparisonMapper)

    @Before
    fun mockResourcesDataStore() {
        whenever(resourcesDataStore.getString(R.string.comparison_category_no_data)).thenReturn("нет данных")
    }

    @Test
    fun `Map valid products to comparison`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = listOf("2"), modelIds = listOf("1")),
            models = listOf(getValidModelWithValidSpecs(1)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        val mapped = productsWithComparisonMapper.map(testDto)

        val expectedResult = listOf(
            ComparisonSpecificationGroup(
                title = TEST_GROUP_NAME,
                areValuesEqual = false,
                params = listOf(
                    ComparisonParam(
                        name = GOOD_VALUE_NAME,
                        areValuesEqual = false,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue(SKU_VALUE))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue(MODEL_VALUE))
                            )
                        )
                    ),
                    ComparisonParam(
                        name = BAD_VALUE_NAME,
                        areValuesEqual = true,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue("нет данных"))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue("нет данных"))
                            )
                        )
                    )
                )
            )
        )

        MatcherAssert.assertThat(mapped.second, Matchers.equalTo(expectedResult))
    }


    /**
     * Проверяется только порядок моделей, так как SKU упорядочиваются при извлечении DTO из коллекции */
    @Test
    fun `Map valid products to comparison with wrong model info order`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = listOf("2"), modelIds = listOf("3", "1")),
            models = listOf(getValidModelWithValidSpecs(1), getValidModelWithValidSpecs(3)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        val mapped = productsWithComparisonMapper.map(testDto)

        val expectedResult = listOf(
            ComparisonSpecificationGroup(
                title = TEST_GROUP_NAME,
                areValuesEqual = false,
                params = listOf(
                    ComparisonParam(
                        name = GOOD_VALUE_NAME,
                        areValuesEqual = false,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue(SKU_VALUE))
                            ),
                            ComparedItem(
                                productId = ModelId("3", null),
                                values = listOf(ComparisonValue(MODEL_VALUE))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue(MODEL_VALUE))
                            )
                        )
                    ),
                    ComparisonParam(
                        name = BAD_VALUE_NAME,
                        areValuesEqual = true,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue("нет данных"))
                            ),
                            ComparedItem(
                                productId = ModelId("3", null),
                                values = listOf(ComparisonValue("нет данных"))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue("нет данных"))
                            )
                        )
                    )
                )
            )
        )

        MatcherAssert.assertThat(mapped.second, Matchers.equalTo(expectedResult))
    }

    @Test
    fun `Map products to comparison with extra model`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = listOf("2"), modelIds = listOf("1")),
            models = listOf(getValidModelWithValidSpecs(3), getValidModelWithValidSpecs(1)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        val mapped = productsWithComparisonMapper.map(testDto)

        val expectedResult = listOf(
            ComparisonSpecificationGroup(
                title = TEST_GROUP_NAME,
                areValuesEqual = false,
                params = listOf(
                    ComparisonParam(
                        name = GOOD_VALUE_NAME,
                        areValuesEqual = false,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue(SKU_VALUE))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue(MODEL_VALUE))
                            )
                        )
                    ),
                    ComparisonParam(
                        name = BAD_VALUE_NAME,
                        areValuesEqual = true,
                        comparedItems = listOf(
                            ComparedItem(
                                productId = SkuId("2", null, null),
                                values = listOf(ComparisonValue("нет данных"))
                            ),
                            ComparedItem(
                                productId = ModelId("1", null),
                                values = listOf(ComparisonValue("нет данных"))
                            )
                        )
                    )
                )
            )
        )

        MatcherAssert.assertThat(mapped.second, Matchers.equalTo(expectedResult))
    }


    @Test(expected = IllegalArgumentException::class)
    fun `Map products with insufficiently model`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = listOf("2"), modelIds = listOf("1", "3")),
            models = listOf(getValidModelWithValidSpecs(1)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        productsWithComparisonMapper.map(testDto)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Map products with extra sku`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = emptyList(), modelIds = listOf("1")),
            models = listOf(getValidModelWithValidSpecs(1)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        productsWithComparisonMapper.map(testDto)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Map products with insufficiently sku`() {
        val testDto = comparisonEntitiesMergedDtoTestInstance(
            comparisonProductIdsDto = getProductIds(skuIds = listOf("2", "3"), modelIds = listOf("1")),
            models = listOf(getValidModelWithValidSpecs(1)),
            skus = listOf(getValidSkuWithValidSpecs("2")),
            offers = emptyList()
        )

        productsWithComparisonMapper.map(testDto)
    }


    companion object {

        private const val TEST_GROUP_NAME = "test group"
        private const val GOOD_VALUE_NAME = "good value name"
        private const val BAD_VALUE_NAME = "bad value name"
        private const val MODEL_VALUE = "model value"
        private const val SKU_VALUE = "sku value"


        fun getValidModelWithValidSpecs(id: Long): FrontApiMergedProductDto {
            return frontApiMergedProductDtoTestInstance(
                id = id,
                specs = frontApiShortModelSpecificationsDtoTestInstance(
                    full = listOf(
                        frontApiFullModelSpecificationsDtoTestInstance(
                            name = TEST_GROUP_NAME,
                            specs = listOf(
                                frontApiModelSpecificationDtoTestInstance(
                                    name = GOOD_VALUE_NAME,
                                    value = MODEL_VALUE
                                ),
                                frontApiModelSpecificationDtoTestInstance(name = BAD_VALUE_NAME, value = null)
                            )
                        )
                    )
                )
            )
        }

        fun getValidSkuWithValidSpecs(id: String): FrontApiMergedSkuDto {
            return frontApiMergedSkuDtoTestInstance(
                id = id,
                specs = frontApiShortModelSpecificationsDtoTestInstance(
                    full = listOf(
                        frontApiFullModelSpecificationsDtoTestInstance(
                            name = TEST_GROUP_NAME,
                            specs = listOf(
                                frontApiModelSpecificationDtoTestInstance(
                                    name = GOOD_VALUE_NAME,
                                    value = SKU_VALUE
                                ),
                                frontApiModelSpecificationDtoTestInstance(name = BAD_VALUE_NAME, value = null)
                            )
                        )
                    )
                ),
                restrictedAge18 = false,
                productIdDto = null,
                isAdultDto = null,
                offersCountDto = null,
                preciseRating = null,
                reviewsCountDto = null,
                ratingCountDto = null,
                reasonsToBuyDto = null,
                skuOffersCount = null,
                skuPrices = null,
                skuStats = null,
                shop = null,
            )
        }

        fun getProductIds(skuIds: List<String>, modelIds: List<String>): ComparisonProductIdsDto {
            return comparisonProductIdsDtoTestInstance(
                sku = skuIds,
                models = modelIds
            )
        }
    }

}
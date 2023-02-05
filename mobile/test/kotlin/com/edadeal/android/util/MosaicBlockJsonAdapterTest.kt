package com.edadeal.android.util

import com.edadeal.android.model.mosaic.MosaicBlock
import com.edadeal.android.model.mosaic.MosaicBlockParams
import com.edadeal.android.model.mosaic.MosaicElement
import com.edadeal.android.model.mosaic.MosaicNativeBody
import com.edadeal.android.model.mosaic.MosaicPage
import com.edadeal.android.model.mosaic.MosaicRetailerTypeShopsParams
import com.edadeal.android.model.mosaic.MosaicSkeleton
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class MosaicBlockJsonAdapterTest {
    private val moshi by lazy { Moshi.Builder().setupMoshi().build() }
    private val adapter by lazy { moshi.adapter<MosaicPage>() }

    @Test
    fun `should return mosaic page with expected blocks`() {
        val expected = listOf(
            nativeBlock(id = MosaicElement.allShopsId, priority = 1),
            nativeBlock(id = MosaicElement.advertId),
            nativeBlock(id = MosaicElement.advertId),
            mosaicDivBlock(
                id = "123",
                priority = 3,
                skeleton = nativeSkeleton(height = 128)
            ),
            nativeBlock(id = MosaicElement.advertId),
            nativeBlock(
                id = MosaicElement.retailerTypeShopsId,
                priority = 4,
                params = MosaicRetailerTypeShopsParams(uuid = "1")
            ),
            nativeBlock(id = MosaicElement.advertId)
        )
        val json = """{
  "id": 1,
  "versionId": 393,
  "blocks": [
    {
      "id": "allShops",
      "slug": "slug_allShops",
      "type": "native",
      "priority": 1
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "123",
      "slug": "slug_123",
      "type": "div",
      "skeleton": {
        "type": "native",
        "body": {
          "height": 128
        }
      },
      "priority": 3
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "retailerTypeShops",
      "slug": "slug_retailerTypeShops",
      "type": "native",
      "params": {
        "uuid": "1"
      },
      "priority": 4
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    }
  ]
}""".trimIndent()

        assertEquals(expected, adapter.fromJson(json)?.blocks)
    }

    @Test
    fun `should return mosaic page without block params on parse error`() {
        val expected = listOf(
            nativeBlock(id = MosaicElement.advertId),
            nativeBlock(id = MosaicElement.retailerTypeShopsId, priority = 8),
            nativeBlock(id = MosaicElement.advertId),
            nativeBlock(id = MosaicElement.retailerTypeShopsId, priority = 8),
            nativeBlock(id = MosaicElement.advertId),
            mosaicDivBlock(
                id = "321",
                priority = 1,
                skeleton = nativeSkeleton(height = 64)
            )
        )
        val json = """{
  "id": 1,
  "versionId": 394,
  "blocks": [
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "retailerTypeShops",
      "slug": "slug_retailerTypeShops",
      "type": "native",
      "params": {
        "value": 0
      },
      "priority": 8
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "retailerTypeShops",
      "slug": "slug_retailerTypeShops",
      "type": "native",
      "priority": 8
    },
    {
      "id": "advert",
      "slug": "slug_advert",
      "type": "native",
      "priority": 50
    },
    {
      "id": "321",
      "slug": "slug_321",
      "type": "div",
      "skeleton": {
        "type": "native",
        "body": {
          "height": 64
        }
      },
      "priority": 1
    }
  ]
}""".trimIndent()

        assertEquals(expected, adapter.fromJson(json)?.blocks)
    }

    private fun nativeBlock(
        id: String,
        priority: Int = 50,
        params: MosaicBlockParams? = null
    ) = MosaicBlock(
        id = id,
        type = MosaicElement.nativeType,
        slug = "slug_$id",
        skeleton = null,
        params = params,
        priority = priority
    )

    private fun nativeSkeleton(height: Int) = MosaicSkeleton(type = "native", body = MosaicNativeBody(height))

    private fun mosaicDivBlock(
        id: String,
        priority: Int = 50,
        skeleton: MosaicSkeleton? = null
    ) = MosaicBlock(
        id = id,
        type = MosaicElement.divType,
        slug = "slug_$id",
        skeleton = skeleton,
        params = null,
        priority = priority
    )
}

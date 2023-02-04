package com.yandex.maps.testapp.directions_navigation

import com.yandex.runtime.image.ImageProvider

interface ImageProviderFactory {
    fun fromResource(resourceId: Int): ImageProvider
}

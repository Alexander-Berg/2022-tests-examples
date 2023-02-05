package ru.yandex.yandexmaps.launch

import ru.yandex.maps.BaseTest
import ru.yandex.yandexmaps.multiplatform.core.uri.Uri

abstract class UriParserBaseTest : BaseTest() {

    protected fun parseUri(uriString: String): Uri {
        return Uri.parse(uriString) ?: throw IllegalArgumentException("Invalid URI: $uriString")
    }
}

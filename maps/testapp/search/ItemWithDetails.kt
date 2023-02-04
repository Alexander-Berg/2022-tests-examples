package com.yandex.maps.testapp.search

data class ItemWithDetails(val text: String?, val details: String? = null, val onClick: (() -> Unit)? = null)

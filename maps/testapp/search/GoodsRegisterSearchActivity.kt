package com.yandex.maps.testapp.search

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.GoodsRegisterSession.GoodsRegisterListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error

class GoodsRegisterSearchActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val searchBox by lazy { find<SearchBox>(R.id.card_search_view) }
    private val searchResults by lazy { find<SectionedListView>(R.id.card_search_results) }
    private var session: GoodsRegisterSession? = null

    private fun defaultSearchBoxText() = "86844720190"

    private fun createSession(uri: String): GoodsRegisterSession {
        return searchManager.requestGoodsRegister(
            "ymapsbm1://org?oid=$uri",
            searchListener)
    }

    private fun addGoodsCategories(categories: List<GoodsCategory>) {
        categories.forEach { category ->
            var categoryName = "category"
            category.name?.let { categoryName += " | $it" }
            searchResults.addSection(categoryName, category.goods.map { goodsItemWithDetails(it) })
        }
    }

    private fun addGoodsTags(tags: List<String>) {
        if (tags.isNotEmpty()) {
            searchResults.addSection("Tags", tags.joinToString(","))
        }
    }

    private fun fillInfo(goodsRegister: GoodsRegister) {
        addGoodsCategories(goodsRegister.categories)
        addGoodsTags(goodsRegister.tags)
    }

    private val searchListener: GoodsRegisterListener = object : GoodsRegisterListener {
        override fun onGoodsRegisterError(error: Error) {
            searchBox.setProgress(false)

            showErrorMessage(this@GoodsRegisterSearchActivity, error)
        }

        override fun onGoodsRegisterResponse(goodsRegister: GoodsRegister) {
            searchBox.setProgress(false)
            informOnEmptyResults(this@GoodsRegisterSearchActivity, goodsRegister.categories)
            fillInfo(goodsRegister)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_base_card_search)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        searchBox.text = defaultSearchBoxText()
        findViewById<View>(R.id.search_options).hide()
        searchBox.setListener(searchBoxListener)
        searchBoxListener.onSubmit(searchBox.text)
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) {
            searchBox.setProgress(true)
            searchResults.clearItems()

            session?.cancel()
            session = createSession(text)
        }
    }
}

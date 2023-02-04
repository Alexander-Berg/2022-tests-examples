package ru.auto.data.model.network.scala.catalog.converter

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.catalog.Suggest
import ru.auto.data.model.network.scala.catalog.NWCarSuggest
import ru.auto.data.model.network.scala.catalog.NWCatalog
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 24.07.17.
 */
@RunWith(AllureRunner::class) class SuggestConverterTest {

    @Test
    fun `try to convert json`() {
        val nwCatalog: NWCatalog = FileTestUtils.readJsonAsset("/assets/catalog.json", NWCatalog::class.java)
        val nwSuggest: NWCarSuggest? = nwCatalog.car_suggest
        val suggest: Suggest = SuggestConverter().fromNetwork(nwSuggest!!)
        println(suggest) // no crushes is ok already :)
    }
}

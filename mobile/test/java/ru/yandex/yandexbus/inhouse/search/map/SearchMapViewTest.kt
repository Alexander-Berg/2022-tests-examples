package ru.yandex.yandexbus.inhouse.search.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.common.layer.PlacemarkLayerObjectMetadata
import ru.yandex.yandexbus.inhouse.common.layer.TestMapObjectCollection
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.map.MapObjectLayer
import ru.yandex.yandexbus.inhouse.map.PlacemarkExtras
import ru.yandex.yandexbus.inhouse.model.GeoModel
import ru.yandex.yandexbus.inhouse.search.layer.SearchLayer
import ru.yandex.yandexbus.inhouse.view.glide.GlideIconManager
import ru.yandex.yandexbus.inhouse.whenever

class SearchMapViewTest : BaseTest() {

    private lateinit var searchLayer: SearchLayer

    @Mock
    private lateinit var glideIconManager: GlideIconManager

    @Mock
    private lateinit var selectedGeoModel: GeoModel

    @Mock
    private lateinit var unselectedGeoModel: GeoModel

    private lateinit var selectedItemMetadata: PlacemarkLayerObjectMetadata<GeoModel>

    private lateinit var unselectedItemMetadata: PlacemarkLayerObjectMetadata<GeoModel>

    private lateinit var searchMapView: SearchMapView

    @Before
    override fun setUp() {
        super.setUp()

        val parentCollection = TestMapObjectCollection()
        val mapObjectLayer = MapObjectLayer<PlacemarkExtras>(id = "", parent = parentCollection)
        val selectedMapObjectLayer = MapObjectLayer<PlacemarkExtras>(id = "", parent = parentCollection)

        searchLayer = SearchLayer(context, glideIconManager, mapObjectLayer, selectedMapObjectLayer)

        selectedItemMetadata = PlacemarkLayerObjectMetadata(
            selectedGeoModel,
            GeoPlaces.Minsk.CENTER,
            isSelected = true
        )

        unselectedItemMetadata = PlacemarkLayerObjectMetadata(
            unselectedGeoModel,
            GeoPlaces.Minsk.YANDEX,
            isSelected = false
        )

        whenever(unselectedGeoModel.requirePosition()).thenReturn(unselectedItemMetadata.location)

        searchMapView = SearchMapView(searchLayer)
    }

    @Test
    fun `clear removes only unselected items from layer`() {
        searchLayer.setItems(listOf(selectedItemMetadata, unselectedItemMetadata), zoomRange = null)
        searchMapView.clearGeoModels()
        assertEquals(listOf(selectedItemMetadata), searchLayer.getItems())
    }

    @Test
    fun `item update does not remove selected items from layer`() {
        searchLayer.setItems(listOf(selectedItemMetadata))
        searchMapView.updateGeoModels(listOf(unselectedGeoModel))
        assertEquals(2, searchLayer.getItems().size)
        assertTrue(selectedGeoModel in searchLayer)
        assertTrue(unselectedGeoModel in searchLayer)
    }
}

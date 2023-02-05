package ru.yandex.yandexbus.inhouse.velobike.map

import org.junit.Assert
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
import ru.yandex.yandexbus.inhouse.velobike.layer.VelobikeLayer
import ru.yandex.yandexbus.inhouse.view.glide.GlideIconManager
import ru.yandex.yandexbus.inhouse.whenever

class VelobikeMapViewTest : BaseTest() {

    private lateinit var velobikeLayer: VelobikeLayer

    @Mock
    private lateinit var glideIconManager: GlideIconManager

    @Mock
    private lateinit var selectedGeoModel: GeoModel

    @Mock
    private lateinit var unselectedGeoModel: GeoModel

    private lateinit var selectedItemMetadata: PlacemarkLayerObjectMetadata<GeoModel>

    private lateinit var unselectedItemMetadata: PlacemarkLayerObjectMetadata<GeoModel>

    private lateinit var velobikeMapView: VelobikeMapView

    @Before
    override fun setUp() {
        super.setUp()

        val parentCollection = TestMapObjectCollection()
        val mapObjectLayer = MapObjectLayer<PlacemarkExtras>(id = "", parent = parentCollection)
        val selectedMapObjectLayer = MapObjectLayer<PlacemarkExtras>(id = "", parent = parentCollection)

        velobikeLayer = VelobikeLayer(context, glideIconManager, mapObjectLayer, selectedMapObjectLayer)

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

        velobikeMapView = VelobikeMapView(velobikeLayer)
    }

    @Test
    fun `clear removes only unselected items from layer`() {
        velobikeLayer.setItems(listOf(selectedItemMetadata, unselectedItemMetadata), zoomRange = null)
        velobikeMapView.clearGeoModels()
        Assert.assertEquals(listOf(selectedItemMetadata), velobikeLayer.getItems())
    }

    @Test
    fun `item update does not remove selected items from layer`() {
        velobikeLayer.setItems(listOf(selectedItemMetadata))
        velobikeMapView.updateGeoModels(zoom = 0f, geoModels = listOf(unselectedGeoModel))
        Assert.assertEquals(2, velobikeLayer.getItems().size)
        Assert.assertTrue(selectedGeoModel in velobikeLayer)
        Assert.assertTrue(unselectedGeoModel in velobikeLayer)
    }
}
package ru.yandex.yandexbus.inhouse.common.layer

import com.yandex.mapkit.geometry.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.map.MapObjectLayer
import ru.yandex.yandexbus.inhouse.map.Placemark
import ru.yandex.yandexbus.inhouse.map.PlacemarkExtras
import ru.yandex.yandexbus.inhouse.map.Range

class PlacemarkLayerTest : BaseTest() {

    private lateinit var itemsLayer: MapObjectLayer<PlacemarkExtras>
    private lateinit var selectedItemsLayer: MapObjectLayer<PlacemarkExtras>
    private lateinit var placemarkLayer: PlacemarkLayer<String, TestZoomRange>

    private lateinit var metadata: PlacemarkLayerObjectMetadata<String>
    private lateinit var selectedMetadata: PlacemarkLayerObjectMetadata<String>

    @Before
    override fun setUp() {
        super.setUp()

        itemsLayer = MapObjectLayer("general", TestMapObjectCollection())
        selectedItemsLayer = MapObjectLayer("selected", TestMapObjectCollection())

        placemarkLayer = PlacemarkLayer(context, itemsLayer, selectedItemsLayer) { placemark, metadata, range ->
            TestLayerObject(placemark, metadata, range)
        }

        metadata = PlacemarkLayerObjectMetadata("test1", Point(0.0, 0.0), isSelected = false)
        selectedMetadata = PlacemarkLayerObjectMetadata("test2", Point(1.0, 0.0), isSelected = true)
    }

    @Test
    fun `clear empties`() {
        placemarkLayer.setItems(listOf(metadata))
        placemarkLayer.clear()

        assertTrue(placemarkLayer.getItems().isEmpty())
    }

    @Test
    fun `contains added only`() {
        val data = metadata.data

        placemarkLayer.setItems(listOf(metadata))

        assertTrue(placemarkLayer.contains(data))
        assertFalse(placemarkLayer.contains(selectedMetadata.data))
    }

    @Test
    fun `added not changed`() {
        val items = listOf(metadata)
        placemarkLayer.setItems(items)
        assertEquals(items, placemarkLayer.getItems())
    }

    @Test
    fun `adds new only`() {
        placemarkLayer.addItem(metadata)
        placemarkLayer.addItem(metadata)
        assertEquals(1, (itemsLayer.mapObjectCollection as TestMapObjectCollection).placemarks.size)
    }

    @Test
    fun `duplicate items are ignored`() {
        placemarkLayer.setItems(listOf(metadata, metadata))
        assertEquals(1, (itemsLayer.mapObjectCollection as TestMapObjectCollection).placemarks.size)
    }

    @Test
    fun `notifies of clicked metadata only`() {
        placemarkLayer.setItems(listOf(metadata, selectedMetadata))

        val subscriber = placemarkLayer.clicks.test()

        (itemsLayer.mapObjectCollection as TestMapObjectCollection).placemarks[0].performTap()

        subscriber.assertValues(metadata).assertNotCompleted()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `added items can be selected only`() {
        placemarkLayer.setSelection(metadata.data)
    }

    @Test
    fun `selection applies from metadata`() {
        placemarkLayer.setItems(listOf(metadata, selectedMetadata))

        assertFalse(placemarkLayer.getItem(metadata.data)!!.isSelected)
        assertTrue(placemarkLayer.getItem(selectedMetadata.data)!!.isSelected)
    }

    @Test
    fun `selection is not accumulated`() {
        placemarkLayer.setItems(listOf(metadata, selectedMetadata))

        placemarkLayer.setSelection(metadata.data)

        assertTrue(placemarkLayer.isSelected(metadata.data))
        assertFalse(placemarkLayer.isSelected(selectedMetadata.data))
    }

    @Test
    fun `selection is not saved after items change`() {
        placemarkLayer.setItems(listOf(metadata))
        placemarkLayer.setSelection(metadata.data)
        placemarkLayer.setItems(listOf(metadata))

        assertFalse(placemarkLayer.isSelected(metadata.data))
    }

    @Test
    fun `selection changes 'selected' info only`() {
        placemarkLayer.setItems(listOf(metadata))

        placemarkLayer.setSelection(metadata.data)

        val placemarkLayerMetadata = placemarkLayer.getItem(metadata.data)!!
        assertEquals(metadata.data, placemarkLayerMetadata.data)
        assertEquals(metadata.location, placemarkLayerMetadata.location)
    }

    @Test
    fun `select multiple`() {
        placemarkLayer.setItems(listOf(metadata, selectedMetadata))

        placemarkLayer.setSelection(listOf(metadata.data, selectedMetadata.data))

        assertTrue(placemarkLayer.isSelected(metadata.data))
        assertTrue(placemarkLayer.isSelected(selectedMetadata.data))
    }

    @Test
    fun `remove call removes existing item`() {
        placemarkLayer.setItems(listOf(metadata, selectedMetadata))

        placemarkLayer.removeItem(selectedMetadata)

        assertEquals(listOf(metadata), placemarkLayer.getItems().toList())
    }

    @Test
    fun `remove does nothing if item is not in layer`() {
        placemarkLayer.setItems(listOf(metadata))

        placemarkLayer.removeItem(selectedMetadata)

        assertEquals(listOf(metadata), placemarkLayer.getItems().toList())
    }

    @Test
    fun `remove ignores selection`() {
        placemarkLayer.setItems(listOf(metadata))

        placemarkLayer.removeItem(metadata.copy(isSelected = true))

        assertTrue(placemarkLayer.getItems().isEmpty())
    }
}

private class TestLayerObject(
    override val placemark: Placemark<PlacemarkExtras>,
    override var metadata: PlacemarkLayerObjectMetadata<String>,
    override var zoom: TestZoomRange?
) : PlacemarkLayerObject<String, TestZoomRange>

private enum class TestZoomRange(override val from: Float, override val to: Float) : Range {
    BIG(10f, Float.POSITIVE_INFINITY),
    SMALL(1f, 10f)
}

package ru.yandex.maps.uikit.atomicviews.snippet.gridgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

private const val MAX_ITEMS_COUNT = 9
private const val LOGO_MOCK = ""
private const val PHOTO_MOCK = ""

class GridGalleryItemViewStateMappingTest {

    private val logoUri = Pair(null, LOGO_MOCK)
    private val photoUris = mutableListOf<Pair<Int?, String>>().apply { repeat(MAX_ITEMS_COUNT) { index -> add(Pair(index, PHOTO_MOCK)) } }

    private fun galleryItem(count: Int, total: Int, withLogo: Boolean): GridGalleryViewModel {
        val list = mutableListOf<Pair<Int?, String>>().apply { 0.until(min(count, MAX_ITEMS_COUNT)).forEach { add(photoUris[it]) } }
        return GridGalleryViewModel(list.map { it.second }, total, logoUri.takeIf { withLogo }?.second)
    }

    @Test
    fun noPhotos_mappedCorrect() {
        val galleryItem = galleryItem(count = 0, total = 0, withLogo = false)
        val mappedViewState = galleryItem.items

        assertTrue(mappedViewState.isEmpty())
    }

    @Test
    fun onePhotoWithoutLogo_mappedCorrect() {
        val galleryItem = galleryItem(count = 1, total = 1, withLogo = false)
        val elements = galleryItem.items

        assertEquals(1, elements.size)
        assertEquals(SinglePhotoElement(photoUris[0]), elements.first())
    }

    @Test
    fun twoPhotosWithoutLogo_mappedCorrect() {
        val galleryItem = galleryItem(count = 2, total = 2, withLogo = false)
        val elements = galleryItem.items

        assertEquals(2, elements.size)
        assertEquals(HalfScreenPhotoElement(photoUris[0]), elements[0])
        assertEquals(HalfScreenPhotoElement(photoUris[1]), elements[1])
    }

    @Test
    fun threePhotosWithLogo_mapped_mappedCorrect() {
        val galleryItem = galleryItem(count = 2, total = 2, withLogo = true)
        val elements = galleryItem.items

        assertEquals(3, elements.size)
        assertEquals(SmallSquarePhotoElement(logoUri, isLogo = true), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[0]), elements[1])
        assertEquals(StretchedPhotoElement(photoUris[1]), elements[2])
    }

    @Test
    fun threePhotosWithoutLogo_mapped_mappedCorrect() {
        val galleryItem = galleryItem(count = 3, total = 3, withLogo = false)
        val elements = galleryItem.items

        assertEquals(3, elements.size)
        assertEquals(StretchedPhotoElement(photoUris[0]), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[1]), elements[1])
        assertEquals(SmallSquarePhotoElement(photoUris[2]), elements[2])
    }

    @Test
    fun tooManyPhotosWithoutLogo_mappedCorrect() {
        val total = 60
        val galleryItem = galleryItem(count = 30, total = total, withLogo = false)
        val elements = galleryItem.items

        assertEquals(MAX_ITEMS_COUNT, elements.size)
        assertEquals(RectPhotoElement(photoUris[0]), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[1]), elements[1])
        assertEquals(SmallSquarePhotoElement(photoUris[2]), elements[2])
        assertEquals(RectPhotoElement(photoUris[3]), elements[3])
        assertEquals(RectPhotoElement(photoUris[4]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[5]), elements[5])
        assertEquals(SmallSquarePhotoElement(photoUris[6]), elements[6])
        assertEquals(RectPhotoElement(photoUris[7]), elements[7])
        assertEquals(MorePhotosElement(photoUris[8], total - MAX_ITEMS_COUNT + 1), elements[8])
    }

    @Test
    fun tooManyPhotosWithLogo_mappedCorrect() {
        val total = 60
        val galleryItem = galleryItem(count = 30, total = total, withLogo = true)
        val elements = galleryItem.items

        assertEquals(MAX_ITEMS_COUNT, elements.size)
        assertEquals(SmallSquarePhotoElement(logoUri, isLogo = true), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[0]), elements[1])
        assertEquals(RectPhotoElement(photoUris[1]), elements[2])
        assertEquals(RectPhotoElement(photoUris[2]), elements[3])
        assertEquals(SmallSquarePhotoElement(photoUris[3]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[4]), elements[5])
        assertEquals(RectPhotoElement(photoUris[5]), elements[6])
        assertEquals(RectPhotoElement(photoUris[6]), elements[7])
        assertEquals(MorePhotosElement(photoUris[7], total - MAX_ITEMS_COUNT + 2), elements[8])
    }

    @Test
    fun sixPhotosWithoutLogo_mappedCorrect() {
        val total = 6
        val galleryItem = galleryItem(count = total, total = total, withLogo = false)
        val elements = galleryItem.items

        assertEquals(total, elements.size)
        assertEquals(RectPhotoElement(photoUris[0]), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[1]), elements[1])
        assertEquals(SmallSquarePhotoElement(photoUris[2]), elements[2])
        assertEquals(RectPhotoElement(photoUris[3]), elements[3])
        assertEquals(RectPhotoElement(photoUris[4]), elements[4])
        assertEquals(RectPhotoElement(photoUris[5]), elements[5])
    }

    @Test
    fun fivePhotosWithLogo_mappedCorrect() {
        val total = 4
        val galleryItem = galleryItem(count = total, total = total, withLogo = true)
        val elements = galleryItem.items

        assertEquals(total + 1, elements.size)
        assertEquals(SmallSquarePhotoElement(logoUri, isLogo = true), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[0]), elements[1])
        assertEquals(RectPhotoElement(photoUris[1]), elements[2])
        assertEquals(RectPhotoElement(photoUris[2]), elements[3])
        assertEquals(RectPhotoElement(photoUris[3]), elements[4])
    }

    @Test
    fun sixPhotosWithLogo_mappedCorrect() {
        val total = 5
        val galleryItem = galleryItem(count = total, total = total, withLogo = true)
        val elements = galleryItem.items

        assertEquals(total + 1, elements.size)
        assertEquals(SmallSquarePhotoElement(logoUri, isLogo = true), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[0]), elements[1])
        assertEquals(RectPhotoElement(photoUris[1]), elements[2])
        assertEquals(RectPhotoElement(photoUris[2]), elements[3])
        assertEquals(SmallSquarePhotoElement(photoUris[3]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[4]), elements[5])
    }

    @Test
    fun sevenPhotosWithoutLogo_mappedCorrect() {
        val total = 7
        val galleryItem = galleryItem(count = total, total = total, withLogo = false)
        val elements = galleryItem.items

        assertEquals(total, elements.size)
        assertEquals(RectPhotoElement(photoUris[0]), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[1]), elements[1])
        assertEquals(SmallSquarePhotoElement(photoUris[2]), elements[2])
        assertEquals(RectPhotoElement(photoUris[3]), elements[3])
        assertEquals(RectPhotoElement(photoUris[4]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[5]), elements[5])
        assertEquals(SmallSquarePhotoElement(photoUris[6]), elements[6])
    }

    @Test
    fun eightPhotosWithLogo_mappedCorrect() {
        val total = 7
        val galleryItem = galleryItem(count = total, total = total, withLogo = true)
        val elements = galleryItem.items

        assertEquals(MAX_ITEMS_COUNT - 1, elements.size)
        assertEquals(SmallSquarePhotoElement(logoUri, isLogo = true), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[0]), elements[1])
        assertEquals(RectPhotoElement(photoUris[1]), elements[2])
        assertEquals(RectPhotoElement(photoUris[2]), elements[3])
        assertEquals(SmallSquarePhotoElement(photoUris[3]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[4]), elements[5])
        assertEquals(RectPhotoElement(photoUris[5]), elements[6])
        assertEquals(RectPhotoElement(photoUris[6]), elements[7])
    }

    @Test
    fun eightPhotosWithoutLogo_mappedCorrect() {
        val total = 8
        val galleryItem = galleryItem(count = 8, total = total, withLogo = false)
        val elements = galleryItem.items

        assertEquals(MAX_ITEMS_COUNT - 1, elements.size)
        assertEquals(RectPhotoElement(photoUris[0]), elements[0])
        assertEquals(SmallSquarePhotoElement(photoUris[1]), elements[1])
        assertEquals(SmallSquarePhotoElement(photoUris[2]), elements[2])
        assertEquals(RectPhotoElement(photoUris[3]), elements[3])
        assertEquals(RectPhotoElement(photoUris[4]), elements[4])
        assertEquals(SmallSquarePhotoElement(photoUris[5]), elements[5])
        assertEquals(SmallSquarePhotoElement(photoUris[6]), elements[6])
        assertEquals(RectPhotoElement(photoUris[7]), elements[7])
    }
}

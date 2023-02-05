package ru.yandex.market.activity.searchresult.sponsored.richphoto

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.common.experiments.experiment.sponsored.SponsoredTagNameExperiment
import ru.yandex.market.domain.media.model.ImageReference
import ru.yandex.market.feature.manager.SponsoredTagNameFeatureManager
import ru.yandex.market.common.android.ResourcesManager

class SponsoredRichPhotoPresenterTest {

    private val sponsoredRichPhotoTitleFormatter = mock<SponsoredRichPhotoTitleFormatter> {
        on { formatTitle(any()) } doReturn "TEST VENDOR"
    }

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(any()) } doReturn "Перейти к товару"
        on { getQuantityString(R.plurals.more_photos_text, 1) } doReturn "Еще 1\nфотография"
        on { getQuantityString(R.plurals.more_photos_text, 2) } doReturn "Еще 2\nфотографии"
        on { getQuantityString(R.plurals.more_photos_text, 5) } doReturn "Еще 5\nфотографий"
    }

    private val sponsoredTagNameFeatureManager = mock<SponsoredTagNameFeatureManager> {
        on { getSponsoredNaming() } doReturn SponsoredTagNameExperiment.SponsoredNaming.SPONSORED
    }

    private val emptyImage = ImageReference.empty()

    private val offerZero = mock<ProductOffer> {
        on { images } doReturn listOf()
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerOne = mock<ProductOffer> {
        on { images } doReturn listOf(emptyImage)
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerTwo = mock<ProductOffer> {
        on { images } doReturn listOf(emptyImage, emptyImage)
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerFour = mock<ProductOffer> {
        on { images } doReturn listOf(emptyImage, emptyImage, emptyImage, emptyImage)
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerFive = mock<ProductOffer> {
        on { images } doReturn listOf(emptyImage, emptyImage, emptyImage, emptyImage, emptyImage)
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerSix = mock<ProductOffer> {
        on { images } doReturn listOf(emptyImage, emptyImage, emptyImage, emptyImage, emptyImage, emptyImage)
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }

    private val offerNine = mock<ProductOffer> {
        on { images } doReturn listOf(
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage,
            emptyImage
        )
        on { model } doReturn mock()
        on { model?.vendor } doReturn mock()
        on { model?.vendor?.name } doReturn "TEST VENDOR"
    }


    @Test
    fun `show zero photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerZero,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).hideView()
    }

    @Test
    fun `show one photo test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerOne,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).hideView()
    }

    @Test
    fun `show two photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerTwo,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).showView()
        verify(sponsoredRichPhotoView).setTitle("TEST VENDOR")
        verify(sponsoredRichPhotoView).showPhotos(listOf(emptyImage), "Перейти к товару")
    }

    @Test
    fun `show four photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerFour,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).showView()
        verify(sponsoredRichPhotoView).setTitle(any())
        verify(sponsoredRichPhotoView, times(1)).showPhotos(
            listOf(emptyImage, emptyImage, emptyImage),
            "Перейти к товару"
        )
    }

    @Test
    fun `show five photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerFive,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).showView()
        verify(sponsoredRichPhotoView).setTitle(any())
        verify(sponsoredRichPhotoView).showPhotos(listOf(emptyImage, emptyImage, emptyImage), "Еще 1\nфотография")
    }

    @Test
    fun `show six photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerSix,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).showView()
        verify(sponsoredRichPhotoView).setTitle(any())
        verify(sponsoredRichPhotoView).showPhotos(listOf(emptyImage, emptyImage, emptyImage), "Еще 2\nфотографии")
    }

    @Test
    fun `show nine photos test`() {
        val presenter = SponsoredRichPhotoPresenter(
            mock(),
            sponsoredRichPhotoTitleFormatter,
            offerNine,
            mock(),
            resourcesDataStore,
            mock(),
            mock(),
            mock(),
            sponsoredTagNameFeatureManager,
        )
        val sponsoredRichPhotoView = mock<SponsoredRichPhotoView>()

        presenter.attachView(sponsoredRichPhotoView)
        verify(sponsoredRichPhotoView).showView()
        verify(sponsoredRichPhotoView).setTitle(any())
        verify(sponsoredRichPhotoView).showPhotos(listOf(emptyImage, emptyImage, emptyImage), "Еще 5\nфотографий")
    }

}
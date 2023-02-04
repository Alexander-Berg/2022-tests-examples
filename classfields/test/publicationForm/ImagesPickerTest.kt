package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.allure.step
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.registerImageCaptureIntentAndMockImage
import com.yandex.mobile.realty.core.robot.*
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author matek3022 on 2020-06-24.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ImagesPickerTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule,
        GrantPermissionRule.grant(Manifest.permission.CAMERA)
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun checkEmptyImages() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            waitUntil { isExpandedToolbarShown() }
            collapseAppBar()
            waitUntil { isCollapsedToolbarShown() }
            isMaxPhotosDescriptionShown()
            isGuideMessageShown()
            isLargeAddImageButtonShown()
            isSmallAddImageButtonHidden()
            isDoneButtonShown()
        }
    }

    @Test
    fun successUploadSingleImageAndDelete() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri) }
            isLargeAddImageButtonHidden()
            isSmallAddImageButtonShown()

            tapOn(lookup.matchesImageDeleteButton(1))
            isImageItemHidden(1)
            isSmallAddImageButtonHidden()
            isLargeAddImageButtonShown()
        }
    }

    @Test
    fun errorUploadSingleImageAndDelete() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isErrorImageItemShown(1) }
            isLargeAddImageButtonHidden()
            isSmallAddImageButtonShown()

            tapOn(lookup.matchesImageDeleteButton(1))
            isImageItemHidden(1)
            isSmallAddImageButtonHidden()
            isLargeAddImageButtonShown()
        }
    }

    @Test
    fun successUploadMaxImageCount() {
        val uris = ArrayList<String>()
        for (i in 0 until MAX_IMAGE_COUNT) {
            uris.add(createImageAndGetUriString("${Date()}_$i"))
        }
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            uris.forEachIndexed { index, uri ->
                registerUploadPhoto(uri, index.toString())
            }
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(uris)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }

            for (i in 1..MAX_IMAGE_COUNT) {
                waitUntil { isSuccessImageItemShown(i, uris[i - 1]) }
            }

            isLargeAddImageButtonHidden()
            isSmallAddImageButtonHidden()
        }
    }

    @Test
    fun errorImageRetry() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerErrorUploadPhoto()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isErrorImageItemShown(1) }

            tapOn(lookup.matchesImageRetryButton(1))

            waitUntil { isSuccessImageItemShown(1, imageUri) }
        }
    }

    @Test
    fun errorImageBackPressedDeclineAndConfirm() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isErrorImageItemShown(1) }
            pressBack()

            performOnConfirmationDialog {
                isTitleEquals(getResourceString(R.string.image_picker_dialog_close_title))
                isMessageEquals(getResourceString(R.string.image_picker_dialog_close_message))
                isPositiveButtonTextEquals(getResourceString(R.string.yes))
                isNegativeButtonTextEquals(getResourceString(R.string.no))
                tapOn(lookup.matchesNegativeButton())
            }

            isCollapsedToolbarShown()
            pressBack()

            performOnConfirmationDialog {
                tapOn(lookup.matchesPositiveButton())
            }
        }

        performOnPublicationFormScreen {
            hasSellApartmentCollapsedToolbarTitle()
        }
    }

    @Test
    fun errorImageDonePressedDeclineAndConfirm() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isErrorImageItemShown(1) }
            tapOn(lookup.matchesDoneButton())
        }

        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.image_picker_dialog_done_title))
            isMessageEquals(getResourceString(R.string.image_picker_dialog_done_error_message))
            isPositiveButtonTextEquals(getResourceString(R.string.yes))
            isNegativeButtonTextEquals(getResourceString(R.string.no))
            tapOn(lookup.matchesNegativeButton())
        }

        performOnImagesPickerScreen {
            isCollapsedToolbarShown()
            tapOn(lookup.matchesDoneButton())
        }

        performOnConfirmationDialog {
            tapOn(lookup.matchesPositiveButton())
        }

        performOnPublicationFormScreen {
            hasSellApartmentCollapsedToolbarTitle()
        }
    }

    @Test
    fun uploadImagesDoneAndReenterToPickerScreen() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri) }

            tapOn(lookup.matchesDoneButton())
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesEditImagesButton())
            tapOn(lookup.matchesEditImagesButton())
        }

        performOnImagesPickerScreen {
            waitUntil { isSuccessImageItemShown(1, imageUri) }
        }
    }

    @Test
    fun switchImages() {
        val imageUri1 = createImageAndGetUriString("1")
        val imageUri2 = createImageAndGetUriString("2")
        val imageUri3 = createImageAndGetUriString("3")
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri1, "1")
            registerUploadPhoto(imageUri2, "2")
            registerUploadPhoto(imageUri3, "3")
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri1)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri1) }
            registerGetContentIntent(imageUri2)
            tapOn(lookup.matchesAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(2, imageUri2) }
            registerGetContentIntent(imageUri3)
            tapOn(lookup.matchesAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(3, imageUri3) }

            dragAndDropRight(lookup.matchesImageListItem(1))

            waitUntil { isSuccessImageItemShown(1, imageUri2) }
            isSuccessImageItemShown(2, imageUri1)
            isSuccessImageItemShown(3, imageUri3)

            dragAndDropDown(lookup.matchesImageListItem(1))

            waitUntil { isSuccessImageItemShown(1, imageUri3) }
            isSuccessImageItemShown(2, imageUri1)
            isSuccessImageItemShown(3, imageUri2)
        }
    }

    @Test
    fun pickSecondImageAndDeclineBackThenRevert() {
        val imageUri1 = createImageAndGetUriString("1")
        val imageUri2 = createImageAndGetUriString("2")
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri1, "1")
            registerUploadPhoto(imageUri2, "2")
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri1)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri1) }

            tapOn(lookup.matchesDoneButton())
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesEditImagesButton())
            tapOn(lookup.matchesEditImagesButton())
        }

        performOnImagesPickerScreen {
            waitUntil { isSuccessImageItemShown(1, imageUri1) }
            registerGetContentIntent(imageUri2)
            tapOn(lookup.matchesAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(2, imageUri2) }
            pressBack()
        }

        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.image_picker_dialog_close_title))
            isMessageEquals(getResourceString(R.string.image_picker_dialog_close_message))
            isPositiveButtonTextEquals(getResourceString(R.string.yes))
            isNegativeButtonTextEquals(getResourceString(R.string.no))
            tapOn(lookup.matchesNegativeButton())
        }

        performOnImagesPickerScreen {
            tapOn(lookup.matchesImageDeleteButton(2))
            pressBack()
        }

        performOnPublicationFormScreen {
            hasSellApartmentCollapsedToolbarTitle()
        }
    }

    @Test
    fun switchImagesAndDeclineBackThenRevert() {
        val imageUri1 = createImageAndGetUriString("1")
        val imageUri2 = createImageAndGetUriString("2")
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri1, "1")
            registerUploadPhoto(imageUri2, "2")
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri1)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri1) }
            registerGetContentIntent(imageUri2)
            tapOn(lookup.matchesAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(2, imageUri2) }
            tapOn(lookup.matchesDoneButton())
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesEditImagesButton())
            tapOn(lookup.matchesEditImagesButton())
        }

        performOnImagesPickerScreen {
            waitUntil { isSuccessImageItemShown(1, imageUri1) }
            dragAndDropRight(lookup.matchesImageListItem(1))
            waitUntil { isSuccessImageItemShown(1, imageUri2) }
            pressBack()
        }

        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.image_picker_dialog_close_title))
            isMessageEquals(getResourceString(R.string.image_picker_dialog_close_message))
            isPositiveButtonTextEquals(getResourceString(R.string.yes))
            isNegativeButtonTextEquals(getResourceString(R.string.no))
            tapOn(lookup.matchesNegativeButton())
        }

        performOnImagesPickerScreen {
            dragAndDropRight(lookup.matchesImageListItem(1))
            waitUntil { isSuccessImageItemShown(1, imageUri1) }
            pressBack()
        }

        performOnPublicationFormScreen {
            hasSellApartmentCollapsedToolbarTitle()
        }
    }

    @Test
    fun pickTwoSameImages() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri) }

            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }

            waitUntil {
                isToastShown(getResourceString(R.string.image_picker_image_same_image_selected))
            }
            isImageItemHidden(2)
        }
    }

    @Test
    fun brokenImageSelected() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            step("Мокаем не существующую фотографию") {
                registerGetContentIntent("file:///test.jpg")
            }
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil {
                isToastShown("Не\u00a0удалось загрузить 1 фото. Попробуйте выбрать другой файл.")
            }
            isImageItemHidden(1)
        }
    }

    @Test
    fun successUploadSingleImageFromCamera() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerImageCaptureIntentAndMockImage(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesCameraButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri) }
        }
    }

    @Test
    fun checkPublicationFormImage() {
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUploadPhoto(imageUri)
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            registerGetContentIntent(imageUri)
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, imageUri) }

            tapOn(lookup.matchesDoneButton())
        }

        performOnPublicationFormScreen {
            isEditImagesButtonShown()
            isImageGalleryShown()
            isImageShownInGallery(imageUri)
            isAddImagesButtonHidden()
        }
    }

    @Test
    fun checkChooseMediaButtons() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }
        prepareImagesPickerScreen()

        performOnImagesPickerScreen {
            collapseAppBar()
            waitUntil { isLargeAddImageButtonShown() }
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                isViewStateMatches("ImagesPickerTest/checkChooseMediaButtons")
            }
        }
    }

    private fun prepareImagesPickerScreen() {
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            isAddImagesButtonShown()
            isEditImagesButtonHidden()
            isImageGalleryHidden()
            tapOn(lookup.matchesAddImagesButton())
        }
    }

    private fun DispatcherRegistry.registerErrorUploadPhoto() {
        register(
            request {
                path("1.0/photo.json")
            },
            response {}
        )
    }

    companion object {
        const val MAX_IMAGE_COUNT = 20
    }
}

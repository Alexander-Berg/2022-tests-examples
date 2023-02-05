package ru.yandex.market.perftests.dsl

import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import ru.yandex.market.perftests.scenario.MarketPerfTest

private const val CMS_ROOT_RECYCLER_DESCRIPTION = "cms_root_recycler"
private const val PROFILE_SCROLL_DESCRIPTION = "profile_scroll"
private const val CHECKOUT_ROOT_RECYCLER_DESCRIPTION = "checkout_root_recycler"
private const val PRODUCT_ROOT_RECYCLER_DESCRIPTION = "product_root_recycler"
private const val SNIPPET_PRODUCT_IMAGE_DESCRIPTION = "snippet_product_image"
private const val CART_PRODUCT_IMAGE_DESCRIPTION = "cart_product_image"
private const val MEDIA_CAROUSEL_DESCRIPTION = "media_carousel"
private const val SEARCH_INPUT_DESCRIPTION = "search_input"
private const val NAVIGATE_TO_CART_DESCRIPTION = "navigate_to_cart"
private const val NAVIGATE_TO_PROFILE_DESCRIPTION = "navigate_to_profile"
private const val ADD_TO_CART_DESCRIPTION = "В корзину"
private const val DELETE_CART_ITEM_DESCRIPTION = "Удалить товар из корзины"

private const val LOGIN_BUTTON_DESCRIPTION = "login_button"
private const val CHECKOUT_BUTTON_DESCRIPTION = "checkout_button"
private const val LOGIN_ADD_ACCOUNT_BUTTON_ID = "button_other_account_single_mode"
private const val SEARCH_INPUT_ID = "search_text"
private const val NAVIGATE_TO_CATALOG_ID = "nav_catalog"
private const val CHECKOUT_MAP_VIEW_ID = "deliveryPointsMapView"
private const val SEARCH_VIEW_ID = "searchRequestView"
private const val ITEM_CATALOG_GRID_NODE_ID = "categoryImageView"
private const val ITEM_CATALOG_NODE_ID = "itemListBoxWithArrowTitle"
private const val APPLE_BRAND = "apple"
private const val APPLE_IPHONE_12 = "iphone 12"
private const val NAVIGATE_TO_MAIN_ID = "nav_main"
private const val MAIN_CATALOG_IMAGE_ID = "node_image"
private const val CATALOG_RECYCLER_ID = "fragmentCatalogRecyclerView"
private const val CART_RECYCLER_ID = "fragmentCartRecyclerView"
private const val CART_BOTTOM_PRICE_VIEW = "bottomPriceView"
private const val RECOMMENDATION_CATALOG_SCROLL_ID = "scroll_view"
private const val SEARCH_RECYCLER_ID = "searchResultListView"
private const val SEARCH_PRODUCT_PHOTO_ID = "photoSnippetBlock"
private const val HOME_CATEGORY_NAME = "Дом"
private const val FOOD_STAFF_CATEGORY_NAME = "Сковороды и сотейники"
private const val POPULAR_OFFERS_NAME = "Популярные предложения"
private const val SETTINGS_NAME = "Настройки"
private const val CATALOG_ROOT_RECYCLER_ID = "fragmentCatalogRootRecyclerView"
private const val LOGIN_NEXT_BUTTON_ID = "button_next"
private const val LOGIN_EDIT_LOGIN_ID = "edit_login"
private const val LOGIN_EDIT_PASSWORD_ID = "edit_password"
private const val WAIT_LOGIN_OBJECTS_TIMEOUT_MS = 5000L

private const val WAIT_BEFORE_MEASURE_DELAY_MS = 10000L
private const val IDLE_TIMEOUT_MS = 20000L
private const val WAIT_TIMEOUT_MS = 40000L

fun MarketPerfTest.findAddToCartInCmsRootRecycler(): UiObject {
    return findInRecyclerByDescription(
        recyclerDescription = CMS_ROOT_RECYCLER_DESCRIPTION,
        targetViewDescription = ADD_TO_CART_DESCRIPTION
    )
}

fun MarketPerfTest.findProductImageInCmsRootRecycler(): UiObject {
    return findInRecyclerByDescription(
        recyclerDescription = CMS_ROOT_RECYCLER_DESCRIPTION,
        targetViewDescription = SNIPPET_PRODUCT_IMAGE_DESCRIPTION
    )
}

fun MarketPerfTest.findProductImageInSearchRecycler(): UiObject {
    return findInRecyclerById(
        recyclerId = SEARCH_RECYCLER_ID,
        targetViewId = SEARCH_PRODUCT_PHOTO_ID
    )
}

fun MarketPerfTest.findProductImageInCatalogRecycler(): UiObject {
    return findInRecyclerByIdAndDescription(
        recyclerId = CATALOG_RECYCLER_ID,
        targetViewDescription = SNIPPET_PRODUCT_IMAGE_DESCRIPTION
    )
}

fun MarketPerfTest.findCatalogItem(): UiObject {
    return findInRecyclerById(
        recyclerId = CATALOG_RECYCLER_ID,
        targetViewId = ITEM_CATALOG_GRID_NODE_ID
    )
}

fun MarketPerfTest.findCatalogListItem(): UiObject {
    return findById(id = ITEM_CATALOG_NODE_ID)
}

fun MarketPerfTest.findFoodStaffCategoryInCatalogRecycler(): UiObject {
    return findInRecyclerByIdAndText(
        recyclerId = CATALOG_RECYCLER_ID,
        targetViewText = FOOD_STAFF_CATEGORY_NAME
    )
}

fun MarketPerfTest.findNavigateToCartButton(): UiObject {
    return findByDescription(
        description = NAVIGATE_TO_CART_DESCRIPTION
    )
}

fun MarketPerfTest.findCartDeleteItemButton(): UiObject {
    return findByDescription(DELETE_CART_ITEM_DESCRIPTION)
}

fun MarketPerfTest.findNavigateToCatalogButton(): UiObject {
    return findById(
        id = NAVIGATE_TO_CATALOG_ID
    )
}

fun MarketPerfTest.findSearchButton(): UiObject {
    return findById(
        id = SEARCH_VIEW_ID
    )
}

fun MarketPerfTest.findAppleBrand(): UiObject {
    return findByText(
        text = APPLE_BRAND
    )
}

fun MarketPerfTest.findIphone12(): UiObject {
    return findByText(
        text = APPLE_IPHONE_12
    )
}

fun MarketPerfTest.findNavigateToMainButton(): UiObject {
    return findById(
        id = NAVIGATE_TO_MAIN_ID
    )
}

fun MarketPerfTest.findHomeCategory(): UiObject {
    return findInRecyclerByIdAndText(
        recyclerId = RECOMMENDATION_CATALOG_SCROLL_ID,
        targetViewText = HOME_CATEGORY_NAME
    )
}

fun MarketPerfTest.findNavigateToProfileButton(): UiObject {
    return findByDescription(
        description = NAVIGATE_TO_PROFILE_DESCRIPTION
    )
}

fun MarketPerfTest.findNavigateToSettingsButton(): UiObject {
    return try {
        findInRecyclerByDescriptionAndText(
            recyclerDescription = PROFILE_SCROLL_DESCRIPTION,
            targetViewText = SETTINGS_NAME
        )
    } catch (e: Throwable) {
        findByText(SETTINGS_NAME)
    }
}

fun MarketPerfTest.disablePassportWebExperiment() {
    startMainActivity()
    waitForIdle()
    findNavigateToProfileButton().clickAndWaitForNewWindow()
    findNavigateToSettingsButton().clickAndWaitForNewWindow()
    findByText("Для разработчиков").clickAndWaitForNewWindow()
    findByText("Эксперименты паспорта").clickAndWaitForNewWindow()
    UiScrollable(UiSelector().scrollable(true)).scrollIntoView(findByText("web_am_on"))
    val exp = findByText("web_am_on")
    exp.click()
    val disablePassportExperiment = findByText("false")
    disablePassportExperiment.click()
    pressBack()
    waitForIdle()
    pressBack()
    waitForIdle()
    findNavigateToMainButton().clickAndWaitForNewWindow()
    waitForIdle()
}

fun MarketPerfTest.pressBack() {
    device.pressBack()
}

fun MarketPerfTest.login() {
    disablePassportWebExperiment()
    startMainActivity()

    findNavigateToProfileButton().clickAndWaitForNewWindow()
    findLoginButton().clickAndWaitForNewWindow()

    login("marketperftest", "MarketPerfTest-1")
    waitForIdle()
}

fun MarketPerfTest.findLoginButton(): UiObject {
    return findByDescription(
        description = LOGIN_BUTTON_DESCRIPTION
    )
}

fun MarketPerfTest.findCheckoutButton(): UiObject {
    return findByDescription(
        description = CHECKOUT_BUTTON_DESCRIPTION
    )
}

fun MarketPerfTest.waitCheckoutButton(): UiObject2 {
    return waitByDescription(
        CHECKOUT_BUTTON_DESCRIPTION, WAIT_TIMEOUT_MS
    )
}

fun MarketPerfTest.waitMediaCarousel() {
    waitByDescription(MEDIA_CAROUSEL_DESCRIPTION, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCatalogItem() {
    waitById(ITEM_CATALOG_NODE_ID, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitPopularOffersText() {
    waitByText(POPULAR_OFFERS_NAME, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitProductImage() {
    waitById(SEARCH_PRODUCT_PHOTO_ID, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCheckoutMap() {
    waitById(CHECKOUT_MAP_VIEW_ID, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitSearchInput() {
    waitByDescription(SEARCH_INPUT_DESCRIPTION, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCartProductImage() {
    waitByDescription(CART_PRODUCT_IMAGE_DESCRIPTION, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCheckoutRootRecycler() {
    waitByDescription(CHECKOUT_ROOT_RECYCLER_DESCRIPTION, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCatalogRecycler() {
    waitById(CATALOG_ROOT_RECYCLER_ID, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCatalogImage() {
    waitById(MAIN_CATALOG_IMAGE_ID, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitProductRootRecycler() {
    waitByDescription(PRODUCT_ROOT_RECYCLER_DESCRIPTION, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitCartActualization() {
    waitById(CART_BOTTOM_PRICE_VIEW, WAIT_TIMEOUT_MS)
}

fun MarketPerfTest.waitForIdle() {
    device.waitForIdle(IDLE_TIMEOUT_MS)
}

fun MarketPerfTest.openCatalogHomeCategory() {
    startMainActivity()
    waitForIdle()

    val navigateToCatalogButton = findNavigateToCatalogButton()
    navigateToCatalogButton.click()

    val homeCategory = findHomeCategory()
    waitForIdle()
    homeCategory.click()
    waitMediaCarousel()
}

fun MarketPerfTest.waitBeforeMeasure() {
    Thread.sleep(WAIT_BEFORE_MEASURE_DELAY_MS)
    waitForIdle()
}

fun MarketPerfTest.longWaitForIdle() {
    Thread.sleep(WAIT_BEFORE_MEASURE_DELAY_MS)
    waitForIdle()
}

fun MarketPerfTest.login(login: String, password: String) {

    val addAccountButton = findById(LOGIN_ADD_ACCOUNT_BUTTON_ID)
    if (addAccountButton.waitForExists(WAIT_LOGIN_OBJECTS_TIMEOUT_MS)) {
        addAccountButton.clickAndWaitForNewWindow()
    }

    val loginEdit = findById(LOGIN_EDIT_LOGIN_ID)
    loginEdit.text = login

    val nextAfterLogin = findById(LOGIN_NEXT_BUTTON_ID)
    nextAfterLogin.clickAndWaitForNewWindow()

    val passwordEdit = findById(LOGIN_EDIT_PASSWORD_ID)
    passwordEdit.text = password
    val nextAfterPassword = findById(LOGIN_NEXT_BUTTON_ID)
    nextAfterPassword.clickAndWaitForNewWindow()
}

fun MarketPerfTest.search(text: String) {
    val loginEdit = findById(SEARCH_INPUT_ID)
    loginEdit.text = text
}

fun MarketPerfTest.getCmsRootRecycler(): UiScrollable {
    return getRecyclerByDescription(CMS_ROOT_RECYCLER_DESCRIPTION)
}

fun MarketPerfTest.getSearchRootRecycler(): UiScrollable {
    return getRecyclerById(SEARCH_RECYCLER_ID)
}

fun MarketPerfTest.getProductRootRecycler(): UiScrollable {
    return getRecyclerByDescription(PRODUCT_ROOT_RECYCLER_DESCRIPTION)
}

fun MarketPerfTest.getCatalogRecycler(): UiScrollable {
    return getRecyclerById(CATALOG_RECYCLER_ID)
}

fun MarketPerfTest.getProfileRootRecycler(): UiScrollable? {
    return try {
        getRecyclerByDescription(PROFILE_SCROLL_DESCRIPTION)
    } catch (e: Throwable) {
        return null
    }
}

fun MarketPerfTest.getCartRootRecycler(): UiScrollable {
    return getRecyclerById(CART_RECYCLER_ID)
}

fun MarketPerfTest.getCheckoutRootRecycler(): UiScrollable {
    return getRecyclerByDescription(CHECKOUT_ROOT_RECYCLER_DESCRIPTION)
}
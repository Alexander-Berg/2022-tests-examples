package ru.yandex.autotests.mobile.disk.android.geoalbums

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.SavePublicResource
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.CreateUser
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.GeoAlbumsSteps
import ru.yandex.autotests.mobile.disk.android.steps.PhotosSteps
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@CreateUser
@Feature("Geo Albums")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class GeoAlbumsNewUserTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onAlbumsPage: GeoAlbumsSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Test
    @TmsLink("6888")
    @AuthorizationTest
    @SavePublicResource(url = FilesAndFolders.GEO_ZURICH_FOLDER_PUBLIC_URL)
    @Category(FullRegress::class)
    fun shouldCreateGeoAlbumsForNewUser() {
        onBasePage.openAlbums()
        onAlbumsPage.shouldDisplayGeoMetaAlbum()
    }
}

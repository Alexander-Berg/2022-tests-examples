package ru.yandex.market.clean.presentation.feature.sis.shopinfodialog

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.supplierTestInstance
import ru.yandex.market.common.android.ResourcesManager

class ShopInfoVoFormatterTest {

    private val supplier = supplierTestInstance()
    private val supplierWithoutOrganization = supplier.copy(
        organizations = emptyList()
    )
    private val supplierAddress = "$LEGAL_ADDRESS${supplier.organizations[0].address}"
    private val supplierOgrn = "$OGRN${supplier.organizations[0].ogrn}"
    private val supplierPhone = "$PHONE${supplier.organizations[0].contactPhone}"

    private val resourcesManager = mock<ResourcesManager>() {

        on { getString(R.string.dialog_about_shop_guarantees_subtitle) } doReturn GUARANTEES

        on { getString(R.string.dialog_about_shop_guarantees_info) } doReturn GUARANTEES_INFO

        on { getString(R.string.dialog_about_shop_shop_subtitle) } doReturn ABOUT_SHOP

        on { getString(R.string.dialog_about_shop_contact_subtitle) } doReturn CONTACT

        on {
            getFormattedString(
                R.string.dialog_about_shop_address,
                supplier.organizations[0].address
            )
        } doReturn supplierAddress

        on {
            getFormattedString(
                R.string.dialog_about_shop_ogrn,
                supplier.organizations[0].ogrn
            )
        } doReturn supplierOgrn

        on {
            getFormattedString(
                R.string.dialog_about_shop_phone,
                supplier.organizations[0].contactPhone
            )
        } doReturn supplierPhone

        on {
            getString(R.string.cart_retail_juridical_info_text)
        } doReturn INFO_TEXT

        on {
            getString(R.string.dialog_about_shop_seller_subtitle)
        } doReturn ABOUT_SHOP_SUBTITLE

        on {
            getString(R.string.dialog_about_shop_availability_subtitle)
        } doReturn AVAILABILITY_SUBTITLE

        on {
            getString(R.string.cart_retail_juridical_info_delivery_partners_title)
        } doReturn DELIVERY_PARTNERS_TITLE
    }

    private val formatter = ShopInfoVoFormatter(
        resourcesManager = resourcesManager
    )

    private val commonShopInfoVo = ShopInfoVo.CommonShopInfoVo(
        title = supplier.name,
        guaranteesSubtitle = GUARANTEES,
        guaranteesInfo = GUARANTEES_INFO,
        aboutShopSubtitle = ABOUT_SHOP,
        name = supplier.organizations[0].name,
        address = supplierAddress,
        ogrn = supplierOgrn,
        contactSubtitle = CONTACT,
        phone = supplierPhone,
    )

    @Test
    fun `Check common shop info vo formatter`() {
        assertThat(formatter.formatCommonInfo(supplier)).isEqualTo(commonShopInfoVo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Common shop info vo formatter throwing an exception`() {
        formatter.formatCommonInfo(supplierWithoutOrganization)
    }

    private val juredicalInfoVo = ShopInfoVo.JuredicalInfoVo(
        infoText = INFO_TEXT,
        aboutShopSubtitle = ABOUT_SHOP_SUBTITLE,
        organizationInfo = "${supplier.organizations[0].type.title} ${supplier.organizations[0].name}\n$LEGAL_ADDRESS${supplier.organizations[0].address}\n$OGRN${supplier.organizations[0].ogrn}\n$PHONE${supplier.organizations[0].contactPhone}",
        availabilitySubtitle = AVAILABILITY_SUBTITLE,
        availability = supplier.workSchedule,
        deliveryPartnersTitle = DELIVERY_PARTNERS_TITLE,
    )

    @Test
    fun `Check juredical shop info vo formatter`() {
        assertThat(formatter.formatJuredicalInfo(supplier)).isEqualTo(juredicalInfoVo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Juredical shop info vo formatter throwing an exception`() {
        formatter.formatJuredicalInfo(supplierWithoutOrganization)
    }

    companion object {
        private const val GUARANTEES = "Гарантии"
        private const val GUARANTEES_INFO =
            "Вернуть товар ненадлежащего качестов можно в течение 7 дней после покупки. :click:Подробнее:click:"
        private const val ABOUT_SHOP = "О магазине"
        private const val LEGAL_ADDRESS = "Юридический адрес: "
        private const val OGRN = "ОГРН: "
        private const val CONTACT = "Контакты"
        private const val PHONE = "Телефон: "
        private const val INFO_TEXT = "cart_retail_juridical_info_text"
        private const val ABOUT_SHOP_SUBTITLE = "dialog_about_shop_seller_subtitle"
        private const val AVAILABILITY_SUBTITLE = "dialog_about_shop_availability_subtitle"
        private const val DELIVERY_PARTNERS_TITLE = "cart_retail_juridical_info_delivery_partners_title"
    }
}
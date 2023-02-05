package ru.yandex.market.test.kakao.matchers

import android.view.View
import kotlinx.android.synthetic.main.view_delivery_types.view.courierButton
import kotlinx.android.synthetic.main.view_delivery_types.view.pvzButton
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.clean.presentation.feature.checkout.map.MapDeliveryType
import ru.yandex.market.clean.presentation.feature.checkout.map.view.DeliveryTypeSelectorView
import ru.yandex.market.uikit.button.ProgressButton

class DeliveryTypeSelectorMatcher(private val deliveryType: MapDeliveryType) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("DeliveryTypeSelectorView checking for delivery type $deliveryType")
    }

    override fun matchesSafely(deliveryTypeSelectorView: View): Boolean {
        return if (deliveryTypeSelectorView is DeliveryTypeSelectorView) {
            val activatedButton = getButtonByType(deliveryTypeSelectorView, deliveryType)
            activatedButton.isActivated
        } else {
            false
        }
    }

    private fun getButtonByType(
        deliveryTypeSelectorView: DeliveryTypeSelectorView,
        deliveryType: MapDeliveryType
    ): ProgressButton {
        return when (deliveryType) {
            MapDeliveryType.OUTLET -> deliveryTypeSelectorView.pvzButton
            MapDeliveryType.COURIER -> deliveryTypeSelectorView.courierButton
        }
    }

}

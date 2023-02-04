package ru.auto.ara.core.testdata

import androidx.annotation.DrawableRes
import ru.auto.ara.R
import ru.auto.ara.core.utils.getResourceString

const val OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER = "1097088908-fe0ff054"
const val OFFER_ID_WITH_ALL_ADVANTAGES = "1097088908-fe0ff055"

const val OFFER_ID_WITH_PROVEN_OWNER_ADVANTAGE = "1056093692-2f0b33d3"
const val OFFER_ID_WITH_NO_ACCIDENTS_ADVANTAGE = "1095669442-b3989724"
const val OFFER_ID_WITH_ONE_OWNER_ADVANTAGE = "1084044743-07c46fb3"
const val OFFER_ID_WITH_CERTIFICATE_MANUFACTURER = "1097265576-4c551050"
const val OFFER_ID_WITH_WARRANTY_ADVANTAGE = "1092718938-48ec2434"
const val OFFER_ID_WITH_ALMOST_NEW_ADVANTAGE = "1056093692-2f0b33d2"
const val OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE_WITH_DATA_IN_OFFER = "1089534836-475a5aca"
const val OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE = "1074918427-cdd53a67"
const val OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE_WITH_DATA_IN_OFFER = "1089335968-ed330052"
const val OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE = "1089249352-3dc864f3"
const val OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE = "1098212462-17b2c964"
const val OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_CHAT_ONLY = "1098212462-17b2c965"
const val OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_IS_DEALER = "1098212462-17b2c966"

data class OfferAdvantageTestParams(
    val advantage_tag: String,
    val test_name: String = advantage_tag,
    val position: Int,
    val offerIdWithSingleAdvantage: String,
    @DrawableRes val imageResId: Int,
    val title: String,
    val subtitle: String,
    val isWithDataInOffer: Boolean = false
) {
    val offerIdWithAllAdvantages =
        if (isWithDataInOffer) OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER else OFFER_ID_WITH_ALL_ADVANTAGES

    override fun toString(): String = test_name
}

val OFFER_ADVANTAGES = arrayOf(
    OfferAdvantageTestParams(
        advantage_tag = "proven_owner",
        position = 1,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_PROVEN_OWNER_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_proven_owner,
        title = getResourceString(R.string.offer_advantage_proven_owner_title),
        subtitle = getResourceString(R.string.offer_advantage_proven_owner_subtitle)
    ),
    OfferAdvantageTestParams(
        advantage_tag = "no_accidents",
        position = 2,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_NO_ACCIDENTS_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_no_accidents,
        title = getResourceString(R.string.offer_advantage_no_accidents_title),
        subtitle = getResourceString(R.string.offer_advantage_no_accidents_subtitle)
    ),
    OfferAdvantageTestParams(
        advantage_tag = "one_owner",
        position = 3,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_ONE_OWNER_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_one_owner,
        title = getResourceString(R.string.offer_advantage_one_owner_title),
        subtitle = getResourceString(R.string.offer_advantage_one_owner_subtitle)
    ),
    OfferAdvantageTestParams(
        advantage_tag = "certificate_manufacturer",
        position = 4,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_CERTIFICATE_MANUFACTURER,
        imageResId = R.drawable.ic_offer_advantage_certificate_manufacturer,
        title = getResourceString(R.string.offer_advantage_certificate_manufacturer_title),
        subtitle = "Mercedes-Benz Certified"
    ),
    OfferAdvantageTestParams(
        advantage_tag = "warranty",
        position = 5,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_WARRANTY_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_warranty,
        title = getResourceString(R.string.offer_advantage_warranty_title),
        subtitle = "До февраля 2025"
    ),
    OfferAdvantageTestParams(
        advantage_tag = "almost_new",
        position = 6,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_ALMOST_NEW_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_almost_new,
        title = getResourceString(R.string.offer_advantage_almost_new_title),
        subtitle = getResourceString(R.string.offer_advantage_almost_new_subtitle)
    ),
    OfferAdvantageTestParams(
        advantage_tag = "high_reviews_mark",
        test_name = "high_reviews_mark_with_data_in_offer",
        position = 8,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE_WITH_DATA_IN_OFFER,
        imageResId = R.drawable.ic_offer_advantage_high_reviews_mark,
        title = "4.8 / 5",
        subtitle = "Рейтинг по 300 отзывам",
        isWithDataInOffer = true
    ),
    OfferAdvantageTestParams(
        advantage_tag = "high_reviews_mark",
        test_name = "high_reviews_mark_without_data_in_offer",
        position = 7,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_HIGH_REVIEWS_MARK_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_high_reviews_mark,
        title = getResourceString(R.string.offer_advantage_high_reviews_mark_title_empty),
        subtitle = getResourceString(R.string.offer_advantage_high_reviews_mark_subtitle_empty)
    ),
    OfferAdvantageTestParams(
        advantage_tag = "stable_price",
        test_name = "stable_price_with_data_in_offer",
        position = 9,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE_WITH_DATA_IN_OFFER,
        imageResId = R.drawable.ic_offer_advantage_stable_price,
        title = getResourceString(R.string.offer_advantage_stable_price_title),
        subtitle = "Менее 3% в год",
        isWithDataInOffer = true
    ),
    OfferAdvantageTestParams(
        advantage_tag = "stable_price",
        test_name = "stable_price_without_data_in_offer",
        position = 8,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_stable_price,
        title = getResourceString(R.string.offer_advantage_stable_price_title_empty),
        subtitle = getResourceString(R.string.offer_advantage_stable_price_subtitle_empty)
    )
)

data class OfferAdvantageDescriptionTestParams(
    val advantageTagName: String,
    val singleAdvantageTagName: String = advantageTagName,
    val testName: String = advantageTagName,
    val position: Int,
    val offerIdWithSingleAdvantage: String,
    @DrawableRes val imageResId: Int,
    val title: String,
    val description: String,
    val action: String? = null
) {
    override fun toString(): String = testName
}

val OFFER_ADVANTAGE_DESCRIPTIONS = arrayOf(
    OfferAdvantageDescriptionTestParams(
        advantageTagName = "proven_owner",
        singleAdvantageTagName = "single_proven_owner",
        position = 1,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_PROVEN_OWNER_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_proven_owner,
        title = getResourceString(R.string.offer_advantage_proven_owner_title),
        description = getResourceString(R.string.offer_advantage_proven_owner_description)
    ),
    OfferAdvantageDescriptionTestParams(
        advantageTagName = "no_accidents",
        position = 2,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_NO_ACCIDENTS_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_no_accidents,
        title = getResourceString(R.string.offer_advantage_no_accidents_title),
        description = getResourceString(R.string.offer_advantage_no_accidents_description),
        action = getResourceString(R.string.show_full_report)
    ),
    OfferAdvantageDescriptionTestParams(
        advantageTagName = "one_owner",
        position = 3,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_ONE_OWNER_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_one_owner,
        title = getResourceString(R.string.offer_advantage_one_owner_title),
        description = getResourceString(R.string.offer_advantage_one_owner_description),
        action = getResourceString(R.string.show_full_report)
    ),
    OfferAdvantageDescriptionTestParams(
        advantageTagName = "warranty",
        position = 5,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_WARRANTY_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_warranty,
        title = "На гарантии до февраля 2025",
        description = getResourceString(R.string.offer_advantage_warranty_description)
    ),
    OfferAdvantageDescriptionTestParams(
        advantageTagName = "almost_new",
        position = 6,
        offerIdWithSingleAdvantage = OFFER_ID_WITH_ALMOST_NEW_ADVANTAGE,
        imageResId = R.drawable.ic_offer_advantage_almost_new,
        title = getResourceString(R.string.offer_advantage_almost_new_title),
        description = getResourceString(R.string.offer_advantage_almost_new_description)
    )
)

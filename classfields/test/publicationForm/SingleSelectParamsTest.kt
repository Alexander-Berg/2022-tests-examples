package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.PublicationFormRobot
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 28.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SingleSelectParamsTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    private val fields = mutableMapOf<String, Any>()
    private lateinit var offerBody: String

    @Test
    fun checkSellApartmentFields() {
        configureWebServer {
            registerUserProfile()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()

        offerBody = """
            {
              "offer": {
                "type": "SELL",
                "category": "APARTMENT",
                "currency": "RUR",
                "imageOrderChangeAllowed": true,
                "rooms": [],
                $FIELDS_PLACEHOLDER
              }
            }
        """.trimIndent()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }

            checkField(
                "roomsTotalApartment",
                lookup.matchesRoomsCountField(),
                lookup.matchesRoomsSelectorStudio() to "STUDIO",
                lookup.matchesRoomsSelectorOne() to "1",
                lookup.matchesRoomsSelectorTwo() to "2",
                lookup.matchesRoomsSelectorThree() to "3",
                lookup.matchesRoomsSelectorFour() to "4",
                lookup.matchesRoomsSelectorFive() to "5",
                lookup.matchesRoomsSelectorSix() to "6",
                lookup.matchesRoomsSelectorSeven() to "7"
            )

            checkField(
                "bathroomUnit",
                lookup.matchesBathroomField(),
                lookup.matchesBathroomSelectorMatched() to "MATCHED",
                lookup.matchesBathroomSelectorSeparated() to "SEPARATED",
                lookup.matchesBathroomSelectorTwoAndMore() to "TWO_AND_MORE"
            )

            checkField(
                "balcony",
                lookup.matchesBalconyField(),
                lookup.matchesBalconySelectorNone() to "NONE",
                lookup.matchesBalconySelectorBalcony() to "BALCONY",
                lookup.matchesBalconySelectorLoggia() to "LOGGIA",
                lookup.matchesBalconySelectorBalconyAndLoggia() to "BALCONY_LOGGIA",
                lookup.matchesBalconySelectorTwoBalcony() to "TWO_BALCONY",
                lookup.matchesBalconySelectorTwoLoggia() to "TWO_LOGGIA"
            )

            checkField(
                "renovation",
                lookup.matchesRenovationField(),
                lookup.matchesRenovationSelectorCosmetic() to "COSMETIC_DONE",
                lookup.matchesRenovationSelectorEuro() to "EURO",
                lookup.matchesRenovationSelectorDesigner() to "DESIGNER_RENOVATION",
                lookup.matchesRenovationSelectorNeedRenovation() to "NEEDS_RENOVATION"
            )

            checkField(
                "parkingType",
                lookup.matchesParkingTypeField(),
                lookup.matchesParkingTypeSelectorClosed() to "CLOSED",
                lookup.matchesParkingTypeSelectorOpen() to "OPEN",
                lookup.matchesParkingTypeSelectorUnderground() to "UNDERGROUND"
            )

            checkField(
                "buildingType",
                lookup.matchesBuildingTypeField(),
                lookup.matchesBuildingTypeSelectorBrick() to "BRICK",
                lookup.matchesBuildingTypeSelectorBlock() to "BLOCK",
                lookup.matchesBuildingTypeSelectorPanel() to "PANEL",
                lookup.matchesBuildingTypeSelectorMonolit() to "MONOLIT",
                lookup.matchesBuildingTypeSelectorMonolitBrick() to "MONOLIT_BRICK"
            )

            checkField(
                "dealStatus",
                lookup.matchesDealStatusField(),
                lookup.matchesDealStatusSelectorSale() to "SALE",
                lookup.matchesDealStatusSelectorCountersale() to "COUNTERSALE",
                lookup.matchesDealStatusSelectorReassignment() to "REASSIGNMENT"
            )

            checkField(
                "apartments",
                lookup.matchesPropertyStatusField(),
                lookup.matchesPropertyStatusSelectorApartments() to true,
                lookup.matchesPropertyStatusSelectorHousingStock() to false
            )

            checkField(
                "newFlat",
                lookup.matchesHouseReadyStatusField(),
                lookup.matchesHouseReadyStatusSelectorNew() to true,
                lookup.matchesHouseReadyStatusSelectorSecondary() to false
            )
        }
    }

    @Test
    fun checkSellRoomsFields() {
        configureWebServer {
            registerUserProfile()
        }
        draftRule.prepareSellRoom()
        activityTestRule.launchActivity()

        offerBody = """
            {
              "offer": {
                "type": "SELL",
                "category": "ROOMS",
                "currency": "RUR",
                "imageOrderChangeAllowed": true,
                "rooms": [],
                $FIELDS_PLACEHOLDER
              }
            }
        """.trimIndent()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellRoomCollapsedToolbarTitle() }

            checkField(
                "roomsTotal",
                lookup.matchesRoomsCountField(),
                lookup.matchesRoomsSelectorThree() to "3",
                lookup.matchesRoomsSelectorFour() to "4",
                lookup.matchesRoomsSelectorFive() to "5",
                lookup.matchesRoomsSelectorSix() to "6",
                lookup.matchesRoomsSelectorSeven() to "7"
            )

            checkField(
                "roomsOffered",
                lookup.matchesRoomsOfferedField(),
                lookup.matchesRoomsOfferedSelectorOne() to "1",
                lookup.matchesRoomsOfferedSelectorTwo() to "2",
                lookup.matchesRoomsOfferedSelectorThree() to "3",
                lookup.matchesRoomsOfferedSelectorFour() to "4",
                lookup.matchesRoomsOfferedSelectorFive() to "5",
                lookup.matchesRoomsOfferedSelectorSix() to "6"
            )

            checkField(
                "floorCovering",
                lookup.matchesFloorCoveringField(),
                lookup.matchesFloorCoveringSelectorLinoleum() to "LINOLEUM",
                lookup.matchesFloorCoveringSelectorLaminat() to "LAMINATED_FLOORING_BOARD",
                lookup.matchesFloorCoveringSelectorParquet() to "PARQUET",
                lookup.matchesFloorCoveringSelectorGlazed() to "GLAZED"
            )
        }
    }

    @Test
    fun checkSellHouseFields() {
        configureWebServer {
            registerUserProfile()
        }
        draftRule.prepareSellHouse()
        activityTestRule.launchActivity()

        offerBody = """
            {
              "offer": {
                "type": "SELL",
                "category": "HOUSE",
                "currency": "RUR",
                "imageOrderChangeAllowed": true,
                $FIELDS_PLACEHOLDER
              }
            }
        """.trimIndent()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellHouseCollapsedToolbarTitle() }

            fields["lotAreaUnit"] = "SOTKA"

            checkField(
                "houseType",
                lookup.matchesHouseTypeField(),
                lookup.matchesHouseTypeSelectorHouse() to "HOUSE",
                lookup.matchesHouseTypeSelectorPartHouse() to "PARTHOUSE",
                lookup.matchesHouseTypeSelectorTownhouse() to "TOWNHOUSE",
                lookup.matchesHouseTypeSelectorDuplex() to "DUPLEX"
            )

            checkField(
                "toilet",
                lookup.matchesToiletField(),
                lookup.matchesToiletSelectorInside() to "INSIDE",
                lookup.matchesToiletSelectorOutside() to "OUTSIDE",
                lookup.matchesToiletSelectorNone() to "NONE"
            )

            checkField(
                "shower",
                lookup.matchesShowerField(),
                lookup.matchesShowerSelectorInside() to "INSIDE",
                lookup.matchesShowerSelectorOutside() to "OUTSIDE",
                lookup.matchesShowerSelectorNone() to "NONE"
            )

            checkField(
                "lotAreaUnit",
                lookup.matchesLotAreaUnitField(),
                lookup.matchesLotAreaUnitSelectorHectare() to "HECTARE"
            )

            checkField(
                "lotType",
                lookup.matchesLotTypeField(),
                lookup.matchesLotTypeSelectorIGS() to "IGS",
                lookup.matchesLotTypeSelectorFarm() to "FARM",
                lookup.matchesLotTypeSelectorGarden() to "GARDEN"
            )
        }
    }

    @Test
    fun checkSellGarageFields() {
        configureWebServer {
            registerUserProfile()
        }
        draftRule.prepareSellGarage()
        activityTestRule.launchActivity()

        offerBody = """
            {
              "offer": {
                "type": "SELL",
                "category": "GARAGE",
                "currency": "RUR",
                "imageOrderChangeAllowed": true,
                $FIELDS_PLACEHOLDER
              }
            }
        """.trimIndent()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }

            checkField(
                "garageType",
                lookup.matchesGarageTypeField(),
                lookup.matchesGarageTypeSelectorGarage() to "GARAGE",
                lookup.matchesGarageTypeSelectorParkingPlace() to "PARKING_PLACE",
                lookup.matchesGarageTypeSelectorBox() to "BOX"
            )

            checkField(
                "parkingType",
                lookup.matchesGarageParkingTypeField(),
                lookup.matchesGarageParkingTypeSelectorUnderground() to "UNDERGROUND",
                lookup.matchesGarageParkingTypeSelectorGround() to "GROUND",
                lookup.matchesGarageParkingTypeSelectorMultilevel() to "MULTILEVEL"
            )

            checkField(
                "buildingType",
                lookup.matchesGarageBuildingTypeField(),
                lookup.matchesGarageBuildingTypeSelectorBrick() to "BRICK",
                lookup.matchesGarageBuildingTypeSelectorMetal() to "METAL",
                lookup.matchesGarageBuildingTypeSelectorFerroconcrete() to "FERROCONCRETE"
            )

            checkField(
                "garageOwnership",
                lookup.matchesGarageOwnershipField(),
                lookup.matchesGarageOwnershipSelectorPrivate() to "PRIVATE",
                lookup.matchesGarageOwnershipSelectorCooperative() to "COOPERATIVE",
                lookup.matchesGarageOwnershipSelectorByProxy() to "BY_PROXY"
            )
        }
    }

    private fun PublicationFormRobot.checkField(
        fieldName: String,
        fieldMatcher: NamedViewMatcher,
        vararg valueMatchers: Pair<NamedViewMatcher, Any>
    ) {
        configureWebServer {
            for ((_, value) in valueMatchers) {
                registerUserProfilePatch()

                fields[fieldName] = value
                val fieldsValue = fields.entries.joinToString {
                    "\"${it.key}\": ${if (it.value is String) "\"${it.value}\"" else it.value}"
                }
                val requestBody = offerBody.replace(FIELDS_PLACEHOLDER, fieldsValue)

                val responseBody = """
                        {
                            "error": {
                                "codename": "JSON_VALIDATION_ERROR",
                                "data": {
                                    "validationErrors": [
                                        {
                                            "parameter": "/$fieldName",
                                            "code": "${fieldName}_required",
                                            "localizedDescription": "$fieldName: $value"
                                        }
                                    ],
                                    "valid": false
                                }
                            }
                        }
                """.trimIndent()

                registerValidationError(requestBody, responseBody)
            }
        }

        for ((valueMatcher, value) in valueMatchers) {
            scrollToPosition(fieldMatcher)
            onView(valueMatcher).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(fieldMatcher).check(matches(isCompletelyDisplayed()))
                containsValidationError("$fieldName: $value")
            }
        }
    }

    private fun DispatcherRegistry.registerValidationError(
        requestBody: String,
        responseBody: String
    ) {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
                body(requestBody)
            },
            response {
                setResponseCode(400)
                setBody(responseBody)
            }
        )
    }

    private companion object {

        const val FIELDS_PLACEHOLDER = "%fieldsPlaceholder"
    }
}

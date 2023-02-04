package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.google.gson.JsonArray
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InventoryPreviewScreen
import com.yandex.mobile.realty.core.screen.RentContractScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerTenantRentFlat
import com.yandex.mobile.realty.test.yandexrent.inventory.INVENTORY_VERSION
import com.yandex.mobile.realty.test.yandexrent.inventory.inventoryResponse
import com.yandex.mobile.realty.test.yandexrent.inventory.registerInventory
import com.yandex.mobile.realty.test.yandexrent.inventory.sampleRooms
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 11.07.2022
 */
@LargeTest
class RentFlatDocsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        MetricaEventsRule(),
        activityTestRule
    )

    @Test
    fun showEmptyDocsList() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocsError()
            registerDocs()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("error"))
                .retryButton
                .click()

            emptyDocsView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("emptyDocs"))
        }
    }

    @Test
    fun showOnlyOneUpdateFallback() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(docsWithUpdateFallback())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerMarketIntent()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            val fallbackItem = notificationItem(
                getResourceString(R.string.yandex_rent_update_fallback_title)
            )

            waitUntil { listView.contains(fallbackItem) }
            listView.isContentStateMatches(getTestRelatedFilePath("docs"))
            listView.scrollTo(fallbackItem)
                .invoke { actionButton.click() }

            intended(matchesMarketIntent())
        }
    }

    @Test
    fun openDownloadableDoc() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(ownerDocs())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(KEYS_HANDOVER_TITLE, KEYS_HANDOVER_SUBTITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(KEYS_HANDOVER_ANALYTICS).waitUntil { isOccurred() }
        }
    }

    @Test
    fun openUrlFallback() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(ownerDocs())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(FALLBACK_URL))

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(NEW_DOC_TITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(NEW_DOC_ANALYTICS).waitUntil { isOccurred() }
            intended(matchesExternalViewUrlIntent(FALLBACK_URL))
        }
    }

    @Test
    fun showOwnerDocuments() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(ownerDocs())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(KEYS_HANDOVER_TITLE, KEYS_HANDOVER_SUBTITLE)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(getTestRelatedFilePath("docs"))
        }
    }

    @Test
    fun openOwnerContract() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(ownerDocs())
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(CONTRACT_TITLE, CONTRACT_SUBTITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(CONTRACT_ANALYTICS).waitUntil { isOccurred() }
        }

        onScreen<RentContractScreen> {
            waitUntil { downloadButton.isCompletelyDisplayed() }

            listView.isContentStateMatches(getTestRelatedFilePath("content"))
            downloadButton.click()

            event("Аренда. ДА. Собственник. Переход к просмотру полной версии ДА")
                .waitUntil { isOccurred() }
        }
    }

    @Test
    fun openSignedOwnerInventory() {
        configureWebServer {
            registerOwnerRentFlat()
            registerDocs(ownerDocs())
            registerInventory(
                version = INVENTORY_VERSION,
                inventoryResponse = inventoryResponse(
                    version = INVENTORY_VERSION,
                    rooms = sampleRooms(),
                    confirmedByOwner = true,
                    confirmedByTenant = true
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(INVENTORY_TITLE, INVENTORY_SUBTITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(INVENTORY_ANALYTICS).waitUntil { isOccurred() }
        }

        onScreen<InventoryPreviewScreen> {
            waitUntil { downloadButton.isCompletelyDisplayed() }

            listView.isContentStateMatches(getTestRelatedFilePath("content"))
            downloadButton.click()

            event("Аренда. Опись. Собственник. Скачивание описи").waitUntil { isOccurred() }
        }
    }

    @Test
    fun showTenantDocuments() {
        configureWebServer {
            registerTenantRentFlat()
            registerDocs(tenantDocs())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(KEYS_HANDOVER_TITLE, KEYS_HANDOVER_SUBTITLE)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(getTestRelatedFilePath("docs"))
        }
    }

    @Test
    fun openTenantContract() {
        configureWebServer {
            registerTenantRentFlat()
            registerDocs(tenantDocs())
            registerContractSummary()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(CONTRACT_TITLE, CONTRACT_SUBTITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(CONTRACT_ANALYTICS).waitUntil { isOccurred() }
        }

        onScreen<RentContractScreen> {
            waitUntil { downloadButton.isCompletelyDisplayed() }

            listView.isContentStateMatches(getTestRelatedFilePath("content"))
            downloadButton.click()

            event("Аренда. ДА. Жилец. Переход к просмотру полной версии ДА")
                .waitUntil { isOccurred() }
        }
    }

    @Test
    fun openSignedTenantInventory() {
        configureWebServer {
            registerTenantRentFlat()
            registerDocs(tenantDocs())
            registerInventory(
                version = INVENTORY_VERSION,
                inventoryResponse = inventoryResponse(
                    version = INVENTORY_VERSION,
                    rooms = sampleRooms(),
                    confirmedByOwner = true,
                    confirmedByTenant = true
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .invoke { documentsButton.click() }

            docItem(INVENTORY_TITLE, INVENTORY_SUBTITLE)
                .waitUntil { listView.contains(this) }
                .invoke { contentView.click() }

            clickOnDocEvent(INVENTORY_ANALYTICS).waitUntil { isOccurred() }
        }

        onScreen<InventoryPreviewScreen> {
            waitUntil { downloadButton.isCompletelyDisplayed() }

            listView.isContentStateMatches(getTestRelatedFilePath("content"))
            downloadButton.click()

            event("Аренда. Опись. Жилец. Скачивание описи").waitUntil { isOccurred() }
        }
    }

    private fun clickOnDocEvent(docType: String): EventMatcher {
        return event("Аренда. Карточка квартиры. Тэп по документу") {
            "Документ" to docType
        }
    }

    private fun ownerDocs(): JsonArray {
        return jsonArrayOf(
            jsonObject {
                "beginning" to "2022-01-10"
                "contractCounterparty" to "Павел Иванов"
                "documents" to jsonArrayOf(
                    jsonObject {
                        "title" to KEYS_HANDOVER_TITLE
                        "subtitle" to "№ 1999-2022/АКТ от 10.01.2022"
                        "analyticsType" to KEYS_HANDOVER_ANALYTICS
                        "downloadToken" to "56641ea4011511edb9390242ac120002"
                        "common" to jsonObject {}
                    }
                )
            },
            jsonObject {
                "beginning" to "2020-12-04"
                "end" to "2022-01-04"
                "contractCounterparty" to "Леонид Семёнов"
                "documents" to jsonArrayOf(
                    jsonObject {
                        "title" to KEYS_HANDOVER_TITLE
                        "subtitle" to KEYS_HANDOVER_SUBTITLE
                        "analyticsType" to KEYS_HANDOVER_ANALYTICS
                        "downloadToken" to "2af495ea011611edb9390242ac120002"
                        "common" to jsonObject {}
                    },
                    jsonObject {
                        "title" to CONTRACT_TITLE
                        "subtitle" to CONTRACT_SUBTITLE
                        "analyticsType" to CONTRACT_ANALYTICS
                        "rentContract" to jsonObject {
                            "id" to CONTRACT_ID
                        }
                    },
                    jsonObject {
                        "title" to INVENTORY_TITLE
                        "subtitle" to INVENTORY_SUBTITLE
                        "analyticsType" to INVENTORY_ANALYTICS
                        "confirmedInventory" to jsonObject {
                            "ownerRequestId" to OWNER_REQUEST_ID
                            "version" to INVENTORY_VERSION
                        }
                    },
                    jsonObject {
                        "title" to NEW_DOC_TITLE
                        "analyticsType" to NEW_DOC_ANALYTICS
                        "fallback" to jsonObject {
                            "action" to jsonObject {
                                "url" to FALLBACK_URL
                            }
                        }
                    }
                )
            }
        )
    }

    private fun tenantDocs(): JsonArray {
        return jsonArrayOf(
            jsonObject {
                "beginning" to "2020-12-04"
                "documents" to jsonArrayOf(
                    jsonObject {
                        "title" to KEYS_HANDOVER_TITLE
                        "subtitle" to KEYS_HANDOVER_SUBTITLE
                        "analyticsType" to KEYS_HANDOVER_ANALYTICS
                        "downloadToken" to "2af495ea011611edb9390242ac120002"
                        "common" to jsonObject {}
                    },
                    jsonObject {
                        "title" to CONTRACT_TITLE
                        "subtitle" to CONTRACT_SUBTITLE
                        "analyticsType" to CONTRACT_ANALYTICS
                        "rentContract" to jsonObject {
                            "id" to CONTRACT_ID
                        }
                    },
                    jsonObject {
                        "title" to INVENTORY_TITLE
                        "subtitle" to INVENTORY_SUBTITLE
                        "analyticsType" to INVENTORY_ANALYTICS
                        "confirmedInventory" to jsonObject {
                            "ownerRequestId" to OWNER_REQUEST_ID
                            "version" to INVENTORY_VERSION
                        }
                    }
                )
            }
        )
    }

    private fun docsWithUpdateFallback(): JsonArray {
        return jsonArrayOf(
            jsonObject {
                "beginning" to "2022-01-10"
                "contractCounterparty" to "Павел Иванов"
                "documents" to jsonArrayOf(
                    jsonObject {
                        "title" to "Неизвестный документ"
                        "fallback" to jsonObject {
                            "updateAction" to jsonObject { }
                        }
                    },
                    jsonObject {
                        "title" to "Неизвестный документ"
                        "fallback" to jsonObject {
                            "updateAction" to jsonObject { }
                        }
                    }
                )
            },
            jsonObject {
                "beginning" to "2020-12-04"
                "end" to "2022-01-04"
                "contractCounterparty" to "Леонид Семёнов"
                "documents" to jsonArrayOf(
                    jsonObject {
                        "title" to KEYS_HANDOVER_TITLE
                        "subtitle" to "№ 1777-2020/АКТ • от 04.12.2020"
                        "analyticsType" to KEYS_HANDOVER_ANALYTICS
                        "downloadToken" to "2af495ea011611edb9390242ac120002"
                        "common" to jsonObject {}
                    },
                    jsonObject {
                        "title" to "Неизвестный документ"
                        "fallback" to jsonObject {
                            "updateAction" to jsonObject { }
                        }
                    }
                )
            }
        )
    }

    private fun DispatcherRegistry.registerDocs(docs: JsonArray = jsonArrayOf()) {
        register(
            request {
                path("2.0/rent/user/me/flat/$FLAT_ID/documents/all")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "documents" to docs
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerDocsError() {
        register(
            request {
                path("2.0/rent/user/me/flat/$FLAT_ID/documents/all")
            },
            response {
                error()
            }
        )
    }

    private fun DispatcherRegistry.registerContractSummary() {
        register(
            request {
                path("2.0/rent/user/me/contracts/$CONTRACT_ID/summary")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "summary" to jsonObject {
                            "contractId" to CONTRACT_ID
                            "status" to "ACTIVE"
                            "summary" to contractSummary()
                            "faq" to jsonArrayOf(
                                jsonObject {
                                    "id" to "1"
                                    "question" to "Текст вопроса"
                                    "answer" to "Текст ответа"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private companion object {

        const val FALLBACK_URL = "https://arenda.test.vertis.yandex.ru/docs"
        const val KEYS_HANDOVER_TITLE = "Акт передачи ключей"
        const val KEYS_HANDOVER_SUBTITLE = "№ 1777-2020/АКТ от 04.12.2020"
        const val KEYS_HANDOVER_ANALYTICS = "Акт-передачи-ключей"
        const val NEW_DOC_TITLE = "Новый вид документа"
        const val NEW_DOC_ANALYTICS = "Новый-вид-документа"
        const val CONTRACT_TITLE = "Договор аренды"
        const val CONTRACT_SUBTITLE = "№ 1777-2020/ДА от 04.12.2020"
        const val CONTRACT_ANALYTICS = "Договор-аренды"
        const val INVENTORY_TITLE = "Опись имущества"
        const val INVENTORY_SUBTITLE = "№ 1777-2020/ОП от 04.12.2020"
        const val INVENTORY_ANALYTICS = "Опись-имущества"
    }
}

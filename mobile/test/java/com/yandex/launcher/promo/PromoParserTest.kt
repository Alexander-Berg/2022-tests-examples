package com.yandex.launcher.promo

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.yandex.launcher.BaseRobolectricTest
import okio.buffer
import okio.source
import org.junit.Test

class PromoParserTest : BaseRobolectricTest() {

    companion object {
        private const val EXAMPLE_ID = "example_settings_promo_block"
        private const val EXAMPLE_TITLE = "Безлимит для фото на Яндекс.Диске"
        private const val EXAMPLE_DESCRIPTION = "Бесплатное хранилище для снимков с вашего телефона"
        private const val EXAMPLE_IMAGE = "https://launcher.tst.mobile.yandex.net/get-lnchr-s3/settings_promo_block/PIC_3f7e872429a0d5f25b53920bcbb3795e.jpg"
        private const val EXAMPLE_PRIORITY = 500
        private const val EXAMPLE_BADGE = true
        private const val EXAMPLE_CONDITION_1_NAME = "login"
        private const val EXAMPLE_CONDITION_1_VALUE = true
        private const val EXAMPLE_CONDITION_2_NAME = "age_more_than"
        private const val EXAMPLE_CONDITION_2_VALUE = 20L
        private const val EXAMPLE_CONDITION_3_NAME = "higher_than"
        private const val EXAMPLE_CONDITION_3_VALUE = 180.3f
        private const val EXAMPLE_SKIP_ID = "cancel"
        private const val EXAMPLE_SKIP_STYLE = PromoBlock.PromoButton.BUTTON_STYLE_TRANSPARENT
        private const val EXAMPLE_SKIP_CAPTION = "Не сейчас"
        private const val EXAMPLE_PRIMARY_ID = "some_action"
        private const val EXAMPLE_PRIMARY_STYLE = PromoBlock.PromoButton.BUTTON_STYLE_PRIMARY
        private const val EXAMPLE_PRIMARY_CAPTION = "Сделать хорошо"
        private const val EXAMPLE_PRIMARY_ACTION = "lnchr://do_awesome"

        private val EXAMPLE_ANSWER_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "show_conditions":
                        {
                            "$EXAMPLE_CONDITION_1_NAME": $EXAMPLE_CONDITION_1_VALUE
                        },
                        "buttons": [
                            {
                                "id": "$EXAMPLE_SKIP_ID",
                                "style": "$EXAMPLE_SKIP_STYLE",
                                "caption": "$EXAMPLE_SKIP_CAPTION"
                            },
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_SESSION_SKIPS_JSON = """
            {
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_IMAGE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_ID_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_TITLE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_DESCRIPTION_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_PRIORITY_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_SHOW_CONDITIONS_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_BUTTONS_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE
                    }
                ]
            }
        """.trimIndent()
        private val MISSING_BADGE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val ONE_BUTTON_VALID_STYLE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val ONE_BUTTON_INVALID_STYLE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_SKIP_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val TWO_BUTTONS_VALID_STYLE_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_SKIP_ID",
                                "style": "$EXAMPLE_SKIP_STYLE",
                                "caption": "$EXAMPLE_SKIP_CAPTION"
                            },
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val TWO_BUTTONS_INVALID_BOTH_SKIP_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_SKIP_ID",
                                "style": "$EXAMPLE_SKIP_STYLE",
                                "caption": "$EXAMPLE_SKIP_CAPTION"
                            },
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_SKIP_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val TWO_BUTTONS_INVALID_BOTH_PRIMARY_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "buttons": [
                            {
                                "id": "$EXAMPLE_SKIP_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_SKIP_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            },
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val MULTIPLE_CONDITIONS_JSON = """
            {
                "session_skips_after_hide": 5,
                "blocks": [
                    {
                        "id": "$EXAMPLE_ID",
                        "title": "$EXAMPLE_TITLE",
                        "description": "$EXAMPLE_DESCRIPTION",
                        "image": "$EXAMPLE_IMAGE",
                        "priority": $EXAMPLE_PRIORITY,
                        "badge": $EXAMPLE_BADGE,
                        "show_conditions":
                        {
                            "$EXAMPLE_CONDITION_1_NAME": $EXAMPLE_CONDITION_1_VALUE,
                            "$EXAMPLE_CONDITION_2_NAME": $EXAMPLE_CONDITION_2_VALUE,
                            "$EXAMPLE_CONDITION_3_NAME": $EXAMPLE_CONDITION_3_VALUE
                        },
                        "buttons": [
                            {
                                "id": "$EXAMPLE_PRIMARY_ID",
                                "style": "$EXAMPLE_PRIMARY_STYLE",
                                "caption": "$EXAMPLE_PRIMARY_CAPTION",
                                "action": "$EXAMPLE_PRIMARY_ACTION"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        private val REAL_JSON_RESPONSE = """
            {"blocks":[
                    {"badge":false,
                     "priority":300,
                     "description":"Promo 1, with image, Timout 25 seconds, showing new wallpapers. ",
                     "title":"Test Promo 1",
                     "image":"https://launcher.tst.mobile.yandex.net/resizer?url=https%3A%2F%2Flnchr-files.s3.mdst.yandex.net%2Fsettings_promo_block%2FPIC_3f7e872429a0d5f25b53920bcbb3795e.jpg&width=1020&height=300&typemap=gif%3Agif%3Bpng%3Apng%3B%2A%3Ajpeg%3B&crop=yes&enlarge=yes&quality=92&key=a29aff4a619f89847adb424b53bdddc8",
                     "show_conditions":{"last_shown_delay": 25000},
                     "buttons":[
                            {"action":"lnchr://yandex.com/activity/wallpapers",
                             "caption":"See new wallpapers",
                             "style":"primary",
                             "id":"wallpapers"},
                            {"caption":"Later",
                             "style":"transparent",
                             "id":"skip"}
                     ],
                     "id":"test_settings_promo_block_2"},
                    {"priority":500,
                     "description":"Promo 2. Priority 500 with image and badge. Storka check activated. Tap the button to activate Launcher's Search Widget",
                     "badge":true,
                     "title":"Test Promo  2.",
                     "image":"https://launcher.tst.mobile.yandex.net/resizer?url=https%3A%2F%2Flnchr-files.s3.mdst.yandex.net%2Fsettings_promo_block%2Fbeach-sunset-hd-wallpapers-beautiful-desktop-widescreen-backgrounds-images-desktop-wallpaper-stock-photos-artworks-1920x1080_e1675cd4e444dc241f7d28b802f47fec.jpg&width=1020&height=300&typemap=gif%3Agif%3Bpng%3Apng%3B%2A%3Ajpeg%3B&crop=yes&enlarge=yes&quality=92&key=7921d07b19cefc2f69958707ad0a907e",
                     "show_conditions":{"shtorka_is_shown":false,"last_shown_delay": 20000},
                     "buttons":[
                            {"action":"lnchr://yandex.com/activity/forceSearchBar",
                             "caption":"Change Widget",
                             "style":"primary",
                             "id":"example_button_2"}],
                     "id":"test_settings_promo_block_1"}
                ],
                "session_skips_after_hide":1}
        """.trimIndent()
        private const val EMPTY_BLOCKS_RESPONSE = """{"blocks":[], "session_skips_after_hide": 0}"""

        private fun checkSessionSkips(response: PromoResponse) {
            assertThat(response.sessionSkipsAfterHide, equalTo(5))
        }

        private fun checkOneBlock(response: PromoResponse): PromoBlock {
            assertThat(response.blocks, present())
            assertThat(response.blocks.size, equalTo(1))
            return response.blocks[0]
        }

        private fun checkId(promoBlock: PromoBlock) {
            assertThat(promoBlock.id, equalTo(EXAMPLE_ID))
        }

        private fun checkTitle(promoBlock: PromoBlock) {
            assertThat(promoBlock.title, equalTo(EXAMPLE_TITLE))
        }

        private fun checkDescription(promoBlock: PromoBlock) {
            assertThat(promoBlock.description, equalTo(EXAMPLE_DESCRIPTION))
        }

        private fun checkImage(promoBlock: PromoBlock) {
            assertThat(promoBlock.imageUrl, equalTo(EXAMPLE_IMAGE))
        }

        private fun checkPriority(promoBlock: PromoBlock) {
            assertThat(promoBlock.priority, equalTo(EXAMPLE_PRIORITY))
        }

        private fun checkBadge(promoBlock: PromoBlock) {
            assertThat(EXAMPLE_BADGE, equalTo(promoBlock.badge))
        }

        private fun checkSkip(button: PromoBlock.PromoButton?) {
            assertThat(button, present())
            if (button == null) {
                return
            }
            checkSkipId(button)
            checkSkipStyle(button)
            checkSkipCaption(button)
        }

        private fun checkSkipId(button: PromoBlock.PromoButton) {
            assertThat(button.id, equalTo(EXAMPLE_SKIP_ID))
        }

        private fun checkSkipStyle(button: PromoBlock.PromoButton) {
            assertThat(button.style, equalTo(EXAMPLE_SKIP_STYLE))
        }

        private fun checkSkipCaption(button: PromoBlock.PromoButton) {
            assertThat(button.caption, equalTo(EXAMPLE_SKIP_CAPTION))
        }

        private fun checkPrimary(button: PromoBlock.PromoButton?) {
            assertThat(button, present())
            if (button == null) {
                return
            }
            checkPrimaryId(button)
            checkPrimaryStyle(button)
            checkPrimaryCaption(button)
            checkPrimaryAction(button)
        }

        private fun checkPrimaryId(button: PromoBlock.PromoButton) {
            assertThat(button.id, equalTo(EXAMPLE_PRIMARY_ID))
        }

        private fun checkPrimaryStyle(button: PromoBlock.PromoButton) {
            assertThat(button.style, equalTo(EXAMPLE_PRIMARY_STYLE))
        }

        private fun checkPrimaryCaption(button: PromoBlock.PromoButton) {
            assertThat(button.caption, equalTo(EXAMPLE_PRIMARY_CAPTION))
        }

        private fun checkPrimaryAction(button: PromoBlock.PromoButton) {
            assertThat(button.action, equalTo(EXAMPLE_PRIMARY_ACTION))
        }

        private fun parseJsonData(json: String) : PromoResponse {
            return PromoParser.parseData(json.byteInputStream().source().buffer())
        }
    }

    @Test
    fun `example should be parsed`() {
        val response: PromoResponse = parseJsonData(EXAMPLE_ANSWER_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkBadge(block)
        checkImage(block)
        checkPriority(block)
        assertThat(block.showConditions.size(), equalTo(1))
        assertThat(block.showConditions[EXAMPLE_CONDITION_1_NAME]!!.toBoolean(), equalTo(EXAMPLE_CONDITION_1_VALUE))

        checkSkip(block.skipButton)
        checkPrimary(block.primaryButton)
    }

    @Test(expected = Exception::class)
    fun `session skips missing`() {
        parseJsonData(MISSING_SESSION_SKIPS_JSON)
    }

    @Test
    fun `image missing`() {
        val response: PromoResponse = parseJsonData(MISSING_IMAGE_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkPriority(block)

        assertThat(block.skipButton, absent())
        checkPrimary(block.primaryButton)
    }

    @Test(expected = Exception::class)
    fun `id missing`() {
        parseJsonData(MISSING_ID_JSON)
    }

    @Test(expected = Exception::class)
    fun `title missing`() {
        parseJsonData(MISSING_TITLE_JSON)
    }

    @Test(expected = Exception::class)
    fun `description missing`() {
        parseJsonData(MISSING_DESCRIPTION_JSON)
    }

    @Test(expected = Exception::class)
    fun `priority missing`() {
        parseJsonData(MISSING_PRIORITY_JSON)
    }

    @Test(expected = Exception::class)
    fun `badge missing`() {
        parseJsonData(MISSING_BADGE_JSON)
    }

    @Test
    fun `show conditions missing`() {
        val response: PromoResponse = parseJsonData(MISSING_SHOW_CONDITIONS_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkPriority(block)

        assertThat(block.skipButton, absent())
        checkPrimary(block.primaryButton)
    }

    @Test(expected = Exception::class)
    fun `buttons missing`() {
        parseJsonData(MISSING_BUTTONS_JSON)
    }

    @Test
    fun `one button valid`() {
        val response: PromoResponse = parseJsonData(ONE_BUTTON_VALID_STYLE_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkPriority(block)

        assertThat(block.skipButton, absent())
        checkPrimary(block.primaryButton)
    }

    @Test(expected = Exception::class)
    fun `one button invalid`() {
        parseJsonData(ONE_BUTTON_INVALID_STYLE_JSON)
    }

    @Test
    fun `two buttons valid`() {
        val response: PromoResponse = parseJsonData(TWO_BUTTONS_VALID_STYLE_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkPriority(block)

        checkSkip(block.skipButton)
        checkPrimary(block.primaryButton)
    }

    @Test(expected = Exception::class)
    fun `two buttons both skip`() {
        parseJsonData(TWO_BUTTONS_INVALID_BOTH_SKIP_JSON)
    }

    @Test(expected = Exception::class)
    fun `two buttons both primary`() {
        parseJsonData(TWO_BUTTONS_INVALID_BOTH_PRIMARY_JSON)
    }

    @Test
    fun `multiple conditions`() {
        val response: PromoResponse = parseJsonData(MULTIPLE_CONDITIONS_JSON)
        checkSessionSkips(response)

        val block: PromoBlock = checkOneBlock(response)
        checkId(block)
        checkTitle(block)
        checkDescription(block)
        checkPriority(block)

        assertThat(block.showConditions.size(), equalTo(3))
        assertThat(block.showConditions[EXAMPLE_CONDITION_1_NAME]!!.toBoolean(), equalTo(EXAMPLE_CONDITION_1_VALUE))
        assertThat(block.showConditions[EXAMPLE_CONDITION_2_NAME]!!.toLong(), equalTo(EXAMPLE_CONDITION_2_VALUE))
        assertThat(block.showConditions[EXAMPLE_CONDITION_3_NAME]!!.toFloat(), equalTo(EXAMPLE_CONDITION_3_VALUE))

        assertThat(block.skipButton, absent())
        checkPrimary(block.primaryButton)
    }

    @Test
    fun `real json should be parsed`() {
        parseJsonData(REAL_JSON_RESPONSE)
    }

    @Test
    fun `empty blocks json should be parsed`() {
        parseJsonData(EMPTY_BLOCKS_RESPONSE)
    }
}

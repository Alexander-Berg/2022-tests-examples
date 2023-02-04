package com.yandex.mobile.realty.ui.chat

import com.yandex.mobile.realty.domain.model.chat.ChatUser
import com.yandex.mobile.realty.ui.presenter.getDisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author rogovalex on 27/01/2021.
 */
class UserDisplayNameTest {

    @Test
    fun fullName() {
        val user = ChatUser("123", "alias", "fullName", null)
        assertEquals("fullName", user.getDisplayName(false))
    }

    @Test
    fun aliasName() {
        val user = ChatUser("123", "alias", null, null)
        assertEquals("alias", user.getDisplayName(false))
    }

    @Test
    fun idName() {
        val user = ChatUser("123", null, null, null)
        assertEquals("ID 123", user.getDisplayName(false))
    }

    @Test
    fun hiddenFullName() {
        val user = ChatUser("123", "alias", "fullName", null)
        assertEquals("ID 123", user.getDisplayName(true))
    }

    @Test
    fun hiddenAliasName() {
        val user = ChatUser("123", "alias", null, null)
        assertEquals("ID 123", user.getDisplayName(true))
    }

    @Test
    fun hiddenIdName() {
        val user = ChatUser("123", null, null, null)
        assertEquals("ID 123", user.getDisplayName(true))
    }
}

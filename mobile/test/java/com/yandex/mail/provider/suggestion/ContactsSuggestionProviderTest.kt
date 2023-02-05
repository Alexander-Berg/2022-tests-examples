package com.yandex.mail.provider.suggestion

import android.annotation.SuppressLint
import android.content.pm.ProviderInfo
import android.database.Cursor
import com.yandex.mail.asserts.CursorConditions.totalCount
import com.yandex.mail.entity.ContactInfo
import com.yandex.mail.model.ContactsModel
import com.yandex.mail.provider.SQLUtils
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.tools.Accounts
import com.yandex.mail.util.BaseIntegrationTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildContentProvider
import java.util.Random

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner::class)
class ContactsSuggestionProviderTest : BaseIntegrationTest() {
    private var CONTACT_FIRST_NAME = "First"
    private var CONTACT_LAST_NAME = "Last"

    private var provider: ContactsSuggestionProvider? = null

    @Before
    fun setup() {
        init(Accounts.testLoginData)

        val info = ProviderInfo()
        info.authority = AUTHORITY
        provider = buildContentProvider<ContactsSuggestionProvider>(ContactsSuggestionProvider::class.java)
            .create(info)
            .get()
    }

    private fun insertContactsHelper(count: Int): List<ContactInfo> {
        val random = Random()
        val contacts = (0 until count)
            .map { i ->
                ContactInfo(
                    cid = random.nextInt().toString(),
                    email = "$i@ya.ru",
                    first_name = "$CONTACT_FIRST_NAME$i",
                    last_name = "$CONTACT_LAST_NAME$i"
                )
            }

        abookModel.insertContacts(contacts.reversed())
        return contacts
    }

    private fun query(constraint: String, selection: String? = null, args: Array<String>? = null): Cursor {
        val uri = ContactsSuggestionProvider.constraint(
            ContactsSuggestionProvider.base(user.uid),
            constraint
        )
        return provider!!.query(
            uri,
            ContactsModel.CONTACTS_PROJECTION,
            selection, args, null
        )!!
    }

    @Test
    fun testTop() {
        val insertedContacts =
            insertContactsHelper(10 * ContactsSuggestionProvider.DEFAULT_ABOOK_TOP_LIMIT) // some big number
        query("").use { cursor ->
            Assertions.assertThat(cursor)
                .has(totalCount(ContactsSuggestionProvider.DEFAULT_ABOOK_TOP_LIMIT))
            while (cursor.moveToNext()) {
                val email =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsSuggestionProvider.ADDRESS))
                assertThat(email).isEqualTo(insertedContacts[cursor.position].email)
            }
        }
    }

    @Test
    fun testNotIn() {
        val insertedContacts =
            insertContactsHelper(10 * ContactsSuggestionProvider.DEFAULT_ABOOK_TOP_LIMIT) // some big number

        val exclusionList = insertedContacts.take(2).map { it.email }
        val exclusionArray = exclusionList.filterNotNull().toTypedArray()
        query("", SQLUtils.getNotInClause_N(ContactsSuggestionProvider.ADDRESS, exclusionList), exclusionArray).use { cursor ->
            while (cursor.moveToNext()) {
                val email =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsSuggestionProvider.ADDRESS))
                assertThat(email).isNotIn(exclusionList)
            }
        }

        var flagContainsInExclusion = false

        query("").use { cursor ->
            while (cursor.moveToNext()) {
                val email =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsSuggestionProvider.ADDRESS))
                if (exclusionList.contains(email)) {
                    flagContainsInExclusion = true
                }
            }
        }

        assertThat(flagContainsInExclusion).isTrue()
    }

    /*
        Tests that in case of nontrivial request with no response we query cache
     */
    @Test
    fun testQuery() {
        insertContactsHelper(10)
        query("4").use { cursor -> assertThat(cursor).has(totalCount(1)) }
    }

    @Test
    fun testQueryWithAlternatives() {
        val contactsCount = 10
        insertContactsHelper(contactsCount)
        val address = "tets@ya.ru"
        abookModel.insertContacts(
            listOf(
                ContactInfo(
                    cid = (contactsCount + 1).toString(),
                    email = address,
                    first_name = "александр",
                    last_name = "скворцов"
                )
            )
        )

        query("тет").use { cursor ->
            assertThat(cursor).has(totalCount(1))
            assertThat(cursor.moveToFirst()).isTrue()
            Assert.assertEquals("address", cursor.columnNames[2])
            Assert.assertEquals(address, cursor.getString(2))
        }
    }

    @Test
    fun testGenerateSqlWhere() {
        assertThat(ContactsSuggestionProvider.generateSqlWhere(listOf("a")))
            .isEqualTo(
                "(mimetype = \"vnd.android.cursor.item/email_v2\" AND (data1 LIKE ? ))" +
                    " OR (mimetype = \"vnd.android.cursor.item/name\" AND (data1 LIKE ? ))"
            )
        assertThat(ContactsSuggestionProvider.generateSqlWhere(listOf("a", "b")))
            .isEqualTo(
                "(mimetype = \"vnd.android.cursor.item/email_v2\" AND (data1 LIKE ? OR data1 LIKE ? ))" +
                    " OR (mimetype = \"vnd.android.cursor.item/name\" AND (data1 LIKE ? OR data1 LIKE ? ))"
            )
        assertThat(ContactsSuggestionProvider.generateSqlWhere(listOf())).isEqualTo(null)
    }

    @Test
    fun testGetArguments() {
        assertThat(ContactsSuggestionProvider.getArguments(listOf("a"))).isEqualTo(arrayOf("%a%", "%a%"))
        assertThat(ContactsSuggestionProvider.getArguments(listOf("a", "b"))).isEqualTo(arrayOf("%a%", "%b%", "%a%", "%b%"))
        assertThat(ContactsSuggestionProvider.getArguments(listOf())).isEqualTo(null)
    }

    @Test
    fun testWithAbook() {
        val contactsCount = 100
        insertContactsHelper(contactsCount)

        val resultIds: MutableList<Int> = ArrayList()
        val query = "1"
        for (i in (0 until contactsCount).sorted()) {
            if (i.toString().contains(query)) {
                resultIds.add(i)
            }
        }
        query(query).use { cursor ->
            assertThat(cursor).has(totalCount(resultIds.size))
            assertThat(cursor.moveToFirst()).isTrue()
            var i = 0
            while (!cursor.isLast) {
                val resultId = resultIds[i]
                Assert.assertEquals("$CONTACT_FIRST_NAME$resultId $CONTACT_LAST_NAME$resultId", cursor.getString(1))
                cursor.moveToNext()
                i++
            }
        }
    }

    companion object {
        private val AUTHORITY = "com.yandex.mail.provider.contacts"
    }
}

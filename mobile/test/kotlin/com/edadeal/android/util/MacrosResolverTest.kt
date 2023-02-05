package com.edadeal.android.util

import android.net.Uri
import org.junit.Test
import kotlin.test.assertEquals

class MacrosResolverTest {

    @Test
    fun `assert value in uri is resolved correctly`() {
        val url = Uri.Builder()
            .scheme("https")
            .authority("www.tns-counter.ru")
            .appendPath("V13a**{{TNS}}**click_ad")
            .appendQueryParameter("q", "{\"a\"=\"{{A}}\",\"b\"=\"{{B}}\"}")
            .build().toString()
        val resolver = MacrosResolver.build<Unit> {
            "{{A}}" resolve { "?some" }
            "{{B}}" resolve { "other" }
            "{{TNS}}" resolve { "dvtp:0:adid:1" }
        }

        val expected = Uri.Builder()
            .scheme("https")
            .authority("www.tns-counter.ru")
            .appendEncodedPath("V13a**dvtp:0:adid:1**click_ad")
            .appendQueryParameter("q", "{\"a\"=\"?some\",\"b\"=\"other\"}")
            .build().toString()
        assertEquals(expected, resolver.resolveInUrl(Unit, url))
    }

    @Test
    fun `assert value is resolved correctly`() {
        val json = "{\"v\":\"{{VALUE}}\",\"n\":0}"
        val resolver = MacrosResolver.build<Unit> { "{{VALUE}}" resolve { "some" } }

        assertEquals("{\"v\":\"some\",\"n\":0}", resolver.resolve(Unit, json))
    }
}

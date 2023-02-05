package com.edadeal.android.model

import android.net.Uri
import com.edadeal.android.model.macros.DynamicPlaceholderResolver
import com.edadeal.android.model.macros.PlaceHolder
import com.edadeal.android.model.macros.PlaceholderResolver
import com.edadeal.android.ui.common.navigation.intents.UrlPlaceholderResolver
import org.junit.Test
import kotlin.test.assertEquals

class PlaceholderResolverTest {

    @Test
    fun `assert value in uri is resolved correctly`() {
        val url = Uri.Builder()
            .scheme("https")
            .authority("www.tns-counter.ru")
            .appendPath("V13a**{{TNS}}**click_ad")
            .appendQueryParameter("q", "{\"a\"=\"{{A}}\",\"b\"=\"{{B}}\"}")
            .build()
        val resolver = DynamicPlaceholderResolver.Builder(Resolver)
            .withPlaceholder("{{A}}", "?some")
            .withPlaceholder("{{B}}", "other")
            .withPlaceholder("{{TNS}}", "dvtp:0:adid:1")
            .build()
        val urlResolver = UrlPlaceholderResolver(resolver)

        val expected = Uri.Builder()
            .scheme("https")
            .authority("www.tns-counter.ru")
            .appendEncodedPath("V13a**dvtp:0:adid:1**click_ad")
            .appendQueryParameter("q", "{\"a\"=\"?some\",\"b\"=\"other\"}")
            .build()
        assertEquals(expected, urlResolver.resolveInUrl(url))
    }

    @Suppress("Detekt.NotImplementedDeclaration")
    private object Resolver : PlaceholderResolver {

        override fun getPlaceholderValue(placeholderAlias: String): String = placeholderAlias

        override fun getPlaceholderValue(placeholder: PlaceHolder): String {
            return getPlaceholderValue(placeholder.alias)
        }

        override fun replacePlaceholders(input: String): String = input

        override fun replacePlaceholders(
            input: String,
            transformValue: (placeholderAlias: String, placeholderValue: String) -> String
        ): String {
            return replacePlaceholders(input)
        }

        override fun getPlaceholderAliases(): Collection<String> = PlaceHolder.values().map { it.alias }

        override fun prepareUrl(inputUrl: String): String = throw NotImplementedError()
        override fun getDefaultArgsKeyValueMap(): Map<String, String> = throw NotImplementedError()
        override fun getFeedbackUrlDefaultArgs(): Map<String, String> = throw NotImplementedError()
    }
}

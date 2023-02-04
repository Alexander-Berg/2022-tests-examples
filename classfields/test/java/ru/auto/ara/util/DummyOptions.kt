package ru.auto.ara.util

import org.mockito.kotlin.whenever
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.util.android.OptionsProvider

/**
 * Some options for tests
 * Created by airfreshener on 19.01.2018.
 */
class DummyOptions {

    companion object {
        fun applyDummyOptions(optionsProvider: OptionsProvider<Option>){
            val dummyOptions: MutableMap<String, List<Option>> = mutableMapOf()
            dummyOptions[Filters.PERIOD_FIELD] = listOf(
                    Option("", "Любой срок")
            )

            dummyOptions[Filters.BEATEN_FIELD] = listOf(
                    Option("1", "Неважно"),
                    Option("2", "Кроме битых"),
                    Option("3", "Битые")
            )

            dummyOptions[Filters.CUSTOM_KEY_FIELD] = listOf(
                    Option("", "Неважно"),
                    Option("1", "Не растаможен"),
                    Option("2", "Растаможен")
            )

            dummyOptions[Filters.WHEEL_FIELD] = listOf(
                    Option("1", "Любой руль"),
                    Option("2", "Левый"),
                    Option("3", "Правый")
            )

            dummyOptions.forEach { f, s ->
                whenever(optionsProvider.get(f))
                        .thenReturn(s)
            }

        }
    }
}

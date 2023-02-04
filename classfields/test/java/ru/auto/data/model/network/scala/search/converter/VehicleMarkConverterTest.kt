package ru.auto.data.model.network.scala.search.converter

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.data.model.network.scala.search.NWEntryView
import ru.auto.data.model.network.scala.search.NWGenView
import ru.auto.data.model.network.scala.search.NWMarkModelNameplateView
import ru.auto.data.model.search.Generation
import ru.auto.data.model.search.Mark
import ru.auto.data.model.search.Model
import ru.auto.data.model.search.Nameplate
import kotlin.test.assertEquals

/**
 *
 * @author jagger on 14.08.18.
 */
@RunWith(AllureRunner::class) class VehicleMarkConverterTest {


    @Test
    fun `convert views to mark`() {

        val view1 = NWMarkModelNameplateView(
                mark = NWEntryView(code = "volvo", name = "Volvo"),
                model = NWEntryView(code = "xc90", name = "XC90"),
                nameplate = NWEntryView(code = "1", name = "I")
        )

        val view2 = NWMarkModelNameplateView(
                mark = NWEntryView(code = "volvo", name = "Volvo"),
                model = NWEntryView(code = "xc60", name = "XC60"),
                nameplate = NWEntryView(code = "2", name = "II")
        )

        val view3 = NWMarkModelNameplateView(
                mark = NWEntryView(code = "volvo", name = "Volvo"),
                model = NWEntryView(code = "xc60", name = "XC60"),
                nameplate = NWEntryView(code = "3", name = "III")
        )

        val expected = Mark(
                id = "volvo",
                name = "Volvo",
                models = listOf(
                        Model(
                                id = "xc90",
                                name = "XC90",
                                nameplates = listOf(
                                        Nameplate(
                                                id = "1",
                                                name = "I"
                                        )
                                ),
                                generations = emptyList()
                        ),
                        Model(
                                id = "xc60",
                                name = "XC60",
                                nameplates = listOf(
                                        Nameplate(
                                                id = "2",
                                                name = "II"
                                        ),
                                        Nameplate(
                                                id = "3",
                                                name = "III"
                                        )
                                ),
                                generations = emptyList()
                        )
                )
        )

        val actual = VehicleMarkConverter.fromNetwork(listOf(view1, view2, view3)).first()
        assertEquals(expected, actual)
    }

    @Test
    fun `convert views to mark with nameplate`() {

        val view1 = NWMarkModelNameplateView(
            mark = NWEntryView(code = "volvo", name = "Volvo"),
            model = NWEntryView(code = "xc90", name = "XC90"),
            super_gen = NWGenView(id = "2", name = "II")
        )

        val view2 = NWMarkModelNameplateView(
            mark = NWEntryView(code = "volvo", name = "Volvo"),
            model = NWEntryView(code = "xc90", name = "XC90"),
            nameplate = NWEntryView(code = "1", name = "I"),
            super_gen = NWGenView(id = "3", name = "III")
        )

        val expected = Mark(
            id = "volvo",
            name = "Volvo",
            models = listOf(
                Model(
                    id = "xc90",
                    name = "XC90",
                    nameplates = listOf(
                        Nameplate(
                            id = "1",
                            name = "I"
                        )
                    ),
                    generations = listOf(
                        Generation(
                            id = "3",
                            name = "III"
                        )
                    )
                ),
                Model(
                    id = "xc90",
                    name = "XC90",
                    nameplates = emptyList(),
                    generations = listOf(
                        Generation(
                            id = "2",
                            name = "II"
                        )
                    )
                )
            )
        )

        val actual = VehicleMarkConverter.fromNetwork(listOf(view1, view2)).first()
        assertEquals(expected, actual)
    }
}

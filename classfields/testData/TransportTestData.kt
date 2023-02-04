package ru.auto.ara.core.testdata

import ru.auto.ara.R
import ru.auto.ara.core.testdata.Preset.CommCategory
import ru.auto.ara.core.testdata.Preset.MotoCategory

sealed class Preset(val label: String, val imageRes: Int? = null) {
    class MotoCategory(val name: String, label: String, imageRes: Int) : Preset(label, imageRes)
    class CommCategory(val name: String, label: String, imageRes: Int) : Preset(label, imageRes)
}

val MOTO_CATEGORY_PRESETS = listOf(
    MotoCategory(name = "MOTORCYCLE", label = "Мотоциклы", imageRes = R.drawable.moto_1_bike),
    MotoCategory(name = "SCOOTERS", label = "Скутеры", imageRes = R.drawable.moto_2_scooter),
    MotoCategory(name = "ATV", label = "Мотовездеходы", imageRes = R.drawable.moto_3_atv),
    MotoCategory(name = "SNOWMOBILE", label = "Снегоходы", imageRes = R.drawable.moto_4_snowmobile)
)

val COMM_CATEGORY_PRESETS = listOf(
    CommCategory(name = "LCV", label = "Лёгкий коммерческий", imageRes = R.drawable.com_1_light),
    CommCategory(name = "TRUCK", label = "Грузовики", imageRes = R.drawable.com_2_truck),
    CommCategory(name = "ARTIC", label = "Седельные тягачи", imageRes = R.drawable.com_3_truck_tractor),
    CommCategory(name = "BUS", label = "Автобусы", imageRes = R.drawable.com_4_bus),
    CommCategory(name = "TRAILER", label = "Прицепы и кузова", imageRes = R.drawable.com_5_trailer),
    CommCategory(name = "AGRICULTURAL", label = "Сельхоз", imageRes = R.drawable.com_6_agricultural),
    CommCategory(name = "CONSTRUCTION", label = "Строительная", imageRes = R.drawable.com_7_building),
    CommCategory(name = "AUTOLOADER", label = "Погрузчики", imageRes = R.drawable.com_8_forklift_truck),
    CommCategory(name = "CRANE", label = "Автокраны", imageRes = R.drawable.com_9_autocrane),
    CommCategory(name = "DREDGE", label = "Экскаваторы", imageRes = R.drawable.com_10_excavator),
    CommCategory(name = "BULLDOZERS", label = "Бульдозеры", imageRes = R.drawable.com_11_bulldozer),
    CommCategory(name = "MUNICIPAL", label = "Коммунальная", imageRes = R.drawable.com_13_municipal_vehicles)
)

package ru.auto.ara.core.testdata

val STATUS_PARAMS: List<Pair<String, String>> = listOf(
    "Активные" to "ACTIVE",
    "Неактивные" to "INACTIVE",
    "Ждут активации" to "NEED_ACTIVATION",
    "Заблокированные" to "BANNED"
)

val APPLIED_SERVICE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Премиум", "service", "all_sale_premium"),
    arrayOf("Поднятые в поиске", "service", "all_sale_fresh"),
    arrayOf("С автоподнятием", "tag", "service_auto_apply"),
    arrayOf("Турбо-продажа", "service", "package_turbo"),
    arrayOf("Спецпредложение", "service", "all_sale_special"),
    arrayOf("Со стикерами", "service", "all_sale_badge"),
    arrayOf("С автостратегией", "tag", "autostrategy_always_at_first_page")
)

class VinCheckParam(
    val name: String,
    val paramName: String,
    val values: Set<String>,
    val description: String
)

val VIN_CHECK_PARAMS: Array<VinCheckParam> = arrayOf(
    VinCheckParam("Проверено", "tag", setOf("vin_resolution_ok"), "green reports"),
    VinCheckParam("Серые отчёты", "tag", setOf("vin_resolution_unknown", "vin_resolution_untrusted"), "grey reports"),
    VinCheckParam("Красные отчёты", "tag", setOf("vin_resolution_error", "vin_resolution_invalid"), "red reports"),
    VinCheckParam(
        "Без отчёта",
        "exclude_tag",
        setOf(
            "vin_resolution_ok",
            "vin_resolution_untrusted",
            "vin_resolution_unknown",
            "vin_resolution_error",
            "vin_resolution_invalid"
        ),
        "empty reports"
    )
)

val SORTING_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Возрастанию цены", "price-ASC"),
    arrayOf("Убыванию цены", "price-DESC"),
    arrayOf("Марка, модель", "alphabet-ASC"),
    arrayOf("Дате размещения: новее", "cr_date-DESC"),
    arrayOf("Дате размещения: старше", "cr_date-ASC"),
    arrayOf("Году: новее", "year-DESC"),
    arrayOf("Году: старше", "year-ASC")
)

val DEALER_MOTO_SUBCATEGORIES_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Мотоциклы", "motorcycle"),
    arrayOf("Скутеры", "scooters"),
    arrayOf("Мотовездеходы", "atv"),
    arrayOf("Снегоходы", "snowmobile")
)

val DEALER_TRUCK_SUBCATEGORIES_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Лёгкие коммерческие", "lcv"),
    arrayOf("Грузовики", "truck"),
    arrayOf("Седельные тягачи", "artic"),
    arrayOf("Автобусы", "bus"),
    arrayOf("Прицепы и съёмные кузова", "trailer"),
    arrayOf("Сельскохозяйственная", "agricultural"),
    arrayOf("Автопогрузчики", "autoloader"),
    arrayOf("Строительная", "construction"),
    arrayOf("Экскаваторы", "dredge"),
    arrayOf("Бульдозеры", "bulldozers"),
    arrayOf("Автокраны", "crane"),
    arrayOf("Коммунальная", "municipal")
)

val DEALER_FEED_SORTING_PARAMS: List<Pair<String, String>> = listOf(
    Pair("Дате размещения", "cr_date-desc"),
    Pair("Возрастанию цены", "price-asc"),
    Pair("Убыванию цены", "price-desc"),
    Pair("Году: новее", "year-desc"),
    Pair("Году: старше", "year-asc"),
    Pair("Пробегу", "km_age-asc"),
    Pair("По названию", "alphabet-asc"),
    Pair("Уникальности", "autoru_exclusive-desc"),
    Pair("Оценке стоимости", "price_profitability-desc")
)

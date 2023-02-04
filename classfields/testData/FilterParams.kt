package ru.auto.ara.core.testdata

import ru.auto.ara.R
import ru.auto.ara.consts.Filters.ONLINE_VIEW_AVAILABLE_TAG
import ru.auto.ara.consts.Filters.PANORAMAS_TAG
import ru.auto.ara.consts.Filters.SEARCH_TAG
import ru.auto.ara.consts.Filters.VIDEO_TAG
import ru.auto.data.util.ALLOWED_FOR_CREDIT_TAG

val STEERING_WHEEL_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Любой руль", "", ""),
    arrayOf("Левый", "Левый", "LEFT"),
    arrayOf("Правый", "Правый", "RIGHT")
)

val OWNERS_COUNT_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Неважно", "", ""),
    arrayOf("Один", "Один", "ONE"),
    arrayOf("Не более двух", "Не более двух", "LESS_THAN_TWO")
)

val OWNING_TIME_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Неважно", "", ""),
    arrayOf("До 1 года", "До 1 года", "LESS_THAN_YEAR"),
    arrayOf("От 1 до 3 лет", "От 1 до 3 лет", "FROM_1_TO_3"),
    arrayOf("От 3 лет и более", "От 3 лет и более", "MORE_THAN_3")
)

class BodyTypeParam(
    val name: String,
    val param: String,
    val drawableId: Int
)

val BODY_TYPE_PARAMS: Array<BodyTypeParam> = arrayOf(
    BodyTypeParam("Седан", "SEDAN", R.drawable.body_sedan),
    BodyTypeParam("Хэтчбек 3 дв.", "HATCHBACK_3_DOORS", R.drawable.body_hatch_3d),
    BodyTypeParam("Хэтчбек 5 дв.", "HATCHBACK_5_DOORS", R.drawable.body_hatch_5d),
    BodyTypeParam("Лифтбек", "LIFTBACK", R.drawable.body_liftback),
    BodyTypeParam("Внедорожник 3 дв.", "ALLROAD_3_DOORS", R.drawable.body_allroad_3d),
    BodyTypeParam("Внедорожник 5 дв.", "ALLROAD_5_DOORS", R.drawable.body_allroad_5d),
    BodyTypeParam("Универсал", "WAGON", R.drawable.body_universal),
    BodyTypeParam("Купе", "COUPE", R.drawable.body_coupe),
    BodyTypeParam("Минивэн", "MINIVAN", R.drawable.body_minivan),
    BodyTypeParam("Пикап", "PICKUP", R.drawable.body_pickup),
    BodyTypeParam("Лимузин", "LIMOUSINE", R.drawable.body_limo),
    BodyTypeParam("Фургон", "VAN", R.drawable.body_furgon),
    BodyTypeParam("Кабриолет", "CABRIO", R.drawable.body_cabrio)
)

val ENGINE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Бензин", "GASOLINE"),
    arrayOf("Дизель", "DIESEL"),
    arrayOf("Гибрид", "HYBRID"),
    arrayOf("Электро", "ELECTRO"),
    arrayOf("Турбированный", "TURBO"),
    arrayOf("Атмосферный", "ATMO"),
    arrayOf("Газобаллонное оборудование", "LPG")
)


val GEAR_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Передний", "FORWARD_CONTROL"),
    arrayOf("Задний", "REAR_DRIVE"),
    arrayOf("Полный", "ALL_WHEEL_DRIVE")
)

val COLOR_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Черный", "040001"),
    arrayOf("Серый", "97948F"),
    arrayOf("Серебряный", "CACECB"),
    arrayOf("Белый", "FAFBFB"),
    arrayOf("Красный", "EE1D19"),
    arrayOf("Розовый", "FFC0CB"),
    arrayOf("Оранжевый", "FF8649"),
    arrayOf("Золотой", "DEA522"),
    arrayOf("Желтый", "FFD600"),
    arrayOf("Зеленый", "007F00"),
    arrayOf("Голубой", "22A0F8"),
    arrayOf("Синий", "0000CC"),
    arrayOf("Фиолетовый", "4A2197"),
    arrayOf("Пурпурный", "660099"),
    arrayOf("Коричневый", "200204"),
    arrayOf("Бежевый", "C49648")
)

val CERTIFICATIONS_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Проверенные по VIN", "vin_checked"),
    arrayOf("Проверенные производителем", "certificate_manufacturer")
)

class TransmissionParam(
    val nameInBottomSheet: String,
    val param: Set<String>,
    val nameInFilters: String,
    val description: String

)

val TRANSMISSION_PARAMS: Array<TransmissionParam> = arrayOf(
    TransmissionParam(
        "Автомат",
        setOf("ROBOT", "AUTOMATIC", "VARIATOR"),
        "Автомат, Автоматическая, Робот, Вариатор",
        "AUTO"
    ),
    TransmissionParam("Автоматическая", setOf("AUTOMATIC"), "Автоматическая", "AUTOMATIC"),
    TransmissionParam("Робот", setOf("ROBOT"), "Робот", "ROBOT"),
    TransmissionParam("Вариатор", setOf("VARIATOR"), "Вариатор", "VARIATOR"),
    TransmissionParam("Механика", setOf("MECHANICAL"), "Механика", "MECHANICAL")
)

val DAMAGE_GROUP_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Неважно", "ANY"),
    arrayOf("Кроме битых", "NOT_BEATEN"),
    arrayOf("Битые", "BEATEN")
)


val CUSTOMS_STATE_GROUP_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Неважно", "DOESNT_MATTER"),
    arrayOf("Растаможен", "CLEARED"),
    arrayOf("Не растаможен", "NOT_CLEARED")
)

class CheckBoxParam(
    val fieldScrollTo: String,
    val fieldClickTo: String,
    val param: String,
    val checkedParamValue: String,
    val uncheckedParamValue: String
)

val CHECKED_CHECKBOXES_WITH_NOT_EMPTY_PARAM: Array<CheckBoxParam> = arrayOf(
    CheckBoxParam("Срок размещения", "Только с фото", "has_image", "true", "false"),
)

val UNCHECKED_CHECKBOXES_WITH_NOT_EMPTY_PARAM: Array<CheckBoxParam> = arrayOf(
    CheckBoxParam("Срок размещения", "Без доставки", "with_delivery", "NONE", "BOTH"),
    CheckBoxParam("Срок размещения", "Только с НДС", "only_nds", "true", "false"),
)

val UNCHECKED_CHECKBOXES_WITH_EMPTY_PARAM: Array<CheckBoxParam> = arrayOf(
    CheckBoxParam("Срок размещения", "Только с видео", SEARCH_TAG, VIDEO_TAG, ""),
    CheckBoxParam("Срок размещения", "Только с панорамой", SEARCH_TAG, PANORAMAS_TAG, ""),
    CheckBoxParam("Срок размещения", "Доступны в\u00A0кредит", SEARCH_TAG, ALLOWED_FOR_CREDIT_TAG, ""),
    CheckBoxParam("Срок размещения", "Онлайн-показ", SEARCH_TAG, ONLINE_VIEW_AVAILABLE_TAG, ""),
    CheckBoxParam("Срок размещения", "На гарантии", "with_warranty", "true", ""),
    CheckBoxParam("Срок размещения", "Возможен обмен", "exchange_group", "POSSIBLE", ""),
    CheckBoxParam("Только с фото", "Оригинал ПТС", "pts_status", "1", "")
)

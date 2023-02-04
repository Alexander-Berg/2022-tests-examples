package ru.auto.ara.core.testdata

val MOTO_SUBCATEGORIES_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Мотоциклы", "MOTORCYCLE"),
    arrayOf("Скутеры", "SCOOTERS"),
    arrayOf("Мотовездеходы", "ATV"),
    arrayOf("Снегоходы", "SNOWMOBILE")
)

val MOTORCYCLE_TRANSMISSION_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("1 передача", "TRANSMISSION_1"),
    arrayOf("2 передачи", "TRANSMISSION_2"),
    arrayOf("3 передачи", "TRANSMISSION_3"),
    arrayOf("4 передачи", "TRANSMISSION_4"),
    arrayOf("5 передач", "TRANSMISSION_5"),
    arrayOf("6 передач", "TRANSMISSION_6"),
    arrayOf("7 передач", "TRANSMISSION_7"),
    arrayOf("8 передач", "TRANSMISSION_8"),
    arrayOf("2-скоростной автомат", "AUTOMATIC_2_SPEED"),
    arrayOf("3-скоростной автомат", "AUTOMATIC_3_SPEED"),
    arrayOf("4 прямых и задняя", "TRANSMISSION_4_FORWARD_AND_BACK"),
    arrayOf("5 прямых и задняя", "TRANSMISSION_5_FORWARD_AND_BACK"),
    arrayOf("6 прямых и задняя", "TRANSMISSION_6_FORWARD_AND_BACK"),
    arrayOf("АКПП", "AUTOMATIC"),
    arrayOf("Роботизированная", "ROBOTIC"),
    arrayOf("Роботизированная с двумя сцеплениями", "ROBOTIC_2_CLUTCH"),
    arrayOf("Вариатор", "VARIATOR")
)

val MOTORCYCLE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Allround", "ALLROUND"),
    arrayOf("Внедорожный Эндуро", "OFFROAD_ENDURO"),
    arrayOf("Мотоцикл повышенной проходимости", "CROSS_COUNTRY"),
    arrayOf("Спортивный Эндуро", "SPORTENDURO"),
    arrayOf("Туристический Эндуро", "TOURIST_ENDURO"),
    arrayOf("Naked bike", "NAKEDBIKE"),
    arrayOf("Дорожный", "ROAD"),
    arrayOf("Классик", "CLASSIC"),
    arrayOf("Кастом", "CUSTOM"),
    arrayOf("Круизер", "CRUISER"),
    arrayOf("Чоппер", "CHOPPER"),
    arrayOf("Кросс", "CROSS"),
    arrayOf("Speedway", "SPEEDWAY"),
    arrayOf("Детский", "CHILDISH"),
    arrayOf("Минибайк", "MINIBIKE"),
    arrayOf("Питбайк", "PITBIKE"),
    arrayOf("Триал", "TRIAL"),
    arrayOf("Спорт-байк", "SPORTBIKE"),
    arrayOf("Спорт-туризм", "SPORTTOURISM"),
    arrayOf("Супер-спорт", "SUPERSPORT"),
    arrayOf("Супермото", "SUPERMOTARD"),
    arrayOf("Трайк", "TRIKE"),
    arrayOf("Трицикл", "TRICYCLE"),
    arrayOf("Туристический", "TOURISM")
)

val MOTORCYCLE_ENGINE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Дизель", "DIESEL"),
    arrayOf("Инжектор", "INJECTOR"),
    arrayOf("Карбюратор", "CARBURETOR"),
    arrayOf("Бензин турбонаддув", "TURBO")
)

val MOTORCYCLE_DRIVE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Кардан", "CARDAN"),
    arrayOf("Ремень", "BELT"),
    arrayOf("Цепь", "CHAIN")
)

val MOTORCYCLE_CYLINDERS_COUNT_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("1", "CYLINDERS_1"),
    arrayOf("2", "CYLINDERS_2"),
    arrayOf("3", "CYLINDERS_3"),
    arrayOf("4", "CYLINDERS_4"),
    arrayOf("6", "CYLINDERS_6"),
    arrayOf("8", "CYLINDERS_8"),
    arrayOf("10", "CYLINDERS_10")
)

val MOTORCYCLE_CYLINDERS_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("V - образное", "V_TYPE"),
    arrayOf("Оппозитное", "OPPOSITE"),
    arrayOf("Роторное", "ROTARY"),
    arrayOf("Рядное", "LINE")
)

val SCOOTERS_ENGINE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Инжектор", "GASOLINE_INJECTOR"),
    arrayOf("Карбюратор", "GASOLINE_CARBURETOR"),
    arrayOf("Ротор", "GASOLINE_ROTOR"),
    arrayOf("Бензин турбонаддув", "GASOLINE_TURBO"),
    arrayOf("Электрический", "ELECTRO")
)

val ATV_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Детский", "CHILDISH"),
    arrayOf("Спортивный", "SPORTS"),
    arrayOf("Туристический", "TOURIST"),
    arrayOf("Утилитарный", "UTILITARIAN"),
    arrayOf("Амфибия", "AMPHIBIAN"),
    arrayOf("Багги", "BUGGI")
)

val ATV_TRANSMISSION_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Автомат", "AUTOMATIC"),
    arrayOf("Вариатор", "VARIATOR"),
    arrayOf("Механика", "MECHANICAL"),
    arrayOf("Полуавтомат", "SEMI_AUTOMATIC"),
    arrayOf("Прямая передача", "DIRECT_DRIVE")
)

val ATV_ENGINE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Инжектор", "GASOLINE_INJECTOR"),
    arrayOf("Карбюратор", "GASOLINE_CARBURETOR")
)

val ATV_DRIVE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Задний", "BACK"),
    arrayOf("Задний с дифференциалом", "BACK_DIFFERENTIAL"),
    arrayOf("Полный", "FULL")
)

val ATV_CYLINDERS_COUNT_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("1", "CYLINDERS_1"),
    arrayOf("2", "CYLINDERS_2"),
    arrayOf("3", "CYLINDERS_3"),
    arrayOf("4", "CYLINDERS_4"),
    arrayOf("5", "CYLINDERS_5"),
    arrayOf("6", "CYLINDERS_6")
)

val ATV_CYLINDERS_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("V - образное", "V_TYPE"),
    arrayOf("Оппозитное", "OPPOSITE"),
    arrayOf("Рядное", "LINE")
)

val SNOWMOBILE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Детский", "CHILDISH"),
    arrayOf("Кроссовер", "CROSSOVER"),
    arrayOf("Спортивный горный", "SPORTS_MOUNTAIN"),
    arrayOf("Спортивный кроссовый", "SPORTS_CROSS"),
    arrayOf("Туристический", "TOURIST"),
    arrayOf("Утилитарный", "UTILITARIAN")
)

val SNOWMOBILE_ENGINE_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Дизель", "DIESEL"),
    arrayOf("Инжектор", "GASOLINE_INJECTOR"),
    arrayOf("Карбюратор", "GASOLINE_CARBURETOR"),
    arrayOf("Турбо", "GASOLINE_TURBO")
)

val SNOWMOBILE_CYLINDERS_COUNT_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("1", "CYLINDERS_1"),
    arrayOf("2", "CYLINDERS_2"),
    arrayOf("3", "CYLINDERS_3"),
    arrayOf("4", "CYLINDERS_4"),
    arrayOf("5", "CYLINDERS_5"),
    arrayOf("6", "CYLINDERS_6")
)

val SNOWMOBILE_CYLINDERS_TYPE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("V-образное", "V_TYPE"),
    arrayOf("Оппозитное", "OPPOSITE"),
    arrayOf("Рядное", "LINE")
)

val MOTORCYCLE_STROKE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Любое", "", ""),
    arrayOf("2", "2", "STROKES_2"),
    arrayOf("4", "4", "STROKES_4")
)

val SCOOTERS_STROKE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Любое", "", ""),
    arrayOf("2", "2", "STROKES_2"),
    arrayOf("4", "4", "STROKES_4")
)

val ATV_STROKE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Любое", "", ""),
    arrayOf("2-тактный", "2-тактный", "STROKES_2"),
    arrayOf("4-тактный", "4-тактный", "STROKES_4")
)

val SNOWMOBILE_STROKE_PARAMS: Array<Array<String>> = arrayOf(
    arrayOf("Любое", "", ""),
    arrayOf("2-тактный", "2-тактный", "STROKES_2"),
    arrayOf("4-тактный", "4-тактный", "STROKES_4")
)

package ru.auto.ara.filter.screen.user

import ru.auto.data.model.catalog.Subcategory
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.MOTO
import ru.auto.data.model.data.offer.TRUCKS
import ru.auto.data.util.BUS_SUBCATEGORY_ID
import ru.auto.data.util.LIGHT_COMMERCIAL_SUBCATEGORY_ID
import ru.auto.data.util.MOTO_SUBCATEGORY_ID
import ru.auto.data.util.SCOOTERS_SUBCATEGORY_ID
import ru.auto.data.util.SNOWMOBILES_SUBCATEGORY_ID
import ru.auto.data.util.TRACTOR_SUBCATEGORY_ID
import ru.auto.data.util.TRAILER_SUBCATEGORY_ID
import ru.auto.data.util.TRUCK_SUBCATEGORY_ID

object CampaignFactory {

    fun buildAll() = listOf(CampaignBuilder().build(), CampaignBuilder(MOTO).newUsedStates().build())

    fun buildCarUsed(uppercase: Boolean = false) =
        listOf(CampaignBuilder(CAR.apply { if (uppercase) toUpperCase() }).build())

    fun buildCarMoto() = listOf(CampaignBuilder().build(), CampaignBuilder(MOTO).newUsedStates().build())

    fun buildMotoComm() = listOf(CampaignBuilder(TRUCKS).build(), CampaignBuilder(MOTO).newUsedStates().build())

    fun buildCarAll() = listOf(CampaignBuilder().newUsedStates().build())

    fun buildCarMotoComm() = listOf(
        CampaignBuilder(CAR).newUsedStates().build(),
        CampaignBuilder(MOTO).newUsedStates().build(),
        CampaignBuilder(TRUCKS).build()
    )

    fun getMotoSubcategories() = listOf(
        Subcategory("id", "Мотоциклы", MOTO_SUBCATEGORY_ID),
        Subcategory("id", "Скутеры", SCOOTERS_SUBCATEGORY_ID),
        Subcategory("id", "Снегоходы", SNOWMOBILES_SUBCATEGORY_ID)
    )

    fun getTruckSubcategories() = listOf(
        Subcategory("id", "Лёгкие коммерческие", LIGHT_COMMERCIAL_SUBCATEGORY_ID),
        Subcategory("id", "Грузовики", TRUCK_SUBCATEGORY_ID),
        Subcategory("id", "Седельные тягачи", TRACTOR_SUBCATEGORY_ID),
        Subcategory("id", "Автобусы", BUS_SUBCATEGORY_ID),
        Subcategory("id", "Прицепы и съёмные кузова", TRAILER_SUBCATEGORY_ID)
    )

}

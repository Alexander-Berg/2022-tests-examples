package ru.auto.ara.core.testdata

import ru.auto.data.model.VehicleCategory

object SearchFeedDefaultParams {

    fun stringParams(category: VehicleCategory): List<Pair<String, String>> {
        when (category) {
            VehicleCategory.CARS -> {
                return listOf(
                    "customs_state_group" to "CLEARED",
                    "damage_group" to "NOT_BEATEN",
                    "has_image" to "true",
                    "in_stock" to "ANY_STOCK",
                    "only_official" to "false",
                    "state_group" to "ALL",
                    "with_delivery" to "BOTH",
                    "with_discount" to "true"
                )
            }
            VehicleCategory.TRUCKS -> {
                return listOf(
                    "customs_state_group" to "DOESNT_MATTER",
                    "damage_group" to "NOT_BEATEN",
                    "has_image" to "true",
                    "in_stock" to "ANY_STOCK",
                    "state_group" to "ALL",
                    "with_delivery" to "BOTH",
                    "trucks_params.trucks_category" to "LCV"
                )
            }
            VehicleCategory.MOTO -> {
                return listOf(
                    "customs_state_group" to "DOESNT_MATTER",
                    "damage_group" to "NOT_BEATEN",
                    "has_image" to "true",
                    "in_stock" to "ANY_STOCK",
                    "state_group" to "ALL",
                    "moto_params.moto_category" to "MOTORCYCLE"
                )
            }
        }
    }

    fun arrayParams(category: VehicleCategory): List<Pair<String, Set<String>>> = listOf("seller_group" to setOf("ANY_SELLER"))
}

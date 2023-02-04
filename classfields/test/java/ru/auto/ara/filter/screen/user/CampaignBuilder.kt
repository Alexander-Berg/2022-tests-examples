package ru.auto.ara.filter.screen.user

import ru.auto.data.model.Campaign
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.MOTO
import ru.auto.data.model.data.offer.NEW
import ru.auto.data.model.data.offer.TRUCKS
import ru.auto.data.model.data.offer.USED

class CampaignBuilder(private val category: String = CAR) {

    private var section: List<String> = listOf(USED)
    private var motoSub = if (category == MOTO) listOf("motorcycle", "scooters", "atv") else emptyList()
    private var truckSub = if (category == TRUCKS) listOf("lcv", "truck", "artic", "bus") else emptyList()

    fun newUsedStates() = apply { this.section = listOf(USED, NEW) }

    fun build(): Campaign {
        return Campaign(
            category,
            section,
            motoSub,
            truckSub
        )
    }
}

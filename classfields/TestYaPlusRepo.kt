package ru.auto.ara.core.rules.di

import ru.auto.data.repository.IYaPlusRepository
import ru.auto.data.repository.YaPlusPointsConfig

class TestYaPlusRepo : IYaPlusRepository {

    override fun markBubbleAsShown() = Unit

    override fun isBubbleShown(): Boolean = isBubbleShown

    override fun getYaPlusPointsConfig(): YaPlusPointsConfig = yaPlusPointsConfig

    companion object {
        @Volatile
        var isBubbleShown: Boolean = false

        @Volatile
        var yaPlusPointsConfig: YaPlusPointsConfig = YaPlusPointsConfig.Unavailable
    }

}

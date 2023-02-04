package ru.auto.ara.core.utils

import ru.auto.util.IRandom
import java.util.UUID

class TestRandom : IRandom {

    var currentBooleanValue = false
    var currentDouble: Double = 0.0
    var uuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    override fun nextBoolean(): Boolean = currentBooleanValue
    override fun nextDouble(): Double = currentDouble
    override fun nextUuid(): UUID = uuid

}

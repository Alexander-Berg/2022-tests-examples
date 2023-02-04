package ru.auto.ara.filter.screen.auto

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.search.LimitValue
import ru.auto.ara.util.SerializablePair
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author aleien on 12.05.17.
 */
@RunWith(AllureRobolectricRunner::class) class AutoExtrasSectionTest : AutoFilterTest() {

    @Test
    fun `engine field should convert to param as engine`() {
        val engine = "1260"
        testedScreen.getValueFieldById<Set<String>>(Filters.ENGINE_TYPE_FIELD).value = setOf(engine)
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.ENGINE_TYPE_FIELD, engine))
    }

    @Test
    fun `turbo field should convert to param as engine type value`() {
        val turbo = "turbo"
        testedScreen.getValueFieldById<Set<String>>(Filters.ENGINE_TYPE_FIELD).value = setOf(turbo)
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.ENGINE_TYPE_FIELD, turbo))
        assertThat(params).doesNotContain(SerializablePair(Filters.FEEDING_TYPE_FIELD, turbo))
        assertThat(params).doesNotContain(SerializablePair(Filters.TURBO_FIELD, turbo))
    }

    @Test
    fun `volume field should convert to param as volume_from and volume_to with fraction`() {
        val volume = SerializablePair(0.2, 10.0)
        testedScreen.getValueFieldById<SerializablePair<Double, Double>>(Filters.VOLUME_FIELD).value = volume
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(
                SerializablePair(Filters.VOLUME_FIELD + "[1]", volume.first.toString()),
                SerializablePair(Filters.VOLUME_FIELD + "[2]", volume.second.toString())
        )
    }

    @Test
    fun `power field should convert to param as power_from and power_to`() {
        val power = SerializablePair(10.0, 10000.0)
        testedScreen.getValueFieldById<SerializablePair<Double, Double>>(Filters.POWER_FIELD).value = power
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(
                SerializablePair(Filters.POWER_FIELD + "[1]", power.first.toInt().toString()),
                SerializablePair(Filters.POWER_FIELD + "[2]", power.second.toInt().toString())
        )
    }

    @Test
    fun `acceleration field should convert to param as acceleration_from and acceleration_to`() {
        val acceleration = SerializablePair(1.0, 50.0)
        testedScreen.getValueFieldById<SerializablePair<Double, Double>>(Filters.ACCELERATION_FIELD).value = acceleration
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(
                SerializablePair(Filters.ACCELERATION_FIELD + "[1]", acceleration.first.toInt().toString()),
                SerializablePair(Filters.ACCELERATION_FIELD + "[2]", acceleration.second.toInt().toString())
        )
    }

    @Test
    fun `clearance field should convert to param as clearance_from`() {
        val clearance = LimitValue(100, LimitValue.LimitDirection.FROM)
        val fieldId = Filters.CLEARANCE_FIELD
        testedScreen.getValueFieldById<LimitValue>(fieldId).value = clearance
        val params = testedScreen.searchParams
        assertThat(params)
                .containsOnlyOnce(SerializablePair("$fieldId[1]", clearance.value.toString()))
                .doesNotContain(SerializablePair("$fieldId[2]", clearance.value.toString()))
    }

    @Test
    fun `fuel rate field should convert to param as fuel_rate_to`() {
        val fuelRate = LimitValue(200, LimitValue.LimitDirection.TO)
        val fieldId = Filters.FUEL_RATE_FIELD
        testedScreen.getValueFieldById<LimitValue>(fieldId).value = fuelRate
        val params = testedScreen.searchParams
        assertThat(params)
                .containsOnlyOnce(SerializablePair("$fieldId[2]", fuelRate.value.toString()))
                .doesNotContain(
                    SerializablePair("$fieldId[1]", fuelRate.value.toString())
        )
    }

    @Test
    fun `trunk volume field should convert to param as trunk_volume_from`() {
        val trunkVolume = LimitValue(300, LimitValue.LimitDirection.FROM)
        val fieldId = Filters.TRUNK_VOLUME_FIELD
        testedScreen.getValueFieldById<LimitValue>(fieldId).value = trunkVolume
        val params = testedScreen.searchParams
        assertThat(params)
                .containsOnlyOnce(SerializablePair("$fieldId[1]", trunkVolume.value.toString()))
                .doesNotContain(SerializablePair("$fieldId[2]", trunkVolume.value.toString()))
    }

    @Test
    fun `warranty field should convert to param as warranty = 1`() {
        testedScreen.getValueFieldById<Boolean>(Filters.WARRANTY_FIELD).value = true
        val params = testedScreen.searchParams
        assertThat(params).containsOnlyOnce(SerializablePair(Filters.WARRANTY_FIELD, "1"))
    }
}

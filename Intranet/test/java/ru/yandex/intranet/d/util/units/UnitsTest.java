package ru.yandex.intranet.d.util.units;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.units.UnitModel;
import ru.yandex.intranet.d.model.units.UnitsEnsembleModel;

/**
 * UnitsTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 11.02.2021
 */
class UnitsTest {
    @Test
    void convert() {
        UnitModel fromUnit = UnitModel.builder()
                .id("fromUnitId")
                .key("fromUnitKey")
                .shortNameSingularEn("from unit")
                .shortNamePluralEn("from units")
                .longNameSingularEn("from unit")
                .longNamePluralEn("from units")
                .base(10)
                .power(0)
                .deleted(false)
                .build();
        UnitModel toUnit = UnitModel.builder()
                .id("toUnitId")
                .key("toUnitKey")
                .shortNameSingularEn("to unit")
                .shortNamePluralEn("to units")
                .longNameSingularEn("to unit")
                .longNamePluralEn("to units")
                .base(10)
                .power(2)
                .deleted(false)
                .build();

        BigDecimal converted = Units.convert(BigDecimal.valueOf(1234L), fromUnit, toUnit);

        Assertions.assertEquals(BigDecimal.valueOf(12.34), converted);
    }

    @Test
    public void convertFromUnitToUnitRoundDownToInteger() {
        UnitModel unitOne = UnitModel.builder()
                .id("oneId")
                .key("oneKey")
                .shortNameSingularEn("one unit")
                .shortNamePluralEn("one units")
                .longNameSingularEn("one unit")
                .longNamePluralEn("two units")
                .base(10)
                .power(0)
                .deleted(false)
                .build();
        UnitModel unitTwo = UnitModel.builder()
                .id("twoId")
                .key("twoKey")
                .shortNameSingularEn("two unit")
                .shortNamePluralEn("two units")
                .longNameSingularEn("two unit")
                .longNamePluralEn("two units")
                .base(10)
                .power(2)
                .deleted(false)
                .build();
        UnitModel unitThree = UnitModel.builder()
                .id("threeId")
                .key("threeKey")
                .shortNameSingularEn("three unit")
                .shortNamePluralEn("three units")
                .longNameSingularEn("three unit")
                .longNamePluralEn("three units")
                .base(100)
                .power(-1)
                .deleted(false)
                .build();
        UnitModel unitFour = UnitModel.builder()
                .id("fourId")
                .key("fourKey")
                .shortNameSingularEn("four unit")
                .shortNamePluralEn("four units")
                .longNameSingularEn("four unit")
                .longNamePluralEn("four units")
                .base(100)
                .power(1)
                .deleted(false)
                .build();
        UnitModel unitFive = UnitModel.builder()
                .id("fiveId")
                .key("fiveKey")
                .shortNameSingularEn("five unit")
                .shortNamePluralEn("five units")
                .longNameSingularEn("five unit")
                .longNamePluralEn("five units")
                .base(10)
                .power(-4)
                .deleted(false)
                .build();
        UnitsEnsembleModel ensemble = UnitsEnsembleModel.builder()
                .id("id")
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("test")
                .nameRu("test")
                .descriptionEn("test")
                .descriptionRu("test")
                .fractionsAllowed(false)
                .deleted(false)
                .key("test")
                .addUnit(unitOne)
                .addUnit(unitTwo)
                .addUnit(unitThree)
                .addUnit(unitFour)
                .addUnit(unitFive)
                .build();
        BigDecimal resultOne = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitTwo, unitOne);
        Assertions.assertEquals(0, resultOne.compareTo(BigDecimal.valueOf(10001L)));
        BigDecimal resultTwo = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100"), ensemble,
                unitOne, unitOne);
        Assertions.assertEquals(0, resultTwo.compareTo(BigDecimal.valueOf(100L)));
        BigDecimal resultThree = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitOne, unitOne);
        Assertions.assertEquals(0, resultThree.compareTo(BigDecimal.valueOf(100L)));
        BigDecimal resultFour = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitOne, unitTwo);
        Assertions.assertEquals(0, resultFour.compareTo(BigDecimal.valueOf(1L)));
        BigDecimal resultFive = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("99"), ensemble,
                unitOne, unitTwo);
        Assertions.assertEquals(0, resultFive.compareTo(BigDecimal.valueOf(0L)));
        BigDecimal resultSix = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitTwo, unitThree);
        Assertions.assertEquals(0, resultSix.compareTo(BigDecimal.valueOf(1000100L)));
        BigDecimal resultSeven = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitThree, unitOne);
        Assertions.assertEquals(0, resultSeven.compareTo(BigDecimal.valueOf(1L)));
        BigDecimal resultEight = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitTwo, unitThree);
        Assertions.assertEquals(0, resultEight.compareTo(BigDecimal.valueOf(1000100L)));
        BigDecimal resultNine = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("100.01"), ensemble,
                unitThree, unitFive);
        Assertions.assertEquals(0, resultNine.compareTo(BigDecimal.valueOf(10001L)));
        BigDecimal resultTen = Units.convertFromUnitToUnitRoundDownToInteger(new BigDecimal("99"), ensemble,
                unitFour, unitOne);
        Assertions.assertEquals(0, resultTen.compareTo(BigDecimal.valueOf(9900L)));
    }

    @Test
    public void convertToBaseUnit() {
        UnitModel unitOne = UnitModel.builder()
                .id("oneId")
                .key("oneKey")
                .shortNameSingularEn("one unit")
                .shortNamePluralEn("one units")
                .longNameSingularEn("one unit")
                .longNamePluralEn("two units")
                .base(10)
                .power(0)
                .deleted(false)
                .build();
        UnitModel unitTwo = UnitModel.builder()
                .id("twoId")
                .key("twoKey")
                .shortNameSingularEn("two unit")
                .shortNamePluralEn("two units")
                .longNameSingularEn("two unit")
                .longNamePluralEn("two units")
                .base(10)
                .power(2)
                .deleted(false)
                .build();
        UnitModel unitThree = UnitModel.builder()
                .id("threeId")
                .key("threeKey")
                .shortNameSingularEn("three unit")
                .shortNamePluralEn("three units")
                .longNameSingularEn("three unit")
                .longNamePluralEn("three units")
                .base(100)
                .power(-1)
                .deleted(false)
                .build();
        UnitModel unitFour = UnitModel.builder()
                .id("fourId")
                .key("fourKey")
                .shortNameSingularEn("four unit")
                .shortNamePluralEn("four units")
                .longNameSingularEn("four unit")
                .longNamePluralEn("four units")
                .base(100)
                .power(1)
                .deleted(false)
                .build();
        UnitModel unitFive = UnitModel.builder()
                .id("fiveId")
                .key("fiveKey")
                .shortNameSingularEn("five unit")
                .shortNamePluralEn("five units")
                .longNameSingularEn("five unit")
                .longNamePluralEn("five units")
                .base(10)
                .power(-4)
                .deleted(false)
                .build();
        UnitsEnsembleModel ensemble = UnitsEnsembleModel.builder()
                .id("id")
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("test")
                .nameRu("test")
                .descriptionEn("test")
                .descriptionRu("test")
                .fractionsAllowed(false)
                .deleted(false)
                .key("test")
                .addUnit(unitOne)
                .addUnit(unitTwo)
                .addUnit(unitThree)
                .addUnit(unitFour)
                .addUnit(unitFive)
                .build();
        ResourceModel.Builder resourceBuilder = ResourceModel.builder();
        resourceBuilder
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(1L)
                .nameEn("test")
                .nameRu("test")
                .descriptionEn("test")
                .descriptionRu("test")
                .deleted(false)
                .unitsEnsembleId(ensemble.getId())
                .providerId(UUID.randomUUID().toString())
                .resourceUnits(ResourceUnitsModel.builder()
                        .defaultUnitId(unitOne.getId())
                        .addAllowedUnitIds(Set.of(unitOne.getId(), unitTwo.getId(), unitThree.getId(),
                                unitFour.getId(), unitFive.getId()))
                        .providerApiUnitId("")
                        .build())
                .managed(true)
                .orderable(true)
                .key("test")
                .readOnly(false);
        Optional<Long> resultOne = Units.convertToBaseUnit(new BigDecimal("100.01"),
                resourceBuilder.baseUnitId(unitOne.getId()).build(), ensemble, unitTwo);
        Assertions.assertEquals(10001L, resultOne.orElseThrow());
        Optional<Long> resultTwo = Units.convertToBaseUnit(new BigDecimal("100"),
                resourceBuilder.baseUnitId(unitOne.getId()).build(), ensemble, unitOne);
        Assertions.assertEquals(100L, resultTwo.orElseThrow());
        Optional<Long> resultThree = Units.convertToBaseUnit(new BigDecimal("100.01"),
                resourceBuilder.baseUnitId(unitThree.getId()).build(), ensemble, unitTwo);
        Assertions.assertEquals(1000100L, resultThree.orElseThrow());
        Optional<Long> resultFour = Units.convertToBaseUnit(new BigDecimal("1.01"),
                resourceBuilder.baseUnitId(unitThree.getId()).build(), ensemble, unitOne);
        Assertions.assertEquals(101L, resultFour.orElseThrow());
        Optional<Long> resultFive = Units.convertToBaseUnit(new BigDecimal("100.01"),
                resourceBuilder.baseUnitId(unitThree.getId()).build(), ensemble, unitTwo);
        Assertions.assertEquals(1000100L, resultFive.orElseThrow());
        Optional<Long> resultSix = Units.convertToBaseUnit(new BigDecimal("100.01"),
                resourceBuilder.baseUnitId(unitFive.getId()).build(), ensemble, unitThree);
        Assertions.assertEquals(10001L, resultSix.orElseThrow());
        Optional<Long> resultSeven = Units.convertToBaseUnit(new BigDecimal("99"),
                resourceBuilder.baseUnitId(unitOne.getId()).build(), ensemble, unitFour);
        Assertions.assertEquals(9900L, resultSeven.orElseThrow());
    }

}

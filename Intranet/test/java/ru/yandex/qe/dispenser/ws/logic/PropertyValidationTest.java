package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiProperty;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.property.PropertyDao;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertyValidationTest extends BusinessLogicTestBase {

    @Autowired
    private PropertyDao propertyDao;
    @Autowired
    private BigOrderManager bigOrderManager;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        propertyDao.clear();

        bigOrderManager.clear();
        bigOrderManager.create(BigOrder.builder(LocalDate.now().plusMonths(1)));
    }

    @Test
    public void adminCanSetProperties() {
        dispenser()
                .properties()
                .setProperty("dispenser", "enable", false)
                .performBy(AMOSOV_F);


        assertThrowsForbiddenWithMessage(() -> {
            dispenser()
                    .properties()
                    .setProperty("dispenser", "stable", true)
                    .performBy(BINARY_CAT);
        });
    }

    @Test
    public void propertyCanBeFetched() {
        dispenser()
                .properties()
                .setProperty("request_system", "active", true)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProperty property = dispenser()
                .properties()
                .getProperty("request_system", "active")
                .perform();

        assertEquals(true, property.getValue());
    }


    @Test
    public void propertiesCanBeFetched() {
        dispenser()
                .properties()
                .setProperty("request_system", "active", true)
                .performBy(AMOSOV_F);

        dispenser()
                .properties()
                .setProperty("request_system", "tested", false)
                .performBy(AMOSOV_F);

        dispenser()
                .properties()
                .setProperty("request_system", "deployed", true)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiProperty> properties = dispenser()
                .properties()
                .getProperties("request_system")
                .perform();

        assertEquals(3, properties.size());
    }

    @Test
    public void stringPropertiesCanBeFetched() {
        final String entityKey = "testing_string";
        final String propertyKey1 = "Dispenser";
        final String value1 = "Cool Dispenser @@!3#$~";
        final String propertyKey2 = "My_cat";
        final String value2 = "Yellow cat drunk red blood &***[ё";
        final String propertyKey3 = "Default_property_key";
        final String value3 = "Yandex-team.ru@235";

        Map<String, DiProperty> propertyByKey = dispenser()
                .properties()
                .getProperties(entityKey)
                .perform()
                .stream()
                .collect(Collectors.toMap(DiProperty::getEntityKey, Function.identity()));
        assertEquals(0, propertyByKey.size());

        dispenser()
                .properties()
                .setProperty(entityKey, propertyKey1, value1)
                .performBy(AMOSOV_F);
        dispenser()
                .properties()
                .setProperty(entityKey, propertyKey2, value2)
                .performBy(AMOSOV_F);
        assertThrowsForbiddenWithMessage(() -> {
            dispenser()
                    .properties()
                    .setProperty(entityKey, propertyKey3, value3)
                    .performBy(BINARY_CAT);
        });

        updateHierarchy();

        propertyByKey = dispenser()
                .properties()
                .getProperties(entityKey)
                .perform()
                .stream()
                .collect(Collectors.toMap(DiProperty::getPropertyKey, Function.identity()));

        assertEquals(2, propertyByKey.size());
        assertEquals(value1, propertyByKey.get(propertyKey1).getValue());
        assertEquals(value2, propertyByKey.get(propertyKey2).getValue());

        final DiProperty property1 = dispenser()
                .properties()
                .getProperty(entityKey, propertyKey1)
                .perform();
        assertEquals(value1, property1.getValue());

        final DiProperty property2 = dispenser()
                .properties()
                .getProperty(entityKey, propertyKey2)
                .perform();
        assertEquals(value2, property2.getValue());
    }

    @Test
    public void propertyCanChangeTypes() {
        dispenser()
                .properties()
                .setProperty("dispenser", "enable", "NOT")
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProperty prop1 = dispenser().properties()
                .getProperty("dispenser", "enable")
                .perform();

        assertEquals("NOT", prop1.getValue());

        dispenser()
                .properties()
                .setProperty("dispenser", "enable", true)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProperty prop2 = dispenser().properties()
                .getProperty("dispenser", "enable")
                .perform();

        assertEquals(true, prop2.getValue());
    }

    @Test
    public void propertyCanDeleted() {
        dispenser()
                .properties()
                .setProperty("dispenser", "forDelete", "veryImportantValue")
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProperty prop1 = dispenser().properties()
                .getProperty("dispenser", "forDelete")
                .perform();

        assertNotNull(prop1);

        final Boolean results = dispenser()
                .properties()
                .deleteProperty("dispenser", "forDelete")
                .performBy(AMOSOV_F);

        assertTrue(results);

        updateHierarchy();

        assertThrows(IllegalStateException.class, () -> {
            dispenser().properties()
                    .getProperty("dispenser", "forDelete")
                    .perform();
        });
    }

}

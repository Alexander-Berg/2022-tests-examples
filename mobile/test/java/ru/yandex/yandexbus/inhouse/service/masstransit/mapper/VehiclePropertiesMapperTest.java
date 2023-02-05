package ru.yandex.yandexbus.inhouse.service.masstransit.mapper;


import com.yandex.mapkit.transport.masstransit.Vehicle;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import ru.yandex.yandexbus.inhouse.BaseTest;
import ru.yandex.yandexbus.inhouse.model.VehicleProperty;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class VehiclePropertiesMapperTest extends BaseTest {

    @Test
    public void noProperties() throws Exception {
        Vehicle.Properties properties = Mockito.mock(Vehicle.Properties.class);

        // Mockito returns default value for primitives (for boolean – false)
        // So there is no need to mock any methods

        Set<VehicleProperty> actual = VehiclePropertiesMapper.getVehicleProperties(properties);

        // Set must be empty
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void nullPropertiesObject() throws Exception {
        Set<VehicleProperty> actual = VehiclePropertiesMapper.getVehicleProperties(null);

        // Set must be empty
        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void nullPropertiesInsidePropertiesObject() throws Exception {
        Vehicle.Properties properties = Mockito.mock(Vehicle.Properties.class);

        when(properties.getBikesAllowed()).thenReturn(null);
        when(properties.getWheelchairAccessible()).thenReturn(null);

        Set<VehicleProperty> actual = VehiclePropertiesMapper.getVehicleProperties(properties);

        assertEquals(true, actual.isEmpty());
    }

    @Test
    public void allProperties() throws Exception {
        Vehicle.Properties properties = Mockito.mock(Vehicle.Properties.class);

        when(properties.getLowFloor()).thenReturn(true);
        when(properties.getBikesAllowed()).thenReturn(true);
        when(properties.getWheelchairAccessible()).thenReturn(true);

        // Mockito returns default value for primitives
        // So there is no need to mock other methods

        Set<VehicleProperty> actual = VehiclePropertiesMapper.getVehicleProperties(properties);

        // Set must contains 3 elements
        assertEquals(3, actual.size());

        // Set must contain all 3 properties
        assertEquals(true, actual.contains(VehicleProperty.BIKES_ALLOWED));
        assertEquals(true, actual.contains(VehicleProperty.LOW_FLOOR));
        assertEquals(true, actual.contains(VehicleProperty.WEELCHAIR_ACESSIBLE));
    }
}

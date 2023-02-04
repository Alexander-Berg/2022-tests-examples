package ru.yandex.realty.buildinginfo.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.buildinginfo.model.RawBuilding;
import ru.yandex.realty.model.offer.*;

import java.io.ByteArrayInputStream;

import static ru.yandex.realty.util.JsonUtil.DEFAULT_JSON_FACTORY;

/**
 * author: rmuzhikov
 */
public class RawBuildingJsonConverterTest {

    @Test
    public void testSerDe() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RawBuilding expected = getBuilding();
        try (JsonGenerator generator = DEFAULT_JSON_FACTORY.createGenerator(byteArrayOutputStream, JsonEncoding.UTF8)) {
            RawBuildingJsonConverter.toJson(expected, generator);
        }
        try (JsonParser parser = DEFAULT_JSON_FACTORY.createParser(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            RawBuilding actual = RawBuildingJsonConverter.fromJson(parser);
            Assert.assertEquals(expected, actual);
        }
    }

    private static RawBuilding getBuilding() {
        RawBuilding.Builder builder = new RawBuilding.Builder();
        builder.id = "qwerty";
        builder.latitude = 123f;
        builder.longitude = 321f;
        builder.buildingName = "Greenland";
        builder.buildYear = 2016;
        builder.buildingType = BuildingType.MONOLIT;
        builder.buildingSeries = null;
        builder.totalFloors = 21;
        builder.hasParking = false;
        builder.hasLift = true;
        builder.hasRubbishChute = true;
        builder.hasSecurity = false;
        builder.isGuarded = null;
        builder.ceilingHeight = 2.75f;
        return builder.build();
    }

}
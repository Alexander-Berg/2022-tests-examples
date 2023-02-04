package ru.yandex.realty.buildinginfo.converter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.buildinginfo.model.Metro;
import ru.yandex.realty.model.location.TransportType;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.yandex.realty.util.JsonUtil.DEFAULT_JSON_FACTORY;


/**
 * author: rmuzhikov
 */
public class MetroJsonConverterTest {
    @Test
    public void testSerDe() throws Exception {
        List<Metro> expected = getMetros();
        checkSerDe(expected);
    }

    @Test
    public void testEmptySerDe() throws Exception {
        List<Metro> expected = Collections.emptyList();
        checkSerDe(expected);
    }

    private void checkSerDe(List<Metro> data) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (JsonGenerator generator = DEFAULT_JSON_FACTORY.createGenerator(byteArrayOutputStream, JsonEncoding.UTF8)) {
            MetroJsonConverter.toJsonArray(data, generator);
        }

        try (JsonParser parser = DEFAULT_JSON_FACTORY.createParser(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            List<Metro> actual = MetroJsonConverter.fromJsonArray(parser);
            Assert.assertEquals(data, actual);
        }
    }

    private static List<Metro> getMetros() {
        return Arrays.asList(
                new Metro(1, TransportType.ON_TRANSPORT, 12),
                new Metro(2, TransportType.ON_FOOT, 30),
                new Metro(2, TransportType.ON_TRANSPORT, 5)
        );
    }
}
package ru.yandex.webmaster3.core.turbo;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import ru.yandex.webmaster3.core.util.json.JsonMapping;

/**
 * ishalaru
 * 25.08.2020
 **/
public class AdvertisingIntegrationServiceTest {

    @Test
    public void testParse() throws IOException {
       /* String value = new String(Files.readAllBytes(Path.of("C:\\work\\pi_api\\", "yndxturbo_match.out")));
        final AdvertisingIntegrationService.Response data = JsonMapping.readValue(value, AdvertisingIntegrationService.Response.class);
        System.out.println(data);
       */ /*Map<String, AdvertisingIntegrationService.Relationship> map = new HashMap<>();
        AdvertisingIntegrationService.Data data = new AdvertisingIntegrationService.Data(map,"test","123",null,null);
        map.put("test",new AdvertisingIntegrationService.Relationship(new AdvertisingIntegrationService.Link("rel1","self1")));
        map.put("test1",new AdvertisingIntegrationService.Relationship(new AdvertisingIntegrationService.Link("rel2","self2")));
        AdvertisingIntegrationService.Response response = new AdvertisingIntegrationService.Response(null, List.of(data));
        System.out.println(JsonMapping.writeValueAsString(response));
        */
    }
}
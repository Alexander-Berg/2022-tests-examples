package ru.yandex.infra.auth.idm.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class IdmMembershipTest {
    @Test
    void testParsing() {
        String input = URLEncoder.encode(
                "data=[{\"login\": \"somebody\", \"group\": 24836, \"passport_login\": \"\"}, {\"login\": \"dude\", \"group\": 24836, \"passport_login\": \"\"}, {\"login\": \"someone\", \"group\": 24836, \"passport_login\": \"\"}]",
                UTF_8
        );

        String result = URLDecoder.decode(input, UTF_8);

        assertDoesNotThrow(() -> {
            String[] chunks = result.split("=");

            ObjectMapper mapper = new ObjectMapper();
            TypeFactory typeFactory = mapper.getTypeFactory();
            List<IdmMembership> idmMembershipList = mapper.readValue(chunks[1],
                    typeFactory.constructCollectionType(List.class, IdmMembership.class));

            assertEquals(ImmutableList.of(
                    new IdmMembership("somebody", 24836L),
                    new IdmMembership("dude", 24836L),
                    new IdmMembership("someone", 24836L)
            ), idmMembershipList);

        });
    }
}

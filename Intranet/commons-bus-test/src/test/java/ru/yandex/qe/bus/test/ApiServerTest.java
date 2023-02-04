package ru.yandex.qe.bus.test;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.qe.spring.profiles.Profiles;

/**
 * Established by terry
 * on 30.01.14.
 */
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath*:spring/qe-plugin-spring.xml", "classpath:spring/bus-server.xml"})
public class ApiServerTest extends ApiClientTest {
}

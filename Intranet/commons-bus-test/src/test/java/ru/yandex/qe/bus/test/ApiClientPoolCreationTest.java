package ru.yandex.qe.bus.test;

import java.util.HashSet;
import java.util.Map;

import javax.annotation.Resource;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.qe.bus.api.ApiService;
import ru.yandex.qe.bus.factories.client.BusClientSet;
import ru.yandex.qe.spring.profiles.Profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * Established by terry
 * on 30.01.14.
 */
@ActiveProfiles(Profiles.TESTING)
@ContextConfiguration({"classpath*:spring/qe-plugin-spring.xml", "classpath:spring/bus-clients.xml"})
@ExtendWith(SpringExtension.class)
public class ApiClientPoolCreationTest {

    @Resource(name = "apiClientsPool")
    private BusClientSet<ApiService> pool;

    @Resource(name = "apiClientsPool2")
    private BusClientSet<ApiService> pool2;

    @Test
    public void client_pool_correct_factory_bean() {
        final Map<String, ApiService> address2Client = pool.getAddress2Client();
        assertThat(address2Client.size(), equalTo(4));
        Assertions.assertNotNull(pool.getClient("http://localhost:12341/api"));
        Assertions.assertNotNull(pool.getClient("http://localhost:12342/api"));
        Assertions.assertNotNull(pool.getClient("http://localhost:12343/api"));
        Assertions.assertNotNull(pool.getClient("http://localhost:12344/api"));
    }

    @Test
    public void client_pool_iteration() {
        final HashSet<Object> urls = Sets.newHashSet(Iterables.transform(pool, new Function<Map.Entry<String, ApiService>, Object>() {
            @Override
            public Object apply(Map.Entry<String, ApiService> input) {
                return input.getKey();
            }
        }));
        assertThat(urls, hasItem("http://localhost:12341/api"));
        assertThat(urls, hasItem("http://localhost:12342/api"));
        assertThat(urls, hasItem("http://localhost:12343/api"));
        assertThat(urls, hasItem("http://localhost:12344/api"));
    }

    @Test
    public void client_pool_2_correct_factory_bean() {
        final Map<String, ApiService> address2Client = pool2.getAddress2Client();
        assertThat(address2Client.size(), equalTo(4));
        Assertions.assertNotNull(pool2.getClient("http://localhost:12341/api"));
        Assertions.assertNotNull(pool2.getClient("http://localhost:12342/api"));
        Assertions.assertNotNull(pool2.getClient("http://localhost:12343/api"));
        Assertions.assertNotNull(pool2.getClient("http://localhost:12344/api"));
    }
}

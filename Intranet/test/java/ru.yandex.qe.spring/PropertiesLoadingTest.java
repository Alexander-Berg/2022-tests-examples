package ru.yandex.qe.spring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import ru.yandex.qe.spring.profiles.Profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

public class PropertiesLoadingTest {

    @Test
    public void check_loading_spring_resolver() {
        final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final Environment environment = Mockito.mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);

        try {
            final String[] profiles = {Profiles.DEVELOPMENT};
            for (String profile : profiles) {
                final PropertyResolverBean propertyResolverBean = new PropertyResolverBean();
                propertyResolverBean.setApplicationContext(applicationContext);
                when(environment.getActiveProfiles()).thenReturn(new String[]{profile});
                final Properties properties = propertyResolverBean.mergeProperties();
                checkPropertiesContent(false, profile, properties);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    private void checkPropertiesContent(boolean checkHostOverlap, String profile, Properties properties) throws UnknownHostException {
        Assertions.assertTrue(!properties.isEmpty());
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            if (key.contains(profile)) {
                assertThat(value, equalTo(profile));
            } else if (checkHostOverlap && key.contains("local")) {
                assertThat(value, equalTo("local"));
            } else if (key.equals("qe.server-profile")) {
                assertThat(value, equalTo(profile));
            } else if (key.equals("qe.hostname")) {
                assertThat(value, equalTo(InetAddress.getLocalHost().getHostName()));
            } else if (key.equals("qe.server-environment")) {
                assertThat(value, equalTo(profile));
            } else if (key.startsWith("env.")) {
                // ignore
            } else {
                assertThat(value, equalTo("root"));
            }
        }
    }
}

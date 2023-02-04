package ru.yandex.payments.testing.micronaut_cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
@RunWith(Cucumber.class)
@CucumberOptions(features = "classpath:test.feature")
class IntegrationTest extends MicronautCucumberTest {
    @BeforeClass
    public static void init() {
        init(IntegrationTest.class);
    }

    @MockBean(TestMockBean.class)
    TestMockBean mockBean() {
        val bean = mock(TestMockBean.class);
        when(bean.getNumber()).thenReturn(100500);
        return bean;
    }
}

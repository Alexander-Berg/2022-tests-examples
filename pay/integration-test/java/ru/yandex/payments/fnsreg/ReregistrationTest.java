package ru.yandex.payments.fnsreg;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import ru.yandex.payments.testing.micronaut_cucumber.MicronautCucumberTest;

import static ru.yandex.payments.testing.micronaut_cucumber.MicronautCucumberTest.EXTRA_GLUE;

@RunWith(Cucumber.class)
@MicronautTest(propertySources = "classpath:application-integration-test.yml")
@CucumberOptions(features = "classpath:reregistration.feature", extraGlue = EXTRA_GLUE)
class ReregistrationTest extends MicronautCucumberTest {
    @BeforeClass
    public static void init() {
        init(ReregistrationTest.class);
    }
}

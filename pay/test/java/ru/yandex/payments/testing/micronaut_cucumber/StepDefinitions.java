package ru.yandex.payments.testing.micronaut_cucumber;

import javax.inject.Inject;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class StepDefinitions {
    @Inject
    TestBean testBean;

    @Inject
    TestMockBean testMockBean;

    @When("we store value {int} into test bean")
    public void weStoreValueIntoTestBean(int value) {
        testBean.setState(value);
    }

    @Then("test bean contains value {int}")
    public void testBeanContainsValue(int value) {
        assertThat(testBean.getState()).isEqualTo(value);
    }

    @When("we mock test bean")
    public void weMockTestBean() {
        // bean already mocked in test class
    }

    @Then("test bean has expected state")
    public void testBeanHasExpectedState() {
        assertThat(testMockBean.getNumber()).isEqualTo(100500);
    }
}

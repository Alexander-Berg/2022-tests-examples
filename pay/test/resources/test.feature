Feature: Features test

    Scenario: Test DI is supported
        When we store value 42 into test bean
        Then test bean contains value 42

    Scenario: Test mocking is supported
        When we mock test bean
        Then test bean has expected state

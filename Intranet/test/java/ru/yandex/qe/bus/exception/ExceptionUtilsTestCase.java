package ru.yandex.qe.bus.exception;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionUtilsTestCase {

    @Test
    public void getExceptionMessage_null() {
        String result = ExceptionUtils.getExceptionMessage(null);
        MatcherAssert.assertThat(result, Matchers.equalTo("null"));
    }

    @Test
    public void getExceptionMessage_null_pointer_exception() {
        NullPointerException exception = new NullPointerException();
        String result = ExceptionUtils.getExceptionMessage(exception);
        MatcherAssert.assertThat(result, Matchers.startsWith(exception.toString() + " - "));
        MatcherAssert.assertThat(result, Matchers.not(Matchers.containsString(" <- ")));
    }

    @Test
    public void getExceptionMessage_exception_with_clause() {
        NullPointerException causeException = new NullPointerException();
        IllegalStateException exception = new IllegalStateException(causeException);
        String result = ExceptionUtils.getExceptionMessage(exception);
        MatcherAssert.assertThat(result, Matchers.containsString(" <- "));
        String[] data = result.split(" <- ");
        MatcherAssert.assertThat(data[0], Matchers.startsWith(exception.toString() + " - "));
        MatcherAssert.assertThat(data[1], Matchers.startsWith(causeException.toString() + " - "));
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void exception_message_with_string_conversion_pattern_without_arguments_does_not_fail() {
        String message = "%d%s%c%t%D";
        assertMessageEquals(ExceptionUtils.newBadRequestException(message), message);
        assertMessageEquals(ExceptionUtils.newBadRequestException(new RuntimeException("test"), message), message);
        assertMessageEquals(ExceptionUtils.newInternalServerErrorException(message), message);
        assertMessageEquals(
                ExceptionUtils.newInternalServerErrorException(new RuntimeException("test"), message),
                message
        );
        assertMessageEquals(ExceptionUtils.newNotAuthorizedException(message), message);
        assertMessageEquals(ExceptionUtils.newNotAuthorizedException(new RuntimeException("test"), message), message);
        assertMessageEquals(ExceptionUtils.newNotFoundException(message), message);
        assertMessageEquals(ExceptionUtils.newNotFoundException(new RuntimeException("test"), message), message);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void exception_message_with_arguments_formatted_as_expected() {
        String messagePattern = "test %d with name %s";
        Object[] args = new Object[] {3, "test-name"};
        String expectedMessage = String.format(messagePattern, args);

        assertMessageEquals(ExceptionUtils.newBadRequestException(messagePattern, args), expectedMessage);
        assertMessageEquals(
                ExceptionUtils.newBadRequestException(new RuntimeException("test"), messagePattern, args),
                expectedMessage
        );
        assertMessageEquals(ExceptionUtils.newInternalServerErrorException(messagePattern, args), expectedMessage);
        assertMessageEquals(
                ExceptionUtils.newInternalServerErrorException(new RuntimeException("test"), messagePattern, args),
                expectedMessage
        );
        assertMessageEquals(ExceptionUtils.newNotAuthorizedException(messagePattern, args), expectedMessage);
        assertMessageEquals(
                ExceptionUtils.newNotAuthorizedException(new RuntimeException("test"), messagePattern, args),
                expectedMessage
        );
        assertMessageEquals(ExceptionUtils.newNotFoundException(messagePattern, args), expectedMessage);
        assertMessageEquals(
                ExceptionUtils.newNotFoundException(new RuntimeException("test"), messagePattern, args),
                expectedMessage
        );
    }

    private void assertMessageEquals(Exception e, String expectedMessage) {
        assertEquals(e.getMessage(), expectedMessage);
    }
}

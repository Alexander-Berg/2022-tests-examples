package ru.yandex.partner.defaultconfiguration.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ErrorBoosterMessageConverterTest {

    @Test
    void convert() {
        ILoggingEvent iLoggingEvent = mock(ILoggingEvent.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        ErrorBoosterMessageConverter messageConverter = new ErrorBoosterMessageConverter(objectMapper);

        doReturn("TestMessage").when(iLoggingEvent).getFormattedMessage();
        doReturn(111999L).when(iLoggingEvent).getTimeStamp();
        doReturn(Level.ERROR).when(iLoggingEvent).getLevel();

        IThrowableProxy iThrowableProxy = mock(IThrowableProxy.class);
        doReturn(iThrowableProxy).when(iLoggingEvent).getThrowableProxy();
        StackTraceElementProxy[] elements = new StackTraceElementProxy[]{
                new StackTraceElementProxy(new StackTraceElement("declaringClass", "methodName", "fileName", 100)),
                new StackTraceElementProxy(new StackTraceElement("declaringClass", "methodName", "fileName", 99)),
                new StackTraceElementProxy(new StackTraceElement("declaringClass", "methodName", "fileName", 98))
        };
        doReturn(elements).when(iThrowableProxy).getStackTraceElementProxyArray();
        doReturn("TestMessage").when(iThrowableProxy).getMessage();
        doReturn(Object.class.toString()).when(iThrowableProxy).getClassName();

        ByteString byteString = messageConverter.convert(iLoggingEvent);

        assertEquals("{\"dc\":\"\",\"env\":\"unknown\",\"host\":\"\",\"language\":\"java\",\"level\":\"error\"," +
                "\"message\":\"TestMessage\",\"project\":\"partner\",\"service\":\"java_backend\"," +
                "\"source\":\"java_unknown\",\"stack\":\"class java.lang.Object: TestMessage\\n\\tat declaringClass" +
                ".methodName(fileName:100)\\n\\tat declaringClass.methodName(fileName:99)\\n\\tat declaringClass" +
                ".methodName(fileName:98)\\n\",\"timestamp\":111999}", byteString.toStringUtf8());


        // new Test
        ErrorBoosterMessageConverter.setErrorBoosterEnvBySpringProfile("prod");
        ErrorBoosterMessageConverter.setErrorApplicationSource("test_app");

        byteString = messageConverter.convert(iLoggingEvent);

        assertEquals("{\"dc\":\"\",\"env\":\"production\",\"host\":\"\",\"language\":\"java\",\"level\":\"error\"," +
                "\"message\":\"TestMessage\",\"project\":\"partner\",\"service\":\"java_backend\"," +
                "\"source\":\"test_app\",\"stack\":\"class java.lang.Object: TestMessage\\n\\tat declaringClass" +
                ".methodName(fileName:100)\\n\\tat declaringClass.methodName(fileName:99)\\n\\tat declaringClass" +
                ".methodName(fileName:98)\\n\",\"timestamp\":111999}", byteString.toStringUtf8());


        // new Test
        MDC.put(ErrorBoosterMessageConverter.MDCKeys.YANDEX_UID, "" + 10L);
        MDC.put(ErrorBoosterMessageConverter.MDCKeys.REQUEST_PATH, "/v1/jsonapi");

        byteString = messageConverter.convert(iLoggingEvent);
        assertEquals("{\"additional\":{\"requestpath\":\"/v1/jsonapi\"},\"dc\":\"\",\"env\":\"production\"," +
                        "\"host\":\"\",\"language\":\"java\",\"level\":\"error\",\"message\":\"TestMessage\"," +
                        "\"project\":\"partner\",\"service\":\"java_backend\",\"source\":\"test_app\"," +
                        "\"stack\":\"class java.lang.Object: TestMessage\\n\\tat declaringClass.methodName" +
                        "(fileName:100)\\n\\tat declaringClass.methodName(fileName:99)\\n\\tat declaringClass" +
                        ".methodName(fileName:98)\\n\",\"timestamp\":111999,\"yandexuid\":10}",
                byteString.toStringUtf8());

        // new Test
        MDC.put(ErrorBoosterMessageConverter.MDCKeys.REQUEST_METHOD, "POST");
        byteString = messageConverter.convert(iLoggingEvent);
        assertEquals("{\"additional\":{\"requestmethod\":\"POST\",\"requestpath\":\"/v1/jsonapi\"},\"dc\":\"\"," +
                        "\"env\":\"production\",\"host\":\"\",\"language\":\"java\",\"level\":\"error\"," +
                        "\"message\":\"TestMessage\",\"project\":\"partner\",\"service\":\"java_backend\"," +
                        "\"source\":\"test_app\",\"stack\":\"class java.lang.Object: TestMessage\\n\\tat " +
                        "declaringClass.methodName(fileName:100)\\n\\tat declaringClass.methodName(fileName:99)" +
                        "\\n\\tat declaringClass.methodName(fileName:98)\\n\",\"timestamp\":111999,\"yandexuid\":10}",
                byteString.toStringUtf8());
    }
}

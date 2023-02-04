package ru.yandex.example;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.tasklet.api.v2.WellKnownStructures.GenericBinary;
import ru.yandex.tasklet.test.TaskletContextStub;

class DummyJavaTaskletTest {

    private static TaskletContextStub stub;

    @BeforeAll
    static void initAll() {
        stub = TaskletContextStub.stub(GenericBinary.class, GenericBinary.class);
    }

    @AfterAll
    static void closeAll() {
        stub.close();
    }

    @Test
    void simpleTest() {
        var input = GenericBinary.newBuilder()
                .setPayload(ByteString.copyFromUtf8("{\"result_data\": \"test-value\"}"))
                .build();

        var output = new DummyJavaTasklet().execute(input, stub.getContext()).getResult();

        Assertions.assertEquals(GenericBinary.newBuilder()
                        .setPayload(ByteString.copyFromUtf8("test-value"))
                        .build(),
                output);
    }

}

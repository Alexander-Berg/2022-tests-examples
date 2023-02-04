package ru.yandex.example;

import com.google.protobuf.ByteString;
import org.json.JSONObject;

import ru.yandex.tasklet.Result;
import ru.yandex.tasklet.TaskletContext;
import ru.yandex.tasklet.TaskletRuntime;
import ru.yandex.tasklet.api.v2.WellKnownStructures.GenericBinary;

public class DummyJavaTasklet {

    public Result<GenericBinary> execute(GenericBinary input, TaskletContext context) {
        JSONObject json = new JSONObject(input.getPayload().toStringUtf8());
        return Result.of(GenericBinary.newBuilder()
                .setPayload(ByteString.copyFromUtf8(json.getString("result_data")))
                .build());
    }

    public static void main(String[] args) {
        var impl = new DummyJavaTasklet();

        TaskletRuntime.execute(
                GenericBinary.class,
                GenericBinary.class,
                args,
                impl::execute
        );
    }
}

package com.yandex.frankenstein;

import com.yandex.frankenstein.CommandsProvider.Argument;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.yandex.frankenstein.CommandsProvider.argument;
import static org.assertj.core.api.Assertions.assertThat;

public class CommandsProviderTest {

    private static final int ID = 1;
    private static final String CLAZZ = "CommandClass";
    private static final String NAME = "commandName";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private final List<Argument> mArguments = Collections.singletonList(argument(KEY, VALUE));
    private final JSONObject mCommandJson = buildCommandJson(ID, CLAZZ, NAME, KEY, VALUE);

    private static final int OLD_ID = ID - 1;
    private static final String OLD_CLAZZ = "OldCommandClass";
    private static final String OLD_NAME = "oldCommandName";
    private static final String OLD_KEY = "oldKey";
    private static final String OLD_VALUE = "oldValue";
    private final List<Argument> mOldArguments = Collections.singletonList(argument(OLD_KEY, OLD_VALUE));
    private final JSONObject mOldCommandJson = buildCommandJson(OLD_ID, OLD_CLAZZ, OLD_NAME, OLD_KEY, OLD_VALUE);

    private static final String LAST_COMMAND_ID = String.valueOf(OLD_ID);
    private final CommandsProvider mCommandsProvider = new CommandsProvider();

    @Before
    public void setUp() {
        mCommandsProvider.add(OLD_CLAZZ, OLD_NAME, mOldArguments);
    }

    @Test
    public void testGetAfterAdd() {
        mCommandsProvider.add(CLAZZ, NAME, mArguments);
        final List<JSONObject> commands = mCommandsProvider.get(LAST_COMMAND_ID);

        assertThat(commands)
                .usingElementComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .containsExactly(mCommandJson);
    }

    @Test
    public void testGetBeforeAdd() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                mCommandsProvider.add(CLAZZ, NAME, mArguments);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        final List<JSONObject> commands = mCommandsProvider.get(LAST_COMMAND_ID);

        assertThat(commands)
                .usingElementComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .containsExactly(mCommandJson);
    }

    @Test
    public void testGetWithoutAdd() {
        final List<JSONObject> commands = mCommandsProvider.get(LAST_COMMAND_ID);

        assertThat(commands).isEmpty();
    }

    @Test
    public void testGetWithoutLastCommandId() {
        final List<JSONObject> commands = mCommandsProvider.get(null);

        assertThat(commands)
                .usingElementComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .containsExactly(mOldCommandJson);
    }

    private JSONObject buildCommandJson(final int commandId, final String commandClass, final String commandName,
                                        final String argumentKey, final String argumentValue) {
        return new JSONObject().put("id", String.valueOf(commandId)).put("class", commandClass)
                .put("name", commandName).put("arguments", new JSONObject().put(argumentKey, argumentValue));
    }
}

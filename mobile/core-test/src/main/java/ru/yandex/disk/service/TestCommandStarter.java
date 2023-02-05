package ru.yandex.disk.service;

import ru.yandex.disk.Log;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCommandStarter implements CommandStarter {

    private static final String TAG = "TestCommandStarter";

    private HashMap<Class, Command> commandsMap = new HashMap<>();
    private List<CommandRequest> queue = new CopyOnWriteArrayList<>();
    private List<Class> completed = new CopyOnWriteArrayList<>();

    private boolean executeCommands;

    @Override
    public void start(@Nonnull final CommandRequest request) {
        Log.d(TAG, "start: " + request + ", " + executeCommands);
        Command command = commandsMap.get(request.getClass());
        assert command != null;
        if (executeCommands) {
            command.execute(request);
            completed.add(request.getClass());
        } else {
            queue.add(request);
        }
    }

    public void registerCommand(Class request, Command command) {
        Log.d(TAG, "registerCommand: " + request);
        commandsMap.put(request, command);
    }

    public void executeQueue(boolean withNew) {
        executeCommands |= withNew;
        List<CommandRequest> requests = new ArrayList<>(queue);
        queue.clear();
        for (CommandRequest request : requests) {
            Command<CommandRequest> command = commandsMap.get(request.getClass());
            assert command != null;
            Log.d(TAG, "executeCommand: " + command);
            command.execute(request);
            completed.add(request.getClass());
        }
        Log.d(TAG, "executeQueue done: " + withNew + ", " + queue.size());
        if (withNew && executeCommands && !queue.isEmpty()) {
            executeQueue(true);
        }
    }

    public List<CommandRequest> getQueue() {
        return queue;
    }

    public void setExecuteCommands(boolean executeCommands) {
        if (executeCommands) {
            Log.d(TAG, "setExecuteCommands: " + executeCommands);
        }
        this.executeCommands = executeCommands;
    }

    public List<Class> getCompleted() {
        return completed;
    }
}

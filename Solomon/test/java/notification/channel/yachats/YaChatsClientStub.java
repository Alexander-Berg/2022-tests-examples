package ru.yandex.solomon.alert.notification.channel.yachats;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Assert;

import ru.yandex.solomon.alert.yachats.YaChatsClient;

public class YaChatsClientStub implements YaChatsClient {
    private record Session(String login, String groupId, String text, CompletableFuture<Response> response) {
    }

    private final ConcurrentLinkedQueue<Session> sessions = new ConcurrentLinkedQueue<>();

    public void addResponse(String login, String groupId, String text, CompletableFuture<Response> response) {
        sessions.add(new Session(login, groupId, text, response));
    }

    public int getLeftResponseCount() {
        return sessions.size();
    }

    @Override
    public CompletableFuture<Response> sendMessage(String login, String groupId, String text) {
        var session = sessions.poll();
        Assert.assertNotNull("No predefined responses given", session);
        Assert.assertEquals(session.login, login);
        Assert.assertEquals(session.groupId, groupId);
        Assert.assertEquals(session.text, text);
        return CompletableFuture.supplyAsync(() -> null)
                .thenCompose(aVoid -> session.response);
    }

    @Override
    public CompletableFuture<List<GroupsInfo>> getChats() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }
}

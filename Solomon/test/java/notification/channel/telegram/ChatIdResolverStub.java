package ru.yandex.solomon.alert.notification.channel.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.HashBiMap;


/**
 * @author alexlovkov
 **/
public class ChatIdResolverStub implements ChatIdStorage {

    public static final long CHAT_ID = 666;
    public static final long GROUP_CHAT_ID = -111111111111L;

    private final HashBiMap<String, TelegramChat> loginToChaId;
    private final HashBiMap<String, TelegramChat> titleToChaId;

    private int queries;
    private boolean returnNull;

    public ChatIdResolverStub() {
        this.queries = 0;
        this.loginToChaId = HashBiMap.create();
        this.titleToChaId = HashBiMap.create();
        this.returnNull = false;
    }

    @Override
    public CompletableFuture<Void> loadAll() {
        return CompletableFuture.supplyAsync(null);
    }

    @Override
    public long getChatIdByTelegramLogin(String name) {
        if (returnNull) {
            return 0;
        }
        return CHAT_ID + queries++;
    }

    @Override
    public long getChatIdByGroupTitle(String groupTitle) {
        if (returnNull) {
            return 0;
        }
        return GROUP_CHAT_ID + queries++;
    }

    @Override
    public List<String> listGroups() {
        return new ArrayList<>(titleToChaId.keySet());
    }

    @Override
    public void removeTelegramLogin(String telegramLogin) {
        loginToChaId.remove(telegramLogin);
    }

    @Override
    public void removeGroupTitle(String groupChat) {
        titleToChaId.remove(groupChat);
    }

    @Override
    public void addTelegramLogin(long chatId, String login) {
        loginToChaId.put(login, new TelegramChat(chatId));
    }

    @Override
    public void addGroupTitle(long chatId, String groupTitle) {
        titleToChaId.put(groupTitle, new TelegramChat(chatId));
    }

    @Override
    public String resolveGroupTitle(long chatId) {
        if (returnNull) {
            return null;
        }
        String result = titleToChaId.inverse().get(new TelegramChat(chatId));
        if (result == null) {
            result = "fakeTitle";
        }
        return result;
    }

    public void returnNull() {
        this.returnNull = true;
    }

}

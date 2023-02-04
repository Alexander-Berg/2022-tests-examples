package ru.yandex.solomon.alert.notification.channel.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import ru.yandex.solomon.alert.telegram.TelegramClient;
import ru.yandex.solomon.alert.telegram.dto.InlineKeyboard;
import ru.yandex.solomon.alert.telegram.dto.ParseMode;
import ru.yandex.solomon.alert.telegram.dto.TelegramSendMessage;
import ru.yandex.solomon.alert.telegram.dto.TelegramUpdate;


/**
 * @author alexlovkov
 **/
public class TelegramClientStub implements TelegramClient {

    private final ConcurrentLinkedQueue<TelegramClientStub.Message> sandbox = new ConcurrentLinkedQueue<>();
    private final Queue<TelegramUpdate> updates = new ConcurrentLinkedQueue<>();
    private final List<Integer> codes = new ArrayList<>();
    private CountDownLatch countDownLatch;
    private final AtomicLong messageIdSequence = new AtomicLong(523452123L);

    public final AtomicInteger codeIndex = new AtomicInteger(0);

    public TelegramClientStub() {
        this.countDownLatch = new CountDownLatch(0);
    }

    @Override
    public CompletableFuture<String> getMe() {
        return CompletableFuture.supplyAsync(() -> "telegram-client-stub");
    }

    @Override
    public CompletableFuture<List<TelegramUpdate>> getUpdates(long offset, int limit, int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> updates.stream()
            .filter(e -> e.getId() >= offset)
            .limit(limit)
            .collect(Collectors.toList()));
    }

    private int getNextStatusCode() {
        int index = codeIndex.getAndIncrement();
        return index < codes.size() ? codes.get(index) : 200;
    }

    @Override
    public CompletableFuture<TelegramSendMessage> sendMessage(
        long chatId, String text, ParseMode parseMode, @Nullable InlineKeyboard inlineKeyboard, long replyMessageId)
    {
        return CompletableFuture.supplyAsync(() -> {
            var msg = new Message(chatId, messageIdSequence.getAndIncrement(), text, parseMode, false);
            msg.keyboard = inlineKeyboard;
            sandbox.add(msg);
            int returnCode = getNextStatusCode();
            countDownLatch.countDown();
            return new TelegramSendMessage(returnCode, msg.getMessageId());
        });
    }

    @Override
    public CompletableFuture<TelegramSendMessage> forceReply(
            long chatId,
            String text,
            ParseMode parseMode,
            long replyMessageId)
    {
        return CompletableFuture.supplyAsync(() -> new TelegramSendMessage(200));
    }

    @Override
    public CompletableFuture<TelegramSendMessage> sendPhoto(
        long chatId,
        byte[] photo,
        String text,
        ParseMode parseMode,
        long replyMessageId,
        InlineKeyboard inlineKeyboard)
    {
        return CompletableFuture.supplyAsync(() -> {
            var msg = new Message(chatId, messageIdSequence.getAndIncrement(), text, parseMode, true);
            msg.keyboard = inlineKeyboard;
            sandbox.add(msg);
            int returnCode = getNextStatusCode();
            countDownLatch.countDown();
            return new TelegramSendMessage(returnCode, msg.getMessageId());
        });
    }

    @Override
    public CompletableFuture<TelegramSendMessage> answerCallbackQuery(
        String callbackId,
        @Nullable String text,
        @Nullable Boolean showAlert,
        @Nullable String url,
        @Nullable Integer cacheTimeSeconds)
    {
        return CompletableFuture.supplyAsync(() -> new TelegramSendMessage(200));
    }

    @Override
    public CompletableFuture<TelegramSendMessage> editMessageReplyMarkup(
            long chatId,
            long messageId,
            InlineKeyboard inlineKeyboard)
    {
        var maybeMessage = sandbox.stream()
                .filter(message -> message.getMessageId() == messageId)
                .findFirst();
        if (maybeMessage.isPresent()) {
            maybeMessage.get().keyboard = inlineKeyboard;
            return CompletableFuture.supplyAsync(() ->
                    new TelegramSendMessage(200, maybeMessage.get().getMessageId()));
        }
        return CompletableFuture.supplyAsync(() ->
                new TelegramSendMessage(400, "no such message"));
    }

    @Override
    public CompletableFuture<TelegramSendMessage> editMessageText(
            long chatId,
            long messageId,
            String text,
            @Nullable ParseMode parseMode)
    {
        var maybeMessage = sandbox.stream()
                .filter(message -> message.getMessageId() == messageId)
                .findFirst();
        if (maybeMessage.isPresent()) {
            maybeMessage.get().text = text;
            return CompletableFuture.supplyAsync(() ->
                    new TelegramSendMessage(200, maybeMessage.get().getMessageId()));
        }
        return CompletableFuture.supplyAsync(() ->
                new TelegramSendMessage(400, "no such message"));
    }

    public TelegramClientStub.Message receiveMessage() {
        return sandbox.poll();
    }

    public int countMessageInSandbox() {
        return sandbox.size();
    }

    public static class Message {

        private final long chatId;
        private final long messageId;
        private String text;
        private final ParseMode parseMode;
        private final boolean isPhoto;
        private InlineKeyboard keyboard;

        public Message(long chatId, long messageId, String text, ParseMode parseMode, boolean isPhoto) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.text = text;
            this.parseMode = parseMode;
            this.isPhoto = isPhoto;
        }

        public long getChatId() {
            return chatId;
        }

        public long getMessageId() {
            return messageId;
        }

        public String getText() {
            return text;
        }

        public ParseMode getParseMode() {
            return parseMode;
        }

        public boolean isPhoto() {
            return isPhoto;
        }
    }

    @Override
    public void close() {
    }

    public void addUpdate(TelegramUpdate update) {
        updates.add(update);
    }

    public void clearUpdates() {
        updates.clear();
    }

    public void addCodeResponse(int code) {
        codes.add(code);
    }

    public void setCountDownLatch(int count) {
        this.countDownLatch = new CountDownLatch(count);
    }

    public void sync() throws InterruptedException {
        countDownLatch.await();
    }
}

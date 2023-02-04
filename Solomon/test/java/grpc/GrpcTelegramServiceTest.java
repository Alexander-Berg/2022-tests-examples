package ru.yandex.solomon.alert.grpc;

import java.util.List;

import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TTelegramGroupTitlesRequest;
import ru.yandex.solomon.alert.protobuf.TTelegramGroupTitlesResponse;


/**
 * @author alexlovkov
 **/
public class GrpcTelegramServiceTest {

    private ChatIdResolverStub chatIdResolver;
    private GrpcTelegramService telegramService;

    @Before
    public void setUp() {
        this.chatIdResolver = new ChatIdResolverStub();
        this.telegramService = new GrpcTelegramService(chatIdResolver);
    }

    @Test(timeout = 5_000)
    public void telegramUsers() {
        chatIdResolver.addGroupTitle(1, "group1");
        chatIdResolver.addGroupTitle(2, "group2");
        chatIdResolver.addTelegramLogin(3, "user1");
        chatIdResolver.addTelegramLogin(4, "user2");
        TTelegramGroupTitlesRequest request = TTelegramGroupTitlesRequest.newBuilder().build();
        MutableBoolean visited = new MutableBoolean(false);
        telegramService.telegramGroupTitles(request,
            new StreamObserver<>() {
                @Override
                public void onNext(TTelegramGroupTitlesResponse response) {
                    List<String> groups = response.getGroupTitleList();
                    Assert.assertEquals(2, groups.size());
                    Assert.assertTrue(groups.contains("group1"));
                    Assert.assertTrue(groups.contains("group2"));
                    Assert.assertEquals("group1", chatIdResolver.resolveGroupTitle(1));
                    Assert.assertEquals("group2", chatIdResolver.resolveGroupTitle(2));
                    visited.setTrue();
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                }
            });
        while (visited.isFalse()) {
            ThreadUtils.sleep(10);
        }
    }
}

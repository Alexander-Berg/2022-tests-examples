package com.yandex.mail.react.model;

import com.yandex.mail.react.ReactTestFactory;
import com.yandex.mail.react.entity.ReactMessage;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesLoadStrategyTest {

    @Test
    public void AddOnlyFirstMessage_shouldAddOnlyFirstMessage() {
        MessagesLoadStrategy.AddOnlyFirstMessage strategy = new MessagesLoadStrategy.AddOnlyFirstMessage();

        List<ReactMessage> messagesWithMetaOnly = ReactTestFactory.buildListOfMessagesWithoutBodies(3);
        Set<Long> localMidsThatNeedFullInfo = new HashSet<>();
        strategy.whatMessagesNeedToBeLoadedFully(messagesWithMetaOnly, localMidsThatNeedFullInfo);

        // Should contain only first message
        assertThat(localMidsThatNeedFullInfo).containsExactly(messagesWithMetaOnly.get(0).messageId());
    }
}

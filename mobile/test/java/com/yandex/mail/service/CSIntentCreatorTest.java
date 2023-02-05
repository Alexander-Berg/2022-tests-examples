package com.yandex.mail.service;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;

import static com.yandex.mail.runners.IntegrationTestRunner.app;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static kotlin.collections.SetsKt.setOf;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(IntegrationTestRunner.class)
public class CSIntentCreatorTest {

    @NonNull
    private CSInputDataCreator inputDataCreator;

    @Before
    public void setUp() throws Exception {
        inputDataCreator = new CSInputDataCreator(app());
    }

    @Test
    public void markReadShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.markRead(1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void markUnreadShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.markUnread(1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void deleteShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.delete(1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void archiveShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.archive(1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void markSpamShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.markSpam(1, 1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void markNotSpamShouldThrowOnEmptyMidsList() {
        assertThatThrownBy(() -> inputDataCreator.markNotSpam(1, 1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithMultiLabelMapEmptyMids() {
        assertThatThrownBy(
                () -> inputDataCreator.multiLabelsMarkOffline(1, singletonMap("1", true), setOf("1"), emptyList())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithMultiLabelMapEmptyLids() {
        assertThatThrownBy(
                () -> inputDataCreator.multiLabelsMarkOffline(1, singletonMap("1", true), emptyList(), setOf(1L))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithMultiLabelOne() {
        assertThatThrownBy(() -> inputDataCreator.multiLabelsMarkOffline(1, setOf(1L), emptyList(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithMultiLabelApiEmptyLids() {
        assertThatThrownBy(() -> inputDataCreator.multiLabelsMarkApi(1, setOf(1L), emptyList(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithMultiLabelApiEmptyMids() {
        assertThatThrownBy(() -> inputDataCreator.multiLabelsMarkApi(1, emptyList(), setOf("1"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMarkWithlabel() {
        assertThatThrownBy(() -> inputDataCreator.markWithLabel(1, "1", true, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }

    @Test
    public void testMoveToFolder() {
        assertThatThrownBy(() -> inputDataCreator.moveToFolder(1, 1, 1, emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a non-empty collection");
    }
}

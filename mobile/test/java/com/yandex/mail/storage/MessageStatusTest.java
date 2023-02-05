package com.yandex.mail.storage;

import com.yandex.mail.storage.MessageStatus.Status;

import org.junit.Test;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageStatusTest {

    @Test
    public void toContentValues() {
        MessageStatus status = MessageStatus.getInstance(true);
        int statusMaskFromCV = status.toStatusMask();
        MessageStatus statusFromCv = MessageStatus.getInstance(statusMaskFromCV);
        assertThat(statusFromCv).isEqualToComparingFieldByField(status);
    }

    @Test
    public void fromStatusMask() {
        int statusMask = 0;
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isUnread()).isFalse();
        assertThat(status.isAnswered()).isFalse();
        assertThat(status.isForwarded()).isFalse();

        statusMask |= MessageStatus.STATUS_UNREAD_FLAG;
        statusMask |= MessageStatus.STATUS_ANSWERED_FLAG;
        statusMask |= MessageStatus.STATUS_FORWARDED_FLAG;

        status = MessageStatus.getInstance(statusMask);
        assertThat(status.isUnread()).isTrue();
        assertThat(status.isAnswered()).isTrue();
        assertThat(status.isForwarded()).isTrue();
    }

    @Test
    public void readFlag_shouldNotModifyStatus() {
        int statusMask = 0;
        int statusAfterMarkAsRead = statusMask | MessageStatus.STATUS_READ_FLAG;
        assertThat(statusAfterMarkAsRead).isEqualTo(statusMask);
    }

    @Test
    public void isUnread_shouldBeSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.UNREAD, Status.ANSWERED, Status.FORWARDED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isUnread()).isTrue();
    }

    @Test
    public void isUnread_shouldNotBeSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.READ, Status.ANSWERED, Status.FORWARDED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isUnread()).isFalse();
    }

    @Test
    public void isAnswered_shouldBeSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.UNREAD, Status.ANSWERED, Status.FORWARDED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isAnswered()).isTrue();
    }

    @Test
    public void isAnswered_shouldBeNotSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.UNREAD, Status.FORWARDED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isAnswered()).isFalse();
    }

    @Test
    public void isForwarded_shouldBeSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.UNREAD, Status.ANSWERED, Status.FORWARDED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isForwarded()).isTrue();
    }

    @Test
    public void isForwarded_shouldNotBeSet() {
        int statusMask = fromStatusMask(generateStatusesList(Status.UNREAD, Status.ANSWERED));
        MessageStatus status = MessageStatus.getInstance(statusMask);
        assertThat(status.isForwarded()).isFalse();
    }

    @NonNull
    private List<Status> generateStatusesList(@NonNull Status... statuses) {
        return CollectionsKt.listOf(statuses);
    }

    private int fromStatusMask(List<Status> statuses) {
        int result = 0;
        for (Status status : statuses) {
            if (status != null) {
                result = result | status.getFlag();
            }
        }
        return result;
    }
}

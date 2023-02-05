package com.yandex.mail.fakeserver;

import com.yandex.mail.wrappers.AttachmentWrapper;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Contains links to all user's attachments
 */
public final class Attachments {

    private long currentId = 0;

    @NonNull
    private final Map<String, AttachmentWrapper> map;

    public Attachments() {
        map = new HashMap<>();
    }

    @NonNull
    private String nextAttachmentId() {
        String ret = "fake_attachment_" + currentId;
        currentId++;
        return ret;
    }

    @NonNull
    public AttachmentWrapper getById(@NonNull String attachmentId) {
        return map.get(attachmentId);
    }

    @NonNull
    public String add(@NonNull AttachmentWrapper attachment) {
        String id = nextAttachmentId();
        map.put(id, attachment);
        return id;
    }
}

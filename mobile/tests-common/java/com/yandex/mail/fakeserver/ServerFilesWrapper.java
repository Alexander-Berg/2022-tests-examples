package com.yandex.mail.fakeserver;

import android.net.Uri;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.yandex.mail.tools.MockNetworkTools.getNotFoundResponse;
import static com.yandex.mail.tools.MockNetworkTools.getOkResponse;

public class ServerFilesWrapper {

    /**
     * Maps from path to the contents
     */
    @NonNull
    private Map<String, byte[]> contents = new HashMap<>();

    public void addFile(@NonNull String path, @NonNull byte[] content) {
        contents.put(path, content);
    }

    public void addFile(@NonNull String path, @NonNull String content) {
        addFile(normalize(path), content.getBytes());
    }

    public void addAttachment(@NonNull String name, @NonNull byte[] content) {
        addFile(getAttachmentPath(name), content);
    }

    public void addAvatarFor(@NonNull String email) {
        addFile(getAvatarPath(email), email.getBytes());
    }

    @NonNull
    public String getAttachmentPath(@NonNull String name) {
        return "attachment/" + name;
    }

    @NonNull
    public String getAvatarPath(@NonNull String email) {
        return "avatars/" + email;
    }

    @NonNull
    public byte[] getContent(@NonNull String path) {
        return contents.get(path);
    }

    public void reset() {
        contents.clear();
    }

    /**
     * handles binary files
     */
    @NonNull
    public MockWebServerResponseRule getFileResponseRule() {
        return request -> {
            final Uri pathUri = Uri.parse(request.getPath());
            final List<String> segments = pathUri.getPathSegments();
            final String first = segments.get(0);
            String path = pathUri.getPath();
            if ("avatars".equals(first)) {
                // meh, hacky. Cutoff '/large'
                path = path.substring(0, path.length() - 6);
            } else {
                // don't do anything, we're fine
            }
            final byte[] content = contents.get(normalize(path));
            if (content == null) {
                return getNotFoundResponse();
            } else {
                return getOkResponse(content, false);
            }
        };
    }

    @NonNull
    private static String normalize(@NonNull String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }
}

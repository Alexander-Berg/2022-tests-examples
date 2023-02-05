package com.yandex.mail.tools;

import com.yandex.mail.network.json.response.StatusWrapper;
import com.yandex.mail.network.response.Header;
import com.yandex.mail.network.response.Status;
import com.yandex.mail.util.NonInstantiableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import kotlin.Pair;
import okhttp3.mockwebserver.RecordedRequest;

public final class MockServerTools {
    private MockServerTools() {
        throw new NonInstantiableException();
    }

    @NonNull
    public static Status createOkStatus() {
        return Status.create(Status.STATUS_OK, null, null, null);
    }

    @NonNull
    public static Status createTempErrorStatus() {
        return Status.create(Status.STATUS_TEMP_ERROR, null, null, null);
    }

    @NonNull
    public static Status createPermErrorStatus() {
        return Status.create(Status.STATUS_PERM_ERROR, null, null, null);
    }

    @NonNull
    public static Status createAuthErrorStatus() {
        return Status.create(Header.HEADER_AUTH_ERR_CODE, null, null, null);
    }

    @NonNull
    public static StatusWrapper createOkStatusWrapper() {
        StatusWrapper wrapper = new StatusWrapper();
        wrapper.setStatus(StatusWrapper.Status.OK);
        return wrapper;
    }

    private static boolean rangeEquals(@NonNull byte[] source, int from, @NonNull byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (source[from + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private static byte[] extractContent(@NonNull byte[] bytes) {
        byte[] separator = {'\r', '\n', '\r', '\n'};
        // two line breaks separate headers from content
        for (int i = 0; i < bytes.length; i++) {
            if (rangeEquals(bytes, i, separator)) {
                return Arrays.copyOfRange(bytes, i + separator.length, bytes.length - 2); // 2 for '\r\n'
            }
        }
        throw new RuntimeException("Couln't find content!");
    }

    /**
     * Not really efficient, but suits our needs at the moment
     */
    @NonNull
    private static List<byte[]> splitBytes(@NonNull byte[] bytes, @NonNull byte[] separator) {
        List<byte[]> result = new ArrayList<>();
        int start = 0;
        for (int pos = 0; pos < bytes.length - separator.length; ) {
            if (rangeEquals(bytes, pos, separator)) {
                result.add(Arrays.copyOfRange(bytes, start, pos));
                start = pos + separator.length;
                pos += separator.length;
            } else {
                pos++;
            }
        }
        result.add(Arrays.copyOfRange(bytes, start, bytes.length));
        return result;
    }

    /**
     * See for the format specification https://en.wikipedia.org/wiki/MIME#Multipart_messages
     *
     * @param request multipart POST request
     * @return filename and content extracted from the request
     */
    @NonNull
    public static Pair<String, byte[]> extractAttachmentFromMultipart(@NonNull RecordedRequest request) {
        String header = request.getHeader("Content-Type");
        Matcher matcher = Pattern.compile(".*boundary=([-\\w]+)").matcher(header);
        matcher.find();
        String boundary = matcher.group(1);

        byte[] body = request.getBody().readByteArray();
        List<byte[]> parts = splitBytes(body, ("--" + boundary).getBytes());

        // first and last parts are just leftovers from splitting
        // second part contains filename
        // remaining parts contain body

        String filename = new String(extractContent(parts.get(1)));

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (int i = 2; i < parts.size() - 1; i++) {
                bos.write(extractContent(parts.get(i)));
            }
            return new Pair<>(filename, bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

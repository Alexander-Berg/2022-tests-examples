package com.yandex.mail.util;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.yandex.mail.network.response.GsonTest;
import com.yandex.mail.react.entity.Avatar;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public final class IOUtilTest extends GsonTest {

    @Test
    public void fileByContentUriExistsShouldReturnTrue() throws IOException {
        ContentResolver contentResolver = mock(ContentResolver.class);
        Uri uri = mock(Uri.class);

        ParcelFileDescriptor fileDescriptor = mock(ParcelFileDescriptor.class);

        when(contentResolver.openFileDescriptor(eq(uri), eq("r")))
                .thenReturn(fileDescriptor);

        when(fileDescriptor.getFileDescriptor())
                .thenReturn(new FileDescriptor());

        assertThat(IOUtil.fileByContentUriExists(contentResolver, uri)).isTrue();

        verify(fileDescriptor).close();
    }

    @Test
    public void fileByContentUriExistsShouldReturnFalse() throws FileNotFoundException {
        ContentResolver contentResolver = mock(ContentResolver.class);
        Uri uri = mock(Uri.class);

        when(contentResolver.openFileDescriptor(eq(uri), eq("r")))
                .thenThrow(new FileNotFoundException("test exception"));

        assertThat(IOUtil.fileByContentUriExists(contentResolver, uri)).isFalse();
    }

    @Test
    public void escapeCharsInJsonForJsonpSupport_shouldEscapeU2028AndU2029() {
        final Avatar avatar = new Avatar.Builder().monogram("Yo \u2028 \u2029").imageUrl(null).build();
        final String json = gson.toJson(avatar);
        assertThat(json).isEqualTo("{\"type\":\"monogram\",\"monogram\":\"Yo \\u2028 \\u2029\"}");

        String input = "{ \"someField\": \"Yo \u2028 \u2029 \"}";
        String output = IOUtil.escapeCharsInJsonForJsonpSupport(input);
        assertThat(output).isEqualTo("{ \"someField\": \"Yo \\u2028 \\u2029 \"}");

        String input2 = json;
        String output2 = IOUtil.escapeCharsInJsonForJsonpSupport(input2);
        assertThat(output2).isEqualTo(input2);
    }
}

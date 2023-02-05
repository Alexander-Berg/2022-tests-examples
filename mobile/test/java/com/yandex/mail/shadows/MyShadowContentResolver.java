package com.yandex.mail.shadows;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.FileNotFoundException;

import static android.os.ParcelFileDescriptor.MODE_READ_WRITE;

/**
 * Implemented to overcome various difficulties in handling ParcelFileDescriptor
 * In particular, it seems impossible to shadow deparcelization due to static ParcelFileDescriptor.CREATOR field.
 * Handling FileDescriptors is also difficult, they are way too abstract.
 */
@SuppressWarnings("unused")
@Implements(ContentResolver.class)
public class MyShadowContentResolver extends ShadowContentResolver {

    @Implementation
    public ParcelFileDescriptor openFileDescriptor(
            Uri uri,
            String mode) throws FileNotFoundException {
        ContentProvider provider = getProvider(uri);
        if (provider == null && ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return ParcelFileDescriptor.open(new File(uri.getPath()), MODE_READ_WRITE);
        }
        return provider.openFile(uri, mode);
    }

    @Implementation
    public ParcelFileDescriptor openFileDescriptor(
            Uri uri,
            String mode,
            CancellationSignal cancellationSignal) throws FileNotFoundException {
        return openFileDescriptor(uri, mode);
    }

    @Implementation
    public static boolean isSyncPending(Account account, String authority) {
        return false;
    }

    @Implementation
    public static boolean isSyncActive(Account account, String authority) {
        return false;
    }
}

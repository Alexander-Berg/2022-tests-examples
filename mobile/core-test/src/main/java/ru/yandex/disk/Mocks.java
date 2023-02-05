package ru.yandex.disk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.provider.MediaStore;
import org.robolectric.android.controller.ContentProviderController;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.provider.DH;
import ru.yandex.disk.provider.DiskContentProvider;
import ru.yandex.disk.provider.DiskUriProcessorMatcher;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class Mocks {

    public static AccountManager mockAccountManager() {
        AccountManager am = mock(AccountManager.class);
        when(am.getAccountsByType(nullable(String.class))).thenReturn(new Account[0]);
        when(am.getAuthenticatorTypes()).thenReturn(new AuthenticatorDescription[0]);
        return am;
    }

    public static void addContentProviders(SeclusiveContext context) {
        ContentResolver cr = context.getContentResolver();
        final String providerAuthority = DiskContentProvider.getAuthority(context);
        ContentProviderClient cp = cr.acquireContentProviderClient(providerAuthority);
        if (cp != null) {
            cp.release();
        } else {
            context.setActivityManager(mockActivityManager());
            final DiskContentProvider contentProvider = createDiskContentProvider(context, new DH(context));
            addContentProvider(context, contentProvider, providerAuthority);
        }
    }

    @Nonnull
    public static DiskContentProvider createDiskContentProvider(SeclusiveContext context, DH db) {
        final DiskUriProcessorMatcher matcher =
                TestObjectsFactory.createDiskUriProcessorMatcher(context, db);

        InjectionUtils.setUpInjectionServiceForDiskContentProvider(matcher);

        return new DiskContentProvider();
    }

    public static void addContentProvider(Context context, ContentProvider cp, String authority) {
        cp.attachInfo(context, null);
        ContentProviderController.of(cp).create(authority);
    }

    public static ActivityManager mockActivityManager() {
        ActivityManager am = mock(ActivityManager.class);
        List<RunningAppProcessInfo> emptyList = Collections.emptyList();
        when(am.getRunningAppProcesses()).thenReturn(emptyList);
        return am;
    }

    public static CredentialsManager initCredentials(SeclusiveContext context) {
        addContentProviders(context);
        return new CredentialsManagerWithUser("name");
    }

    public static void addSystemSettingsContentProvider(final SeclusiveContext context) {
        addRealContentProvider(context, android.provider.Settings.System.CONTENT_URI);
    }

    public static void addMediaStoreContentProvider(final SeclusiveContext context) {
        addRealContentProvider(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private static void addRealContentProvider(SeclusiveContext context, Uri uri) {
        throw new UnsupportedOperationException();
    }

    public static WifiManager mockWifiManager() {
        WifiManager wm = mock(WifiManager.class);
        when(wm.createWifiLock(anyInt(), anyString())).thenReturn(mock(WifiLock.class));
        return wm;
    }

    public static PackageManager mockPackageManager() throws NameNotFoundException {
        PackageManager pm = mock(PackageManager.class);
        when(pm.getApplicationInfo(anyString(), anyInt())).thenReturn(new ApplicationInfo());
        return pm;
    }

    public static ConnectivityManager mockConnectivityManager() {
        ConnectivityManager cm = mock(ConnectivityManager.class);
        NetworkInfo networkInfo = mock(NetworkInfo.class);
        when(networkInfo.getTypeName()).thenReturn("WIFI");
        when(networkInfo.isConnected()).thenReturn(true);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(cm.getAllNetworkInfo()).thenReturn(new NetworkInfo[]{networkInfo});
        when(cm.getActiveNetworkInfo()).thenReturn(networkInfo);
        return cm;
    }
}

package ru.yandex.disk.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.test.AndroidTestCase2;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.DiskContract.*;
import static ru.yandex.disk.provider.DiskContract.ServiceInfo.Manifest.ENABLED_META_NAME;

@Config(manifest = Config.NONE)
public class DiskServicesScannerTest extends AndroidTestCase2 {

    private static final String DEFAULT_USER_NAME = "feelgood";
    private static final String SELF_APP_ID = "ru.yandex.client.disk";
    private final DiskServiceInfo selfInfo = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
    private DiskServicesScanner diskServicesScanner;
    private PackageManager pm;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        when(context.getPackageName()).thenReturn(SELF_APP_ID);

        final Credentials creds = mock(Credentials.class);
        when(creds.getUser()).thenReturn(DEFAULT_USER_NAME);
        final CredentialsManager cm = mock(CredentialsManager.class);
        when(cm.getActiveAccountCredentials()).thenReturn(creds);

        pm = mock(PackageManager.class);
        diskServicesScanner = spy(new DiskServicesScanner(null, pm, context, cm));

    }

    @Test
    public void mustNotIncludeSelfInNeighborsList() throws Exception {
        doReturn(asList(
                new DiskServiceInfo(3, DEFAULT_USER_NAME, "ru.yandex.minidisk.1"),
                new DiskServiceInfo(3, DEFAULT_USER_NAME, "ru.yandex.minidisk.2"),
                selfInfo)).when(diskServicesScanner).scanAll();

        final List<DiskServiceInfo> neighbors = diskServicesScanner.getPrioritizedNeighborsByVersion();
        assertThat(neighbors, not(hasItem(selfInfo)));
        assertThat(neighbors.size(), equalTo(2));
    }

    @Test
    public void mustPrioritizeNeighborsByVersion() throws Exception {

        final DiskServiceInfo neighbor1 = new DiskServiceInfo(1, "2", "ru.yandex.minidisk.3");
        final DiskServiceInfo neighbor2 = new DiskServiceInfo(2, "3", "ru.yandex.minidisk.2");
        final DiskServiceInfo neighbor3 = new DiskServiceInfo(3, DEFAULT_USER_NAME, "ru.yandex.minidisk.4");

        doReturn(asList(neighbor1, neighbor2, neighbor3))
                .when(diskServicesScanner).scanAll();

        final List<DiskServiceInfo> neighbors = diskServicesScanner.getPrioritizedNeighborsByVersion();
        assertThat(neighbors, contains(neighbor3, neighbor2, neighbor1));
        assertThat(neighbors.size(), equalTo(3));
    }

    @Test
    public void mustPrioritizeNeighborsAlphabeticallyIfVersionAreTheSame() throws Exception {
        final DiskServiceInfo neighbor1 = new DiskServiceInfo(1, "2", "ru.yandex.minidisk.c");
        final DiskServiceInfo neighbor2 = new DiskServiceInfo(1, "3", "ru.yandex.minidisk.a");
        final DiskServiceInfo neighbor3 = new DiskServiceInfo(1, "1", "ru.yandex.minidisk.b");

        doReturn(asList(neighbor1, neighbor2, neighbor3))
                .when(diskServicesScanner).scanAll();

        final List<DiskServiceInfo> neighbors = diskServicesScanner.getPrioritizedNeighborsByVersion();
        assertThat(neighbors, contains(neighbor2, neighbor3, neighbor1));
    }

    @Test
    public void mustSortByVersionAndAlphabeticallyRight() throws Exception {
        final DiskServiceInfo neighbor1 = new DiskServiceInfo(2, "2", "ru.yandex.minidisk.c");
        final DiskServiceInfo neighbor2 = new DiskServiceInfo(1, "3", "ru.yandex.minidisk.a");
        final DiskServiceInfo neighbor3 = new DiskServiceInfo(1, "1", "ru.yandex.minidisk.b");

        doReturn(asList(neighbor1, neighbor2, neighbor3))
                .when(diskServicesScanner).scanAll();

        final List<DiskServiceInfo> neighbors = diskServicesScanner.getPrioritizedNeighborsByVersion();
        assertThat(neighbors, contains(neighbor1, neighbor2, neighbor3));
    }

    @Test
    public void mustBeDisabledIfNotExported() throws Exception {
        final ServiceInfo info = new ServiceInfo();
        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = false;
        info.metaData = getMetaData(true);

        assertThat(diskServicesScanner.isServiceEnabled(info), equalTo(false));
    }

    @Test
    public void mustBeDisabledIfNotEnabled() throws Exception {
        final ServiceInfo info = new ServiceInfo();
        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = true;
        info.metaData = getMetaData(false);

        assertThat(diskServicesScanner.isServiceEnabled(info), equalTo(false));
    }

    @Test
    public void mustBeDisabledIfNoMetadata() throws Exception {
        final ServiceInfo info = new ServiceInfo();
        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = true;
        info.metaData = null;

        assertThat(diskServicesScanner.isServiceEnabled(info), equalTo(false));
    }

    @Test
    @SuppressWarnings("ResourceType") // for eq(PackageManager.GET_META_DATA)
    public void mustFilterNeighborsByPermission() throws Exception {

        final ResolveInfo selfInfo
                = createServiceResolveInfo(DISK_SERVICE_PERMISSION_NAME, SELF_APP_ID);
        final ResolveInfo wrongPermissionNeighborInfo
                = createServiceResolveInfo(DISK_SERVICE_PERMISSION_NAME + "_DEBUG", "ru.yandex.disk.a");

        final List<ResolveInfo> resolveInfos = asList(selfInfo, wrongPermissionNeighborInfo);
        when(pm.queryIntentServices(any(), eq(PackageManager.GET_META_DATA)))
                .thenReturn(resolveInfos);

        doReturn(DEFAULT_USER_NAME)
                .when(diskServicesScanner).getLoggedInUser(any());

        final List<DiskServiceInfo> result = diskServicesScanner.scanAll();
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).getAppName(), equalTo(SELF_APP_ID));
    }

    @Test
    public void mustFilterServicesWithDisabledDiskContentProvider() throws Exception {
        mockProviderStates(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

        final List<DiskServiceInfo> result = diskServicesScanner.scanAll();
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).getAppName(), equalTo(SELF_APP_ID));
    }

    @Test
    public void mustNotFilterServicesWithEnabledDiskContentProvider() throws Exception {
        mockProviderStates(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        final List<DiskServiceInfo> result = diskServicesScanner.scanAll();
        assertThat(result.size(), equalTo(2));
    }

    @Test
    public void mustNotFilterServicesWithDefaulDiskContentProviderState() throws Exception {
        mockProviderStates(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

        final List<DiskServiceInfo> result = diskServicesScanner.scanAll();
        assertThat(result.size(), equalTo(2));
    }

    @SuppressWarnings("ResourceType")
    private void mockProviderStates(final int selfProviderState, final int neighborProviderState) {
        final String neighborPackageName = "ru.yandex.searchplugin";
        final List<ResolveInfo> resolveInfos = asList(
                createServiceResolveInfo(DISK_SERVICE_PERMISSION_NAME, SELF_APP_ID),
                createServiceResolveInfo(DISK_SERVICE_PERMISSION_NAME, neighborPackageName)
        );

        doReturn(DEFAULT_USER_NAME)
                .when(diskServicesScanner).getLoggedInUser(any());

        when(pm.queryIntentServices(any(), eq(PackageManager.GET_META_DATA)))
                .thenReturn(resolveInfos);

        final ComponentName selfProviderComponentName
                = new ComponentName(context, SELF_APP_ID + YA_DISK_CONTENT_PROVIDER_CLASS_NAME_POSTFIX);
        final ComponentName neighborProviderComponentName
                = new ComponentName(context, neighborPackageName + YA_DISK_CONTENT_PROVIDER_CLASS_NAME_POSTFIX);
        when(pm.getComponentEnabledSetting(eq(selfProviderComponentName)))
                .thenReturn(selfProviderState);
        when(pm.getComponentEnabledSetting(eq(neighborProviderComponentName)))
                .thenReturn(neighborProviderState);
    }

    private ResolveInfo createServiceResolveInfo(final String permissionName, final String packageName) {
        final ResolveInfo result = new ResolveInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.permission = permissionName;
        serviceInfo.exported = true;
        serviceInfo.metaData = getMetaData(true);
        serviceInfo.packageName = packageName;
        result.serviceInfo = serviceInfo;
        return result;
    }

    private Bundle getMetaData(final boolean enabled) {
        final Bundle metaData = new Bundle();
        metaData.putBoolean(ENABLED_META_NAME, enabled);
        return metaData;
    }
}
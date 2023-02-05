package ru.yandex.disk.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.autoupload.AutouploadCheckDebouncer;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.IncidentContentResolver;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.test.AndroidTestCase2;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.DiskContract.*;

@Config(manifest = Config.NONE)
public class DiskServicesAnalyzerTest extends AndroidTestCase2 {

    private static final String DEFAULT_USER_NAME = "testuser";
    private static final String SELF_APP_ID = "ru.yandex.disk.mini_sample";
    private final DiskServiceInfo selfInfo = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
    private DiskServicesScanner diskServicesScanner;
    private Context context;
    private DiskServicesAnalyzer diskServicesAnalyzer;
    private PackageManager pm;
    private IncidentContentResolver cr;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        when(context.getPackageName()).thenReturn(SELF_APP_ID);

        pm = mock(PackageManager.class);
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.services = new ServiceInfo[0];
        packageInfo.applicationInfo = new ApplicationInfo();
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, PackageManager.GET_SERVICES))
                .thenReturn(packageInfo);
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, 0))
                .thenReturn(packageInfo);
        PackageInfo betaPackage = new PackageInfo();
        betaPackage.services = new ServiceInfo[0];
        when(pm.getPackageInfo(BETA_DISK_APP_ID, PackageManager.GET_SERVICES))
                .thenReturn(betaPackage);

        cr = mock(IncidentContentResolver.class);
        diskServicesScanner = mock(DiskServicesScanner.class);
        final CredentialsManager credentialsManager =
                new CredentialsManagerWithUser(DEFAULT_USER_NAME);
        diskServicesAnalyzer = new DiskServicesAnalyzer(diskServicesScanner, credentialsManager, pm, cr,
                 mock(CommandStarter.class), mock(AutouploadCheckDebouncer.class));
        when(diskServicesScanner.getSelfInfo()).thenReturn(new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID));
        when(diskServicesScanner.scanAllServices()).thenReturn(singletonList(selfInfo));
    }

    @Test
    public void mustFindOldDiskIfInstalledButNotHaveService() throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.services = new ServiceInfo[0];
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, PackageManager.GET_SERVICES))
                .thenReturn(packageInfo);
        assertThat(diskServicesAnalyzer.isOldDiskInstalled(), equalTo(true));
    }

    @Test
    public void mustFindOldDiskIfInstalledWithServiceButNotEnabled() throws Exception {
        final PackageInfo packageInfo = new PackageInfo();
        final ServiceInfo info = new ServiceInfo();

        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = true;

        packageInfo.services = new ServiceInfo[]{info};
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, PackageManager.GET_SERVICES))
                .thenReturn(packageInfo);
        assertThat(diskServicesAnalyzer.isOldDiskInstalled(), equalTo(true));
    }

    @Test
    public void mustNotFoundOldDiskIfInstalledAndHaveServiceEnabled() throws Exception {
        final ServiceInfo info = new ServiceInfo();

        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = true;
        info.metaData = getMetaData();

        final ComponentName componentName = new ComponentName(DiskContract.MAIN_DISK_APP_ID, "ru.yandex.disk.service.DiskService");
        when(pm.getServiceInfo(componentName, PackageManager.GET_META_DATA))
                .thenReturn(info);
        when(diskServicesScanner.isServiceEnabled(info)).thenReturn(true);
        assertThat(diskServicesAnalyzer.isOldDiskInstalled(), equalTo(false));
    }

    @Test
    public void mustFoundOldDiscIfInstalledAndServiceIsNotExported() throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        final ServiceInfo info = new ServiceInfo();
        info.name = "ru.yandex.disk.service.DiskService";
        info.exported = false;

        packageInfo.services = new ServiceInfo[]{info};
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, PackageManager.GET_SERVICES))
                .thenReturn(packageInfo);
        assertThat(diskServicesAnalyzer.isOldDiskInstalled(), equalTo(true));
    }

    @Test
    public void mustNotFoundOldDiskIfNotInstalled() throws PackageManager.NameNotFoundException {
        when(pm.getPackageInfo(MAIN_DISK_APP_ID, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        assertThat(diskServicesAnalyzer.isOldDiskInstalled(), equalTo(false));
    }

    @Test
    public void mustBeMasterIfAlone() {
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(selfInfo));
    }

    @Test
    public void mustBeMasterIfHaveGreatestVersion() {
        final DiskServiceInfo master = new DiskServiceInfo(3, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices())
                .thenReturn(asList(new DiskServiceInfo(1, DEFAULT_USER_NAME, "ru.yandex.minidisk.1"),
                        new DiskServiceInfo(2, DEFAULT_USER_NAME, "ru.yandex.minidisk.2"),
                        new DiskServiceInfo(2, DEFAULT_USER_NAME, "ru.yandex.minidisk.3"),
                        master));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustNotBeMasterIfNotGreatestVersion() {
        final DiskServiceInfo master = new DiskServiceInfo(3, DEFAULT_USER_NAME, "ru.yandex.minidisk.3");
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(
                new DiskServiceInfo(1, DEFAULT_USER_NAME, "ru.yandex.minidisk.1"),
                new DiskServiceInfo(2, DEFAULT_USER_NAME, "ru.yandex.minidisk.2"),
                master,
                new DiskServiceInfo(2, DEFAULT_USER_NAME, SELF_APP_ID)
        ));

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustBeMasterIfSameVersionsButAlfabeticallyFirst() {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(
                new DiskServiceInfo(1, DEFAULT_USER_NAME, "ru.yandex.disk.zdisk"),
                master));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustNotBeMasterIfSameVersionsButAlfabeticallyNotFirst() {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, "ru.yandex.client.adisk");
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID)));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustFilterAnotherUsers() {
        final DiskServiceInfo master = new DiskServiceInfo(2, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices())
                .thenReturn(asList(new DiskServiceInfo(2, "3", "ru.yandex.minidisk.2"),
                        new DiskServiceInfo(3, "2", "ru.yandex.minidisk.3"),
                        new DiskServiceInfo(3, "2", "ru.yandex.minidisk.4"),
                        master));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustHandleNullUid() {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(
                new DiskServiceInfo(0, null, "ru.yandex.minidisk.1"),
                master));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustBeEnabledIfMainDisk() {
        final String tempSelfId = MAIN_DISK_APP_ID;
        when(context.getPackageName()).thenReturn(tempSelfId);

        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, tempSelfId);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(2, DEFAULT_USER_NAME, "ru.yandex.client.test")));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustNotBeMasterIfMainDiskInstalledAndSameUserIgnoreVersion() {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, MAIN_DISK_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(2, DEFAULT_USER_NAME, SELF_APP_ID)));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustBeMasterIfMainDiskInstalledButWithAnotherUser() throws Exception {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(1, "another_user", MAIN_DISK_APP_ID)));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustPickMainDiskIfNoSelfCreds() throws Exception {
        makeSelfCredsNull();

        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, MAIN_DISK_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(2, null, SELF_APP_ID),
                new DiskServiceInfo(3, null, "ru.yandex.disk.sample")));

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustPickBetaDiskIfNoSelfCreds() throws Exception {
        makeSelfCredsNull();

        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, BETA_DISK_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(
                asList(master,
                        new DiskServiceInfo(2, null, SELF_APP_ID),
                        new DiskServiceInfo(3, null, "ru.yandex.disk.sample"),
                        new DiskServiceInfo(4, null, MAIN_DISK_APP_ID)
                ));

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustPickHigherVersionIfNoSelfCreds() throws Exception {
        makeSelfCredsNull();

        final DiskServiceInfo master = new DiskServiceInfo(3, null, "ru.yandex.disk.sample");
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(2, null, "ru.yandex.disk.another.sample"),
                new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID)));

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustBeMasterIfNoCredsAndNoOneAround() throws Exception {
        makeSelfCredsNull();

        final DiskServiceInfo self = new DiskServiceInfo(1, null, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(singletonList(self));
        when(diskServicesScanner.getSelfInfo()).thenReturn(self);

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(self));
    }

    @Test
    public void mustBeMasterIfMainDiskNotLoggedIn() {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(master,
                new DiskServiceInfo(1, null, MAIN_DISK_APP_ID)));

        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    @Test
    public void mustBeMasterWithDiskNeighborWithAnotherAccount() throws Exception {
        final DiskServiceInfo master = new DiskServiceInfo(1, DEFAULT_USER_NAME, SELF_APP_ID);
        when(diskServicesScanner.scanAllServices()).thenReturn(asList(new DiskServiceInfo(1, "123", MAIN_DISK_APP_ID), master));
        assertThat(diskServicesAnalyzer.getMaster(), equalTo(master));
    }

    private void makeSelfCredsNull() {
        final CredentialsManager credentialsManager = mock(CredentialsManager.class);
        when(credentialsManager.getActiveAccountCredentials()).thenReturn(null);
        diskServicesAnalyzer = new DiskServicesAnalyzer(diskServicesScanner, credentialsManager, pm, cr,
                 mock(CommandStarter.class), mock(AutouploadCheckDebouncer.class));
    }

    private Bundle getMetaData() {
        final Bundle metaData = new Bundle();
        metaData.putBoolean("ru.yandex.disk.DISK_SERVICE_ENABLED", true);
        return metaData;
    }
}

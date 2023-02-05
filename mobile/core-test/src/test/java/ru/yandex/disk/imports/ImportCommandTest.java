package ru.yandex.disk.imports;

import android.content.Context;
import android.net.Uri;
import org.junit.Test;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.Event;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.upload.StorageListProviderStub;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.FakeDiagnostics;
import ru.yandex.util.Path;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static ru.yandex.disk.mocks.Stubber.stub;

public class ImportCommandTest extends AndroidTestCase2 {

    @Test
    public void shouldNotCrashOnSecurityExceptionFromGmail() {
        EventLogger eventSender = new EventLogger();
        CloudProviderClient cloudProviderClient = new ExpiredCloudProviderClient();
        Context context = getMockContext();
        final CommandLogger commandLogger = new CommandLogger();
        final ApplicationStorage applicationStorage = TestObjectsFactory.createApplicationStorage(
            context,
            stub(ApplicationSettings.class),
            stub(CredentialsManager.class),
            commandLogger,
            new StorageListProviderStub(),
            mock(Diagnostics.class)
        );

        final ImportCommand command = new ImportCommand(
            context,
            eventSender,
            mock(ImportingFilesStorage.class),
            cloudProviderClient,
            new FakeDiagnostics(),
            applicationStorage
        );
        List<Uri> gmailUri = Collections.singletonList(Uri.parse("content://gmail-ls/1"));
        Path destinationDirectory = DiskDatabase.ROOT_PATH;

        command.execute(new ImportCommandRequest(gmailUri, destinationDirectory));

        Event event = eventSender.get(0);
        assertThat(event, instanceOf(DiskEvents.PrepareUploadFinished.class));
        assertThat(((DiskEvents.PrepareUploadFinished) event).hasErrors(), is(true));
    }

}

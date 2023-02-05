package ru.yandex.disk.provider;

import android.net.Uri;
import ru.yandex.disk.InjectionUtils;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;

import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;

public abstract class DiskContentProviderTest extends ProviderTestCase3<DiskContentProvider> {

    protected DiskDatabase diskDatabase;

    protected Uri invitesUri;

    public DiskContentProviderTest() {
        super(DiskContentProvider.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final SeclusiveContext context = getMockContext();
        context.setActivityManager(Mocks.mockActivityManager());
        final DH db = new DH(context);
        db.addDatabaseOpenListener(new InvitesSchemeCreator());
        invitesUri = Uri.parse(CONTENT + mockAuthority + "/" + DiskContract.Invites.INVITES_AUTHORITY);
        diskDatabase = TestObjectsFactory.createDiskDatabase(db, null, null);
        final DiskUriProcessorMatcher matcher
                = TestObjectsFactory.createDiskUriProcessorMatcher(context, db);

        InjectionUtils.setUpInjectionServiceForDiskContentProvider(matcher);
    }
}

package ru.yandex.disk.provider;

import android.content.ContentResolver;
import android.net.Uri;
import org.junit.Test;
import ru.yandex.disk.provider.DiskContract.DiskFileAndQueue;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;

public class DiskViewsUriProcessorTest extends DiskContentProviderTest {

    @Test
    public void shouldSkipUncompleted() throws Exception {
        DiskItemBuilder builder = new DiskItemBuilder();
        diskDatabase.updateOrInsert(builder.setPath("/disk/a").build());
        diskDatabase.updateOrInsertUncompleted(builder.setPath("/disk/b").build());

        try (DiskFileCursor c = queryAll()) {
            assertThat(c.getCount(), equalTo(1));
            assertThat(c.get(0).getPath(), equalTo("/disk/a"));
        }
    }

    @Test
    public void shouldNotChangeCompletedState() throws Exception {
        DiskItemBuilder builder = new DiskItemBuilder();
        diskDatabase.updateOrInsertUncompleted(builder.setPath("/disk/a").build());
        diskDatabase.updateOrInsert(builder.setPath("/disk/a").build());

        diskDatabase.updateOrInsert(builder.setPath("/disk/b").build());
        diskDatabase.updateOrInsertUncompleted(builder.setPath("/disk/b").build());

        try (DiskFileCursor c = queryAll()) {
            assertThat(c.getCount(), equalTo(2));
        }
    }

    private DiskFileCursor queryAll() {
        final ContentResolver cr = getContentResolver();
        final Uri uri = Uri.parse(CONTENT + mockAuthority + "/" + DiskFileAndQueue.AUTHORITY);
        return new DiskFileCursor(cr.query(uri, null, null, null, null));
    }

}

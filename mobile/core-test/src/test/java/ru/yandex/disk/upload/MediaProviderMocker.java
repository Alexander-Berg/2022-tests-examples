package ru.yandex.disk.upload;

import android.content.ContentProvider;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;
import ru.yandex.util.Path;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class MediaProviderMocker {

    private final ContentProvider mockMediaProvider;

    public MediaProviderMocker(DiskUploaderTestHelper helper) {
        mockMediaProvider = helper.getMockMediaProvider();
    }

    public void whenQueryImagesThenReturn(MediaStoreRecord record) {
        whenQueryThenReturn(record, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private void whenQueryThenReturn(MediaStoreRecord record, Uri uri) {
        MatrixCursor cursor = new MatrixCursor(DiskUploaderTestHelper.MEDIASTORE_PROJ);
        cursor.addRow(new Object[]{
            record.path.getPath(),
            record.mimeType,
            record.dateTaken,
            record.dateAdded
        });
        when(mockMediaProvider.query(eq(uri), nullable(String[].class), nullable(String.class),
                nullable(String[].class), nullable(String.class))).thenReturn(cursor);
    }

    public static class MediaStoreRecord {

        private final Path path;
        private final long dateTaken;
        private final long dateAdded;
        public final String mimeType;

        private MediaStoreRecord(Builder builder) {
            path = builder.path;
            dateTaken = builder.dateTaken;
            dateAdded = builder.dateAdded;
            mimeType = builder.mimeType;
        }

        public static class Builder {
            private Path path;
            private long dateTaken;
            private long dateAdded;
            public String mimeType;

            public Builder() {
            }

            public Builder path(Path path) {
                this.path = path;
                return this;
            }

            public Builder dateTaken(long dateTaken) {
                this.dateTaken = dateTaken;
                return this;
            }

            public Builder dateAdded(long dateAdded) {
                this.dateAdded = dateAdded;
                return this;
            }

            public Builder mimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public MediaStoreRecord build() {
                return new MediaStoreRecord(this);
            }

        }
    }
}

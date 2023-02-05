package com.yandex.mail.shadows;

import android.app.DownloadManager;
import android.database.Cursor;
import android.database.CursorWrapper;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDownloadManager;

@SuppressWarnings("unused")
@Implements(DownloadManager.class)
public class MailShadowDownloadManager extends ShadowDownloadManager {

    public Cursor query(DownloadManager.Query query) {
        return new ResultCursor(super.query(query));
    }

    private class ResultCursor extends CursorWrapper {

        private static final int COLUMN_MEDIA_TYPE_INDEX = 1000;

        private static final int COLUMN_MEDIA_PROVIDER_URI_INDEX = 1001;

        ResultCursor(Cursor query) {
            super(query);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            final int columnIndex = getColumnIndex(columnName);
            if (columnIndex == -1) {
                throw new IllegalArgumentException("Column not found.");
            }
            return columnIndex;
        }

        @Override
        public int getColumnIndex(String columnName) {
            final int columnIndex = super.getColumnIndex(columnName);
            switch (columnName) {
                case DownloadManager.COLUMN_MEDIA_TYPE:
                    return COLUMN_MEDIA_TYPE_INDEX;
                case DownloadManager.COLUMN_MEDIAPROVIDER_URI:
                    return COLUMN_MEDIA_PROVIDER_URI_INDEX;
                default:
                    return columnIndex;
            }
        }

        @Override
        public String getString(int columnIndex) {
            final String string = super.getString(columnIndex);
            final ShadowRequest request = Shadows.shadowOf(getRequest(getPosition()));
            switch (columnIndex) {
                case COLUMN_MEDIA_TYPE_INDEX:
                    return request.getMimeType().toString();
                case COLUMN_MEDIA_PROVIDER_URI_INDEX:
                    return "content://media/" + getPosition();
                default:
                    return string;
            }
        }
    }
}

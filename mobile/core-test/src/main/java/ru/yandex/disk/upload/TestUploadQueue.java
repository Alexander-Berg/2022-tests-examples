package ru.yandex.disk.upload;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import androidx.annotation.NonNull;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.provider.DiskContract.Queue;
import ru.yandex.disk.replication.SelfContentProviderClient;

import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.yandex.util.Path.asPath;

public class TestUploadQueue {

    private final CredentialsManager credentialsManager;
    private final SelfContentProviderClient client;

    public TestUploadQueue(SelfContentProviderClient client, CredentialsManager cm) {
        credentialsManager = cm;
        this.client = client;
    }

    public void add(Upload upload) {
        ContentValues cv = new ContentValues();
        cv.put(Queue.DEST_DIR, upload.getDestination());
        cv.put(Queue.STATE, Queue.State.IN_QUEUE);
        String srcName = upload.getSource();
        cv.put(Queue.SRC_NAME, srcName);
        cv.put(Queue.SRC_NAME_TOLOWER_NO_PATH, srcName.toLowerCase());
        cv.put(Queue.DEST_NAME, asPath(srcName).getName());
        cv.put(Queue.UPLOAD_ITEM_TYPE, upload.isAuto() ? Queue.UploadItemType.AUTOUPLOAD : Queue.UploadItemType.DEFAULT);

        cv.put(Queue.SIZE, upload.getSize());
        cv.put(Queue.DATE, upload.getDate());
        cv.put(Queue.MEDIA_TYPE_CODE, upload.getMediaTypeCode());

        List<String> path = client.insert(getQueuePath(), cv).getPathSegments();
        long id = Long.valueOf(path.get(path.size() - 1));
        upload.setId(id);
        client.notifyChange(getQueuePath(), null);
    }

    private String getQueuePath() {
        final Credentials user = credentialsManager.getActiveAccountCredentials();
        return Queue.AUTHORITY + "?user=" + user.getUser();
    }

    public Upload peek() {
        return query(null, null, UploadQueue.UPLOADING_SORT_ORDER + " LIMIT 1");
    }

    private Upload query(String selection, String[] selectionArgs, String order) {
        Cursor c = client.query(getQueuePath(), getUploadItemDefaultProjection(), selection, selectionArgs, order);
        if (c.moveToFirst()) {
            Upload upload = new Upload();
            upload.setId(c.getLong(c.getColumnIndexOrThrow(Queue.ID)));
            upload.setState(c.getInt(c.getColumnIndexOrThrow(Queue.STATE)));
            c.close();
            return upload;
        } else {
            return null;
        }
    }

    @NonNull
    public static String[] getUploadItemDefaultProjection() {
        final List<String> columns = new LinkedList<>(asList(UploadQueueSerializer.UPLOAD_ITEM_DEFAULT_PROJECTION));
        columns.add(DiskQueueSerializer.ITEM_TYPE);
        columns.add(DiskQueueSerializer.PRIORITY_CASE);
        return columns.toArray(new String[columns.size()]);
    }

    public Upload findById(long id) {
        return query("_id = ?", asStringsArray(id), null);
    }

    private static String[] asStringsArray(Object object) {
        return new String[]{String.valueOf(object)};
    }

    public void dump() {
        Cursor c = client.query(getQueuePath(), null, null, null, null);
        DatabaseUtils.dumpCursor(c);
        c.close();
    }
}

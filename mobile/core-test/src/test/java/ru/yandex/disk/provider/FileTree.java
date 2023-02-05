package ru.yandex.disk.provider;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import androidx.annotation.VisibleForTesting;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.provider.DiskContract.DiskFile;
import ru.yandex.disk.sql.SQLiteDatabase2;
import ru.yandex.disk.util.Files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static ru.yandex.util.Path.asPath;

public class FileTree {

    private final Directory root;

    public FileTree() {
        this("disk");
    }

    public FileTree(String rootPath) {
        root = new Directory(rootPath);
    }

    public static FileTree create() {
        return new FileTree();
    }

    public FileTree content(Item... items) {
        root().content(items);
        return this;
    }

    private void replaceItemRecursively(Database database, Item item) {
        if (item != root) {
            database.replaceItem(item);
        }
        if (item instanceof Directory) {
            Directory directory = (Directory) item;
            updateDirectoryContent(database, directory);
        }
    }

    private void updateDirectoryContent(Database database, Directory directory) {
        Set<String> fileNames = database.snapshotDirectoryContent(directory);

        for (Item child : directory.items) {
            replaceItemRecursively(database, child);
            fileNames.remove(child.getName());
        }

        database.deleteMissingFiles(directory, fileNames);
    }

    public void createInFileSystem(java.io.File baseDirectory) throws IOException {
        java.io.File rootInFileSystem = new java.io.File(baseDirectory, root.getAbsolutePath());
        Files.dropAllFilesInDir(rootInFileSystem);
        createInFileSystem(baseDirectory, root);
    }

    public void createInFileSystem() throws IOException {
        createInFileSystem(new java.io.File(""));
    }

    private void createInFileSystem(java.io.File baseDirectory, Item item) throws IOException {
        if (item instanceof Directory) {
            Directory directory = (Directory) item;
            java.io.File directoryOnFileSystem = new java.io.File(
                    baseDirectory, directory.getAbsolutePath()
            );
            directoryOnFileSystem.mkdirs();
            for (Item child : directory.items) {
                createInFileSystem(baseDirectory, child);
            }
        } else {
            new java.io.File(baseDirectory, item.getAbsolutePath()).createNewFile();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Item> T itemAt(int... indexs) {
        Item item = root;
        for (int index : indexs) {
            Directory directory = (Directory) item;
            item = directory.items.get(index);
        }
        return (T) item;
    }

    @VisibleForTesting
    public String getPathForItemAt(int... indexs) {
        return DiskContract.DiskFile.AUTHORITY + "/" + itemAt(indexs).getUriPath();
    }

    public Directory root() {
        return root;
    }

    public static Item[] files(String... fileNames) {
        Item[] items = new Item[fileNames.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = file(fileNames[i]);
        }
        return items;
    }

    public void insertToDiskDatabase(DiskDatabase diskDatabase) {
        replaceItemRecursively(new DiskDatabaseAdapter(diskDatabase), root);
    }

    private static ContentValues asContentValues(Item item) {
        ContentValues cv = new ContentValues();
        cv.put(DiskFile.NAME, item.getName());
        Directory parent = item.getParent();
        if (parent != null) {
            String parentName = parent.getAbsolutePath();
            cv.put(DiskFile.PARENT, parentName);
        }
        cv.put(DiskFile.DISPLAY_NAME, item.getName());
        cv.put(DiskFile.DISPLAY_NAME_TOLOWER, item.getName().toLowerCase());

        cv.put(DiskFile.MIME_TYPE, item.contentType);
        cv.put(DiskFile.IS_DIR, item instanceof Directory);

        cv.put(DiskFile.SHARED, item.shared);
        cv.put(DiskFile.READONLY, item.readonly);
        cv.put(DiskFile.PUBLIC_URL, item.publicUrl);
        cv.put(DiskFile.OFFLINE_MARK, item.offlineMark.getCode());
        cv.put(DiskFile.ETAG, item.etag);
        cv.put(DiskFile.ETAG_LOCAL, item.etagLocal);
        cv.put(DiskFile.LAST_MODIFIED, item.lastModified);
        cv.put(DiskFile.ROW_TYPE, DiskFile.RowType.NORMAL);

        if (item instanceof File) {
            File file = (File) item;
            cv.put(DiskFile.MIME_TYPE, file.contentType);
            cv.put(DiskFile.MEDIA_TYPE, file.mediaType);
            cv.put(DiskFile.HAS_THUMBNAIL, file.hasThumbnail);
            cv.put(DiskFile.SIZE, file.size);
            cv.put(DiskFile.ETIME, file.etime);
        }
        return cv;
    }

    public List<String> asPathList() {
        List<String> fileList = new ArrayList<>();
        addNext(root, fileList);
        return fileList;
    }

    private void addNext(Item currentItem, List<String> fileList) {
        fileList.add(currentItem.getPath());

        if (currentItem.isDirectory()) {
            List<Item> children = ((Directory) currentItem).items;
            for (int i = 0; i < children.size(); i++) {
                addNext(children.get(i), fileList);
            }
        }
    }

    public static class Item<T> {
        private Directory parent;
        private final String name;
        protected String contentType;
        protected boolean shared;
        protected boolean readonly;
        private String publicUrl;
        protected DiskItem.OfflineMark offlineMark = DiskItem.OfflineMark.NOT_MARKED;
        protected String etag;
        protected String etagLocal;
        String mediaType;
        private final T self;
        private int lastModified;

        public Item(String name) {
            this.name = name;
            self = (T) this;
        }

        public boolean isDirectory() {
            return this instanceof Directory;
        }

        public String getName() {
            return name;
        }

        public String getAbsolutePath() {
            if (parent != null) {
                return parent.getAbsolutePath() + "/" + name;
            } else {
                return "/" + name;
            }
        }

        public T setContentType(String contentType) {
            this.contentType = contentType;
            return self;
        }

        public String getPath() {
            return getAbsolutePath();
        }

        public Directory getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getContentType() {
            return contentType;
        }

        public Item<T> setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
            return this;
        }

        public Item<T> setOffline(DiskItem.OfflineMark mark) {
            offlineMark = mark;
            return this;
        }

        public Item<T> setLastModified(int lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public String getUriPath() {
            return name;
        }
    }

    public static class File extends Item<File> {
        private boolean hasThumbnail;
        private long size;
        private long etime;

        private File(String name) {
            super(name);
            setEtag("ETAG");
        }

        public File setEtagLocal(String etag) {
            this.etagLocal = etag;
            return this;
        }

        public File setEtag(String etag) {
            this.etag = etag;
            return this;
        }

        public File setEtime(final long etime) {
            this.etime = etime;
            return this;
        }

        @Override
        public File setOffline(DiskItem.OfflineMark mark) {
            super.setOffline(mark);
            return this;
        }

        public File setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public File setHasThumbnail(boolean b) {
            this.hasThumbnail = b;
            return this;
        }

        public File setSize(long size) {
            this.size = size;
            return this;
        }
    }

    public static class Directory extends Item<Directory> {
        private List<Item> items;

        private Directory(String name) {
            super(name);
            items = new LinkedList<>();
        }

        public Directory content(Item... items) {
            for (Item item : items) {
                item.parent = this;
                if (item.contentType == null) {
                    item.contentType = contentType;
                }
                if (!item.shared) {
                    item.shared = shared;
                }
                if (!item.readonly) {
                    item.readonly = readonly;
                }
            }
            this.items = Arrays.asList(items);
            return this;
        }

        public int getItemsCount() {
            return items.size();
        }

        public Directory makeSharedReadonly() {
            shared = readonly = true;
            return this;
        }

        @Override
        public Directory setContentType(String contentType) {
            return super.setContentType(contentType);
        }

        public int getFilesCount() {
            int counter = 0;
            for (Item item : items) {
                if (item instanceof File) {
                    counter++;
                }
            }
            return counter;
        }

        public File fileAt(int position) {
            Item item = items.get(position);
            if (item instanceof File) {
                return (File) item;
            } else {
                return null;
            }
        }

        public Directory setShared(boolean flag) {
            shared = flag;
            return this;
        }

        public List<File> listFiles() {
            LinkedList<File> files = new LinkedList<>();
            for (Item item : items) {
                if (item instanceof File) {
                    files.add((File) item);
                }
            }
            return files;
        }

        @Override
        public Directory setOffline(DiskItem.OfflineMark mark) {
            super.setOffline(mark);
            return this;
        }

        public Directory setSyncStatus(String syncStatus) {
            this.etagLocal = syncStatus;
            return this;
        }

    }

    public static File file(String name) {
        return new File(name);
    }

    public static Directory directory(String name) {
        return new Directory(name);
    }

    private interface Database {

        Set<String> snapshotDirectoryContent(Directory directory);

        void replaceItem(Item item);

        void deleteMissingFiles(Directory directory, Set<String> fileNames);

    }

    private static class DiskDatabaseAdapter implements Database {

        private final DiskDatabase diskDatabase;
        private final SQLiteDatabase2 rawDB;

        public DiskDatabaseAdapter(DiskDatabase diskDatabase) {
            this.diskDatabase = diskDatabase;
            rawDB = diskDatabase.getWritableDatabase();
        }

        @Override
        public Set<String> snapshotDirectoryContent(Directory directory) {
            DiskFileCursor files = diskDatabase.queryFileItemsOfDirectory(asPath(directory.getAbsolutePath()));

            Set<String> fileNames = new HashSet<>();
            for (DiskItem file : files) {
                fileNames.add(file.getDisplayName());
            }
            files.close();
            return fileNames;
        }

        @Override
        public void replaceItem(Item item) {
            ContentValues cv = asContentValues(item);
            int updated = rawDB.update(DiskFileCursor.TABLE, cv,
                    DiskFile.PARENT + " = ? AND " + DiskFile.NAME + " = ?",
                    new String[]{cv.getAsString(DiskFile.PARENT), item.getName()}
            );

            if (updated == 0) {
                rawDB.insert(DiskFileCursor.TABLE, null, cv);
            }

        }

        @Override
        public void deleteMissingFiles(Directory directory, Set<String> fileNames) {
            for (String missingFileName : fileNames) {
                rawDB.delete(DiskFile.TABLE, DiskFile.PARENT + " = ? AND " + DiskFile.NAME + " = ?",
                        new String[]{directory.getAbsolutePath(), missingFileName});
                rawDB.delete(DiskFile.TABLE, DiskFile.PARENT + " LIKE "
                        + DatabaseUtils.sqlEscapeString(missingFileName + "/"), null);
            }
        }

    }
}

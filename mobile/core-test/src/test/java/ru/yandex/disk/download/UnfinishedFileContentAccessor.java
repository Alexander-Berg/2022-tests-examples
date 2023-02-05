package ru.yandex.disk.download;

import ru.yandex.disk.util.FileContentAccessor;

import java.io.File;
import java.io.FilenameFilter;

public class UnfinishedFileContentAccessor extends FileContentAccessor {
    private static final FilenameFilter UNFINISHED_FILES_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.contains(".partial");
        }
    };

    public UnfinishedFileContentAccessor(File root) {
        super(root);
    }
    public UnfinishedFileContentAccessor(String root) {
        super(root);
    }

    public String[] listUnfinishedFiles(String dir) {
        return new File(getRoot(), dir).list(UNFINISHED_FILES_FILTER);
    }

    public String[] listUnfinishedFiles() {
        return getRoot().list(UNFINISHED_FILES_FILTER);
    }

    public void deleteUnfinishedFiles() {
        File root = getRoot();
        String[] unfinishedFiles = root.list(UNFINISHED_FILES_FILTER);
        for (String file : unfinishedFiles) {
            new File(root, file).delete();
        }
    }

}

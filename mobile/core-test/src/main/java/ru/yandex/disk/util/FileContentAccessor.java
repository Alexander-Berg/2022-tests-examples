package ru.yandex.disk.util;

import ru.yandex.util.Path;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static ru.yandex.util.Path.asPath;

public class FileContentAccessor {
    private final File root;

    public FileContentAccessor() {
        this("");
    }
    public FileContentAccessor(String root) {
        this(new File(root));
    }

    public FileContentAccessor(File root) {
        this.root = root;
    }

    public void write(Path path, String data) {
        try {
            mkdirs(path.getParent());
            com.google.common.io.Files.write(data, createFile(path), Charset.defaultCharset());
        } catch (IOException e) {
            Exceptions.crash(e);
        }
    }

    public String read(Path path) throws IOException {
        return com.google.common.io.Files.toString(createFile(path), Charset.defaultCharset());
    }

    private File createFile(Path path) {
        return new File(getRoot(), path.getPath());
    }

    public File getRoot() {
        return root;
    }

    public void mkdirs(Path dir) {
        File file = createFile(dir);
        if (!file.exists() && !file.mkdirs()) {
            Exceptions.crash("cannot mkdir " + file);
        }
    }

    public long length(Path path) {
        return createFile(path).length();
    }

    public void write(String path, String data) {
        write(asPath(path), data);
    }

    public String read(String path) throws IOException {
        return read(asPath(path));
    }

    public void clear() {
        Files.dropAllFilesInDir(getRoot());
    }

    public void delete(Path path) {
        createFile(path).delete();
    }

    public Path getRootPath() {
        return asPath(root.getAbsolutePath());
    }

}

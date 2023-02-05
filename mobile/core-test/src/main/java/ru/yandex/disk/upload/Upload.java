package ru.yandex.disk.upload;

import ru.yandex.disk.provider.DiskContract;
import ru.yandex.util.Path;

public class Upload {

    public interface State extends DiskContract.Queue.State {
    }

    private long id;
    private String source;
    private String destination;
    private int state;
    private boolean auto;
    private long size;
    private long date;
    private int mediaTypeCode;

    public Upload(String sourceFile, String destinationDirectory) {
        this.source = sourceFile;
        this.destination = destinationDirectory;
    }

    Upload() {
    }

    private Upload(Builder builder) {
        source = builder.source;
        destination = builder.destination;
        state = builder.state;
        auto = builder.auto;
        size = builder.size;
        date = builder.date;
        mediaTypeCode = builder.mediaTypeCode;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public int getState() {
        return state;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public boolean isAuto() {
        return auto;
    }

    public long getSize() {
        return size;
    }

    public long getDate() {
        return date;
    }

    public int getMediaTypeCode() {
        return mediaTypeCode;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setState(int state) {
        this.state = state;
    }

    public static class Builder {
        private long size;
        private long date;
        public String source;
        public String destination;
        public int state;
        public boolean auto;
        private int mediaTypeCode;

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder date(long date) {
            this.date = date;
            return this;
        }

        public Builder source(Path path) {
            this.source = path.getPath();
            return this;
        }

        public Builder mediaTypeCode(int mediaType) {
            this.mediaTypeCode = mediaType;
            return this;
        }

        public Builder state(int state) {
            this.state = state;
            return this;
        }

        public Builder auto(boolean b) {
            this.auto = b;
            return this;
        }

        public Builder destination(String d) {
            this.destination = d;
            return this;
        }

        public Upload build() {
            return new Upload(this);
        }
    }

}

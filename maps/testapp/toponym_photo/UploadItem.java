package com.yandex.maps.testapp.toponym_photo;

import android.graphics.Bitmap;
import android.net.Uri;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.places.toponym_photo.PhotoMetadata;

public class UploadItem {
    private Uri uri;
    private String filename;

    private String uploadStatus = "waiting";

    private Long modificationTime;
    private Long shootingTime;
    private Point shootingPoint;
    private Bitmap thumbnail;

    public UploadItem(Uri photoUri, String filename,
                      Long modificationTime, Long shootingTime,
                      Point shootingPoint, Bitmap thumbnail) {
        this.uri = photoUri;
        this.filename = filename;
        this.modificationTime = modificationTime;
        this.shootingTime = shootingTime;
        this.shootingPoint = shootingPoint;
        this.thumbnail = thumbnail;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public Uri uri() {
        return uri;
    }

    public String filename() {
        return filename;
    }

    public Bitmap thumbnail() { return thumbnail; }

    public PhotoMetadata metadata() {
        return new PhotoMetadata(
                modificationTime,
                shootingTime,
                shootingPoint,
                /* targetPoint = */null,
                /* uri = */null);
    }

    public String description() {
        return new StringBuilder()
                .append("Filename: " + filename)
                .append("\nLonLat: ")
                .append(shootingPoint != null
                        ? UploadUtils.formatLonLat(shootingPoint)
                        : "not found")
                .append("\nTaken: ")
                .append(shootingTime != null
                        ? UploadUtils.formatDate(shootingTime)
                        : "not found")
                .append("\nMtime: ")
                .append(modificationTime != null
                        ? UploadUtils.formatDate(modificationTime)
                        : "not found")
                .append("\nUpload: " + uploadStatus)
                .toString();
    }

}

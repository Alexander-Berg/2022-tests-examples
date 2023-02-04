package com.yandex.maps.testapp.photos;

import com.yandex.mapkit.places.photos.Image;

class ImageInfo {
    public final String imageId;
    public final String size;
    public final String author;

    ImageInfo(String imageId, String size, String author) {
        this.imageId = imageId;
        this.size = size;
        this.author = author;
    }
};

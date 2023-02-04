package com.yandex.maps.testapp.photos;

import com.yandex.mapkit.places.photos.PhotosManager;
import com.yandex.mapkit.places.photos.ImageSession;
import com.yandex.mapkit.places.photos.Image;
import com.yandex.runtime.Error;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class PhotosImageView extends ImageView {
    private PhotosManager photosManager;
    private ImageSession imageSession;
    private String pendingImageId;
    private String pendingImageSize;
    private Bitmap notFoundImage;

    public PhotosImageView(Context ctx, AttributeSet attributes) {
        super(ctx, attributes);
    }

    public PhotosImageView(
        Context ctx,
        PhotosManager photosManager,
        Bitmap notFoundImage)
    {
        super(ctx);
        this.photosManager = photosManager;
        this.notFoundImage = notFoundImage;
    }

    public void init(PhotosManager photosManager, Bitmap notFoundImage) {
        this.photosManager = photosManager;
        this.notFoundImage = notFoundImage;
    }

    public void setImage(String id, String size)
    {
        setImageResource(android.R.color.white);
        if (imageSession != null) {
            if (id.equals(pendingImageId) && size == pendingImageSize) {
                Log.d("yandex", "PreviewImage " + id + " is already loading");
                return;
            }
            imageSession.cancel();
        }

        pendingImageId = id;
        pendingImageSize = size;
        imageSession = photosManager.image(
            id,
            size,
            new ImageSession.ImageListener() {
                @Override
                public void onImageReceived(Bitmap bitmap) {
                    imageSession = null;
                    setImageBitmap(bitmap);
                }

                @Override
                public void onImageError(Error error) {
                    imageSession = null;
                    if (notFoundImage != null)
                        setImageBitmap(notFoundImage);
                }
            });
    }
}

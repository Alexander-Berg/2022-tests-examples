package com.yandex.maps.testapp.toponym_photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.yandex.mapkit.places.toponym_photo.ImageSession;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoService;
import com.yandex.runtime.Error;

public class PhotoView extends ImageView {
    private ToponymPhotoService toponymPhotoService;
    private ImageSession imageSession;
    private String pendingImageId;
    private String pendingImageSize;
    private Bitmap notFoundImage;

    public PhotoView(Context ctx, AttributeSet attributes) {
        super(ctx, attributes);
    }

    public PhotoView(
        Context ctx,
        ToponymPhotoService toponymPhotoService,
        Bitmap notFoundImage)
    {
        super(ctx);
        this.toponymPhotoService = toponymPhotoService;
        this.notFoundImage = notFoundImage;
    }

    public void init(ToponymPhotoService toponymPhotoService, Bitmap notFoundImage) {
        this.toponymPhotoService = toponymPhotoService;
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
        imageSession = toponymPhotoService.image(
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

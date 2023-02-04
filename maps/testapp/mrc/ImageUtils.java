package com.yandex.maps.testapp.mrc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.Image;
import com.yandex.runtime.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static Image.ImageSize getImageSize(Image image, String preferred) {
        if (image.getSizes().isEmpty()) {
            return null;
        }

        for (Image.ImageSize size : image.getSizes()) {
            if (size.getSize().equals(preferred)) {
                return size;
            }
        }
        return image.getSizes().get(0);
    }

    public static Bitmap scaleBitmap(Context context, Bitmap bitmap, int sizeDp) {
        int minSide = Math.min(bitmap.getWidth(), bitmap.getHeight());
        double scale = dpToPixels(context, sizeDp) / (double) minSide;
        return Bitmap.createScaledBitmap(
                bitmap,
                (int)Math.round(bitmap.getWidth() * scale),
                (int)Math.round(bitmap.getHeight() * scale),
                true);
    }

    private static float dpToPixels(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    public static byte[] readFakeImageFromAssets(Context context) {
        final String FAKE_IMAGE_FILENAME = "fake_mrc_image.jpg";
        InputStream inputStream;
        try {
            inputStream = context.getAssets().open(FAKE_IMAGE_FILENAME);

            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            Logger.info("Read fake mrc image, " + output.size());

            return output.toByteArray();
        } catch (IOException e) {
            Toast.makeText(context, "Failed to load fake mrc image " + FAKE_IMAGE_FILENAME, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    private static final int THUMBNAIL_SIZE_DP = 60;

    public static Bitmap decodeImage(byte[] imageBytes, float targetSizePx) {
        if (targetSizePx <= 0) {
            throw new IllegalArgumentException("Bitmap size must be greater than zero");
        }

        Size originalSize = readImageSize(imageBytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = (int) (Math.min(originalSize.getWidth(), originalSize.getHeight()) / targetSizePx);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    public static Bitmap decodeThumbnail(Context ctx, byte[] imageBytes) {
        int sizePx = (int)dpToPixels(ctx, THUMBNAIL_SIZE_DP);
        return decodeImage(imageBytes, sizePx);
    }

    public static Bitmap scaleToThumbnail(Context ctx, Bitmap bitmap) {
        int sizePx = (int)dpToPixels(ctx, THUMBNAIL_SIZE_DP);
        return scaleImage(bitmap, sizePx);
    }

    public static byte[] encodeImage(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }


    private static Size readImageSize(byte[] imageData) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
        return new Size(options.outWidth, options.outHeight);
    }

    private static Bitmap scaleImage(Bitmap bitmap, int targetSizePx) {
        if (targetSizePx <= 0) {
            throw new IllegalArgumentException("Bitmap size must be greater than zero");
        }

        int width;
        int height;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            height = targetSizePx;
            width = (int)(height * bitmap.getWidth() / (double)bitmap.getHeight());
        } else {
            width = targetSizePx;
            height = (int)(width * bitmap.getHeight() / (double)bitmap.getWidth());
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }
}

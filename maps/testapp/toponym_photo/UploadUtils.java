package com.yandex.maps.testapp.toponym_photo;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.yandex.mapkit.geometry.Point;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import static kotlin.io.ByteStreamsKt.readBytes;

public class UploadUtils {

    private static final Logger LOGGER =
            Logger.getLogger("yandex.maps");

    public static String getFilename(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static byte[] getBytes(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        return readBytes(inputStream);
    }

    public static Point extractShootingPoint(ExifInterface exifInterface) {
        // Uncomment if built with API 29+
        // double[] latLon = exifInterface.getLatLong();
        // if (latLon == null) { return null; }

        float[] latLon = new float[2];
        if (!exifInterface.getLatLong(latLon)) {
            return null;
        }
        return new Point(latLon[0], latLon[1]);
    }

    public static Long extractShootingTime(ExifInterface exifInterface){
        final String[] dateTimeTags = new String[]{
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME};
        final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
        for (final String dateTimeTag : dateTimeTags) {
            String dateTimeStr = exifInterface.getAttribute(dateTimeTag);
            if (dateTimeStr == null) {
                continue;
            }
            try {
                return dateTimeFormat.parse(dateTimeStr).getTime() / 1000;
            } catch (ParseException e) {
                LOGGER.warning("Unable to parse date " + dateTimeStr + " from tag " + dateTimeTag +
                        " with error: " + e.getMessage());
            }
        }
        return null;
    }

    /* FIXME: modification time is not extracted on Google Pixel 4 XL */
    public static Long extractModificationTime(Context context, Uri uri) {
            if (uri.getScheme().equals("content")) {
                Cursor cursor = context.getContentResolver().query(uri,
                        new String[]{DocumentsContract.Document.COLUMN_LAST_MODIFIED}, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0) {
                        return cursor.getLong(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)) / 1000;
                    }
                } finally {
                    cursor.close();
                }
            } else {
                return new File(uri.getPath()).lastModified() / 1000;
            }
            return null;
    }

    @NonNull
    public static Uri createJpegImageFileInDCIM(Context context, String DCIMSubdir) throws IOException {
        final String imageFileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Uncomment if built with API 29+
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            String relativePath = Environment.DIRECTORY_DCIM + File.separator + CAMERA_PHOTO_SUB_DIR;
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

            return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }*/

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (DCIMSubdir != null) {
            storageDir = new File(storageDir, DCIMSubdir);
        }
        if (!storageDir.exists()) {
            final boolean created = storageDir.mkdirs();
            if (!created) {
                throw new IOException("Can't create folder for image");
            }
        }
        File photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", photoFile);
    }

    public static String formatDate(Long seconds) {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(seconds * 1000));
    }

    public static String formatLonLat(Point point) {
        DecimalFormat fmt = new DecimalFormat("#.0#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return fmt.format(point.getLongitude()) + "," + fmt.format(point.getLatitude());
    }

    private static float dpToPixels(Context context, float dp)
    {
        Resources r = context.getResources();
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }

    public static Bitmap getThumbnail(Context context, Uri imageUri) throws IOException {
        final int THUMBNAIL_SIZE_DP = 60;
        int sizePx = (int)dpToPixels(context, THUMBNAIL_SIZE_DP);

        InputStream input = context.getContentResolver().openInputStream(imageUri);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        // Now decode subsampled image
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = (int) (Math.min(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth) / sizePx);
        input = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }
}

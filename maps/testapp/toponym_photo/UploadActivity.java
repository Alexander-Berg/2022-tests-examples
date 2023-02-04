package com.yandex.maps.testapp.toponym_photo;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoService;
import com.yandex.mapkit.places.toponym_photo.UploadSession;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.runtime.Error;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UploadActivity extends TestAppActivity {

    // Activity request codes
    private static final int ACTIVITY_REQUEST_CHOOSE_IMAGES = 1;
    private static final int ACTIVITY_REQUEST_CAPTURE_IMAGE = 2;

    // Permission request codes
    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    // All photos, captured from camera, will be saved in DCIM subdirectory
    private static final String CAMERA_PHOTO_SUB_DIR = "YandexToponymPhoto";

    private static final Logger LOGGER =
            Logger.getLogger("yandex.maps.ToponymPhotoActivity");

    private ToponymPhotoService toponymPhotoService;
    private Uri capturedImageUri;
    private List<UploadSession> uploadSessions;
    private UploadListAdapter toponymPhotoListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toponym_photo_upload);

        uploadSessions = new ArrayList<UploadSession>();
        toponymPhotoService = PlacesFactory.getInstance().createToponymPhotoService();

        toponymPhotoListAdapter = new UploadListAdapter(this, new ArrayList<>());
        ListView listView = findViewById(R.id.toponym_photo_upload_list);
        listView.setAdapter(toponymPhotoListAdapter);
    }

    public void uploadFromGallery(View view) {
        uploadFromGallery();
    }
    public void uploadFromCamera(View view) {
        uploadFromCamera();
    }

    public void uploadFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent,"Select Picture"), ACTIVITY_REQUEST_CHOOSE_IMAGES);
        } else {
            Toast.makeText(
                    this,
                    "Gallery app not found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadFromCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                return;
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                return;
            }
        }

        try {
            capturedImageUri = UploadUtils.createJpegImageFileInDCIM(this, CAMERA_PHOTO_SUB_DIR);
        } catch (IOException e) {
            LOGGER.warning("Unable to create image file with error: " + e.getMessage());
            Toast.makeText(
                    this,
                    "Unable to create image file",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, ACTIVITY_REQUEST_CAPTURE_IMAGE);
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "Camera app not found",
                    Toast.LENGTH_LONG).show();
        }

    }

    private UploadItem toponymPhotoFromUri(Uri photoUri) {
        // Uncomment if built with API 29+ to get original photo
        //uri = MediaStore.setRequireOriginal(photoUri);

        String filename = UploadUtils.getFilename(this, photoUri);
        Long modificationTime = UploadUtils.extractModificationTime(this, photoUri);
        Point shootingPoint = null;
        Long shootingTime = null;
        Bitmap thumbnail = null;
        try {
            thumbnail = UploadUtils.getThumbnail(this, photoUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                ExifInterface exifInterface = new ExifInterface(this.getContentResolver().openInputStream(photoUri));
                shootingPoint = UploadUtils.extractShootingPoint(exifInterface);
                shootingTime = UploadUtils.extractShootingTime(exifInterface);
            } catch (IOException e) {
                LOGGER.warning("Failed to initialize ExifInterface with error: " + e.getMessage());
            }
        }

        return new UploadItem(photoUri, filename, modificationTime, shootingTime, shootingPoint, thumbnail);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(
                    this,
                    "Photo upload cancelled",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        List<UploadItem> newPhotos = new ArrayList<UploadItem>();
        if (requestCode == ACTIVITY_REQUEST_CHOOSE_IMAGES && data != null) {
            List<Uri> imageList = new ArrayList<Uri>();
            if (data.getData() != null) {
                UploadItem uploadItem = toponymPhotoFromUri(data.getData());
                toponymPhotoListAdapter.add(uploadItem);
                newPhotos.add(uploadItem);
            } else if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    UploadItem uploadItem = toponymPhotoFromUri(clipData.getItemAt(i).getUri());
                    toponymPhotoListAdapter.add(uploadItem);
                    newPhotos.add(uploadItem);
                }
            }
        }
        if (requestCode == ACTIVITY_REQUEST_CAPTURE_IMAGE) {
            UploadItem uploadItem = toponymPhotoFromUri(capturedImageUri);
            toponymPhotoListAdapter.add(uploadItem);
            newPhotos.add(uploadItem);
        }
        if (!newPhotos.isEmpty()) {
            LOGGER.info("Added new photos: " + newPhotos.size());
            toponymPhotoListAdapter.notifyDataSetChanged();
            for (UploadItem uploadItem : newPhotos) {
                uploadPhoto(uploadItem);
            }
        }
    }

    private void uploadPhoto(UploadItem uploadItem) {
        uploadItem.setUploadStatus("uploading");
        toponymPhotoListAdapter.notifyDataSetChanged();

        byte[] imageBytes;
        try {
            imageBytes = UploadUtils.getBytes(this, uploadItem.uri());
        } catch (IOException e) {
            LOGGER.warning("Failed to read image bytes with error: " + e.getMessage());
            uploadItem.setUploadStatus("failed to read");
            toponymPhotoListAdapter.notifyDataSetChanged();
            return;
        }

        final UploadSession.UploadListener uploadListener = new UploadSession.UploadListener() {
            @Override
            public void onUploadSuccess() {
                uploadItem.setUploadStatus("success");
                toponymPhotoListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUploadError(@NonNull @NotNull Error error) {
                LOGGER.warning("Error during photo upload: " + error.toString());
                uploadItem.setUploadStatus("error");
                toponymPhotoListAdapter.notifyDataSetChanged();
            }
        };

        uploadSessions.add(toponymPhotoService.uploadPhoto(
                imageBytes,
                uploadItem.filename(),
                uploadItem.metadata(),
                uploadListener
        ));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                uploadFromCamera();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "external storage permission granted", Toast.LENGTH_LONG).show();
                uploadFromCamera();
            } else {
                Toast.makeText(this, "external storage permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStartImpl() {
        if (AuthUtil.getCurrentAccount() == null) {
            Toast.makeText(
                    this,
                    R.string.sign_into_account,
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStopImpl() {

    }
}

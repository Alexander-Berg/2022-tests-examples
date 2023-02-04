package com.yandex.maps.testapp.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.yandex.mapkit.Attribution;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.atom.Author;
import com.yandex.mapkit.places.photos.Image;
import com.yandex.mapkit.places.photos.PhotoSession;
import com.yandex.mapkit.places.photos.PhotosEntry;
import com.yandex.mapkit.places.photos.PhotosFeed;
import com.yandex.mapkit.places.photos.PhotosManager;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;

import java.util.Iterator;
import java.util.List;

public class PhotosActivity extends TestAppActivity {
    private PhotosManager photosManager;
    private PhotoSession session;
    private EditText organizationId;
    private GridView photosGrid;
    private RelativeLayout bigImageLayout;
    private PhotosImageView bigImage;
    private TextView bigImageAuthor;
    private Bitmap notFoundImage;

    private class ImageGridAdapter extends ArrayAdapter<ImageInfo> {
        public ImageGridAdapter() { super(PhotosActivity.this, 0); }

        @Override
        public View getView(
            final int position,
            View convertView,
            android.view.ViewGroup parent)
        {
            if (position == getCount() - 1)
                fetchNextPage();

            PhotosImageView view = (PhotosImageView)convertView;
            if (view == null) {
                view = new PhotosImageView(
                    PhotosActivity.this,
                    photosManager,
                    notFoundImage);
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setPadding(10, 10, 10, 10);
            }

            final ImageInfo image = getItem(position);
            view.setImage(image.imageId, image.size);
            view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showBigPicture(image);
                    }
                });

            return view;
        }
    }

    @Override
    protected void onPause() {
        photosManager.clear();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.photos);

        photosManager = PlacesFactory.getInstance().createPhotosManager();

        organizationId = (EditText)findViewById(R.id.photos_oid);
        organizationId.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                hideKeyboard(view);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startListPhotos(view.getText().toString());
                }
                return false;
            }
        });

        photosGrid = (GridView)findViewById(R.id.photos_grid);
        photosGrid.setAdapter(new ImageGridAdapter());
        notFoundImage = BitmapFactory.decodeResource(
                getResources(), R.drawable.notfound);
        bigImageLayout = (RelativeLayout)findViewById(
            R.id.photos_full_size_layout);
        bigImage = (PhotosImageView)findViewById(
            R.id.photos_full_size);
        bigImage.init(photosManager, notFoundImage);
        bigImageAuthor = (TextView)findViewById(R.id.photos_author);
    }

    @Override
    public void onBackPressed() {
        if (bigImageLayout.getVisibility() == View.VISIBLE) {
            bigImageLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStopImpl(){}
    @Override
    protected void onStartImpl(){}

    private void hideKeyboard(View view) {
        ((InputMethodManager) getSystemService(
            Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                view.getWindowToken(), 0);
    }

    private void startListPhotos(String bizId) {
        if (session != null) {
            session.cancel();
        }

        ((ImageGridAdapter)photosGrid.getAdapter()).clear();
        session = photosManager.photos(bizId);
        fetchNextPage();
    }

    private void showBigPicture(ImageInfo imageInfo) {
        bigImageLayout.setVisibility(View.VISIBLE);
        bigImage.setImage(imageInfo.imageId, "orig");
        bigImageAuthor.setText(imageInfo.author);
        bigImageLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                bigImageLayout.setVisibility(View.GONE);
            }
        });
    }

    private void fetchNextPage() {
        if (!session.hasNextPage())
            return;

        session.fetchNextPage(
                new PhotoSession.PhotoListener() {
                    @Override
                    public void onPhotosFeedReceived(PhotosFeed feed) {
                        if (feed.getEntries().isEmpty()) {
                            Toast.makeText(
                                    PhotosActivity.this,
                                    "No more photos",
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        loadThumbnails(feed);
                    }

                    @Override
                    public void onPhotosFeedError(Error err) {
                        Utils.showError(PhotosActivity.this, err);
                    }
                });
    }

    private void loadThumbnails(PhotosFeed feed) {
        Iterator<PhotosEntry> it = feed.getEntries().iterator();
        while (it.hasNext()) {
            final PhotosEntry entry = it.next();
            final List<Image> images = entry.getImages();
            if (images.isEmpty())
                continue;

            String photoSource = getImageAuthor(entry);
            String tags = getImageTags(entry);
            String pending = getImagePending(entry);

            ((ImageGridAdapter)photosGrid.getAdapter()).add(
                new ImageInfo(
                    images.get(0).getImageId(),
                    "S",
                    photoSource + tags + pending));
        }

    }

    // returns attribution.author.name if exists;
    // if not - just author.name
    private String getImageAuthor(PhotosEntry entry) {
        String result = "";
        Attribution attribution = entry.getAtomEntry().getAttribution();
        if (attribution != null) {
            Attribution.Author author = attribution.getAuthor();
            if (author != null && author.getName() != null)
                result += "Attr: " + author.getName() + "; ";
        }

        Author author = entry.getAtomEntry().getAuthor();
        if (author != null && author.getName() != null)
            result += "Author: " + author.getName() + "; ";

        return result.isEmpty() ? getString(R.string.photos_not_available) : result;
    }

    private String getImageTags(PhotosEntry entry) {
        String result = "";
        List<String> tags = entry.getTags();
        if (!tags.isEmpty()) {
            result += "Tags: " + TextUtils.join(", ", tags) + "; ";
        }
        return result;
    }

    private String getImagePending(PhotosEntry entry) {
        String result = "";
        Boolean pending = entry.getPending();
        if (pending != null) {
            result += "Pending: " + String.valueOf(pending);
        }
        return result;
    }
}

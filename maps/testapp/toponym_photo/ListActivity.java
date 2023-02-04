package com.yandex.maps.testapp.toponym_photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import com.yandex.mapkit.atom.Author;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.toponym_photo.FeedSession;
import com.yandex.mapkit.places.toponym_photo.Entry;
import com.yandex.mapkit.places.toponym_photo.Feed;
import com.yandex.mapkit.places.toponym_photo.ToponymPhotoService;
import com.yandex.runtime.Error;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;

import java.util.Iterator;

public class ListActivity extends TestAppActivity {
    private ToponymPhotoService toponymPhotoService;
    private FeedSession feedSession;
    private EditText toponymUri;
    private GridView photosGrid;
    private RelativeLayout bigImageLayout;
    private PhotoView bigImage;
    private TextView bigImageAuthor;
    private Bitmap notFoundImage;

    private class ImageGridAdapter extends ArrayAdapter<ListItem> {
        public ImageGridAdapter() { super(ListActivity.this, 0); }

        @Override
        public View getView(
            final int position,
            View convertView,
            android.view.ViewGroup parent)
        {
            if (position == getCount() - 1)
                fetchNextPage();

            PhotoView view = (PhotoView)convertView;
            if (view == null) {
                view = new PhotoView(
                    ListActivity.this,
                    toponymPhotoService,
                    notFoundImage);
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setPadding(10, 10, 10, 10);
            }

            final ListItem listItem = getItem(position);
            view.setImage(listItem.imageId, listItem.size);
            view.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showBigPicture(listItem);
                    }
                });

            return view;
        }
    }

    @Override
    protected void onPause() {
        toponymPhotoService.clearImageCache();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.toponym_photo_list);

        toponymPhotoService = PlacesFactory.getInstance().createToponymPhotoService();

        toponymUri = (EditText)findViewById(R.id.toponym_photo_uri);
        toponymUri.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                hideKeyboard(view);
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startListPhotos(view.getText().toString());
                }
                return false;
            }
        });

        photosGrid = (GridView)findViewById(R.id.toponym_photo_grid);
        photosGrid.setAdapter(new ImageGridAdapter());
        notFoundImage = BitmapFactory.decodeResource(
                getResources(), R.drawable.notfound);
        bigImageLayout = (RelativeLayout)findViewById(
            R.id.toponym_photo_full_size_layout);
        bigImage = (PhotoView)findViewById(
            R.id.toponym_photo_full_size);
        bigImage.init(toponymPhotoService, notFoundImage);
        bigImageAuthor = (TextView)findViewById(R.id.toponym_photo_author);
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

    private void startListPhotos(String uri) {
        if (feedSession != null) {
            feedSession.cancel();
        }

        ((ImageGridAdapter)photosGrid.getAdapter()).clear();
        feedSession = toponymPhotoService.photos(uri);
        fetchNextPage();
    }

    private void showBigPicture(ListItem listItem) {
        bigImageLayout.setVisibility(View.VISIBLE);
        bigImage.setImage(listItem.imageId, "XXL");
        bigImageAuthor.setText(listItem.author);
        bigImageLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                bigImageLayout.setVisibility(View.GONE);
            }
        });
    }

    private void fetchNextPage() {
        if (!feedSession.hasNextPage())
            return;

        feedSession.fetchNextPage(
                new FeedSession.FeedListener() {
                    @Override
                    public void onPhotosFeedReceived(Feed feed) {
                        if (feed.getEntries().isEmpty()) {
                            Toast.makeText(
                                    com.yandex.maps.testapp.toponym_photo.ListActivity.this,
                                    "No more photos",
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        loadThumbnails(feed);
                    }

                    @Override
                    public void onPhotosFeedError(Error err) {
                        Utils.showError(com.yandex.maps.testapp.toponym_photo.ListActivity.this, err);
                    }
                });
    }

    private void loadThumbnails(Feed feed) {
        Iterator<Entry> it = feed.getEntries().iterator();
        while (it.hasNext()) {
            final Entry entry = it.next();

            String photoSource = getImageAuthor(entry);

            ((ImageGridAdapter)photosGrid.getAdapter()).add(
                new ListItem(
                    entry.getContent().getImage().getUrlTemplate(),
                    "S",
                    photoSource));
        }

    }

    // returns attribution.author.name if exists;
    // if not - just author.name
    private String getImageAuthor(Entry entry) {
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

        return result.isEmpty() ? getString(R.string.toponym_photo_not_available) : result;
    }
}

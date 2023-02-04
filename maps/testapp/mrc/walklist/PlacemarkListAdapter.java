package com.yandex.maps.testapp.mrc.walklist;

import static com.yandex.maps.testapp.mrc.ImageUtils.getImageSize;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.mapkit.Image;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.mrc.PlacemarkEditActivity;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.mrc.walk.DeleteLocalPlacemarkSession;
import com.yandex.mrc.walk.DeleteServerPlacemarkSession;
import com.yandex.mrc.walk.LocalPlacemarkIdentifier;
import com.yandex.mrc.walk.PlacemarkData;
import com.yandex.mrc.walk.ServerPlacemarkIdentifier;
import com.yandex.mrc.walk.WalkManager;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlacemarkListAdapter extends RecyclerView.Adapter<PlacemarkListAdapter.ViewHolder> {
    private final List<PlacemarkListItem> placemarksList = new ArrayList<>();

    private final Context context;
    private final ImageDownloader imageDownloader;
    private final WalkManager walkManager;

    public PlacemarkListAdapter(
            Context context,
            ImageDownloader imageDownloader,
            List<PlacemarkListItem> placemarksList) {
        super();
        this.context = context;
        this.imageDownloader = imageDownloader;
        this.placemarksList.addAll(placemarksList);
        walkManager = MRCFactory.getInstance().getWalkManager();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View parentView;
        private TextView createdAtTextView;
        private TextView feedbackTypeTextView;
        private TextView statusTextView;
        private TextView photosCountTextView;
        private ImageView albumImageView;
        private ImageView deletePlacemarkButton;

        PlacemarkListItem placemark;

        ImageSession albumImageLoadingSession;
        ImageSession.ImageListener albumImageListener;

        DeleteLocalPlacemarkSession deleteLocalPlacemarkSession = null;
        DeleteServerPlacemarkSession deleteServerPlacemarkSession = null;

        ViewHolder(View v,
                   View parentView,
                   TextView createdAtTextView,
                   TextView feedbackTypeTextView,
                   TextView statusTextView,
                   TextView photosCountTextView,
                   ImageView albumImageView,
                   ImageView deletePlacemarkButton) {
            super(v);
            this.parentView = parentView;
            this.createdAtTextView = createdAtTextView;
            this.feedbackTypeTextView = feedbackTypeTextView;
            this.statusTextView = statusTextView;
            this.photosCountTextView = photosCountTextView;
            this.albumImageView = albumImageView;
            this.deletePlacemarkButton = deletePlacemarkButton;
        }
    }

    @Override
    @NonNull
    public PlacemarkListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View convertView;
        convertView = inflater.inflate(R.layout.mrc_placemark_list_item, parent, false);

        return new PlacemarkListAdapter.ViewHolder(
                convertView,
                convertView.findViewById(R.id.parent_layout),
                convertView.findViewById(R.id.placemark_created_at),
                convertView.findViewById(R.id.placemark_feedback_type),
                convertView.findViewById(R.id.placemark_status),
                convertView.findViewById(R.id.placemark_comment),
                convertView.findViewById(R.id.placemark_album_image_view),
                convertView.findViewById(R.id.delete_placemark));
    }

    @Override
    public void onBindViewHolder(@NonNull final PlacemarkListAdapter.ViewHolder holder, int position) {
        holder.placemark = placemarksList.get(position);
        refreshPlacemarkInfo(holder);

        if (holder.albumImageLoadingSession != null) {
            holder.albumImageLoadingSession.cancel();
        }

        holder.albumImageListener = new ImageSession.ImageListener() {
            @Override
            public void onImageLoaded(@NonNull Bitmap bitmap) {
                holder.albumImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.albumImageView.setImageBitmap(bitmap);
            }

            @Override
            public void onImageLoadingError(@NonNull Error error) {
                holder.albumImageView.setScaleType(ImageView.ScaleType.CENTER);
                Logger.error("Failed to load image for placemark " + holder.placemark.getId());
            }
        };

        List<PlacemarkData.Photo> photos = holder.placemark.getData().getPhotos();
        if (!photos.isEmpty()) {
            Image albumImage = photos.get(0).getGeoPhoto().getImage();
            holder.albumImageView.setImageBitmap(null);
            Image.ImageSize size = getImageSize(albumImage, "thumbnail");
            if (size == null) {
                Logger.error("Missing album image sizes for placemark " + holder.placemark.getId());
                return;
            }

            holder.albumImageLoadingSession = imageDownloader.loadImageBitmap(
                    albumImage.getUrlTemplate(),
                    size,
                    holder.albumImageListener);
        }
        holder.deletePlacemarkButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();

            if (pos != RecyclerView.NO_POSITION && placemarksList.contains(holder.placemark)) {
                if (holder.placemark.hasLocalPlacemark()) {
                    holder.deleteLocalPlacemarkSession = walkManager.deleteLocalPlacemark(
                            holder.placemark.getLocalPlacemark().id(),
                            deleteLocalPlacemarkListener);
                }
                if (holder.placemark.hasServerPlacemark()) {
                    holder.deleteServerPlacemarkSession = walkManager.deleteServerPlacemark(
                            holder.placemark.getServerPlacemark().id(),
                            deleteServerPlacemarkListener);
                }
            }
        });

        holder.parentView.setOnClickListener(view -> {
            viewPlacemark(holder.placemark);
        });
    }

    void refreshPlacemarkInfo(@NonNull final PlacemarkListAdapter.ViewHolder holder) {
        PlacemarkData data = holder.placemark.getData();

        final CharSequence createdAt = DateUtils.getRelativeDateTimeString(context,
                data.getCreatedAt(), DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.createdAtTextView.setText(createdAt);

        holder.feedbackTypeTextView.setText(context.getString(R.string.mrc_placemark_feedback_type, data.getFeedbackType().toString()));
        holder.statusTextView.setText(context.getString(R.string.mrc_placemark_status, data.getStatus().toString()));
        holder.photosCountTextView.setText(context.getString(R.string.mrc_photos_count, data.getPhotos().size()));
    }

    @Override
    public int getItemCount() {
        return placemarksList.size();
    }

    public void setPlacemarksList(List<PlacemarkListItem> placemarksList) {
        this.placemarksList.clear();
        this.placemarksList.addAll(placemarksList);

        Collections.sort(this.placemarksList, (lhs, rhs) -> {
            if (lhs.getData().getCreatedAt() < rhs.getData().getCreatedAt()) {
                return 1;
            } else if (rhs.getData().getCreatedAt() < lhs.getData().getCreatedAt()) {
                return -1;
            }

            if (lhs.hasLocalPlacemark() && rhs.hasServerPlacemark()) {
                return -1;
            }
            if (lhs.hasServerPlacemark() && rhs.hasLocalPlacemark()) {
                return 1;
            }
            return 0;
        });

        notifyDataSetChanged();
    }

    void viewPlacemark(PlacemarkListItem item) {
        if (item.hasLocalPlacemark()) {
            Intent intent = new Intent(context, PlacemarkEditActivity.class);
            intent.putExtra(PlacemarkEditActivity.INTENT_EXTRA_PLACEMARK_ID,
                    walkManager.serializeLocalPlacemarkId(item.getLocalPlacemark().id()));
            context.startActivity(intent);
        }
    }

    DeleteLocalPlacemarkSession.DeleteLocalPlacemarkListener deleteLocalPlacemarkListener
            = new DeleteLocalPlacemarkSession.DeleteLocalPlacemarkListener() {
        @Override
        public void onLocalPlacemarkDeleted(@NonNull LocalPlacemarkIdentifier localPlacemarkIdentifier) {
            Logger.info("Local Placemark " + localPlacemarkIdentifier + " was deleted");
        }

        @Override
        public void onLocalPlacemarkDeletingError(@NonNull Error error) {
            showUserMessage("Failed to delete local placemark");
        }
    };

    DeleteServerPlacemarkSession.DeleteServerPlacemarkListener deleteServerPlacemarkListener
            = new DeleteServerPlacemarkSession.DeleteServerPlacemarkListener() {
        @Override
        public void onServerPlacemarkDeleted(@NonNull ServerPlacemarkIdentifier serverPlacemarkIdentifier) {
            Logger.info("Server Placemark " + serverPlacemarkIdentifier + " was deleted");
            for (int i = 0; i < placemarksList.size(); i++) {
                PlacemarkListItem item = placemarksList.get(i);
                if (item.hasServerPlacemark() &&
                        item.getServerPlacemark().id().equals(serverPlacemarkIdentifier)) {
                    placemarksList.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, placemarksList.size());
                    break;
                }
            }
        }

        @Override
        public void onServerPlacemarkDeletingError(@NonNull Error error) {
            showUserMessage("Failed to delete server placemark");
        }
    };

    protected void showUserMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}

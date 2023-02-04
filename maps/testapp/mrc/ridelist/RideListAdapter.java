package com.yandex.maps.testapp.mrc.ridelist;

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
import com.yandex.maps.testapp.mrc.DetailedRideActivity;
import com.yandex.mrc.BriefRideInfo;
import com.yandex.mrc.DeleteLocalRideSession;
import com.yandex.mrc.DeleteServerRideSession;
import com.yandex.mrc.Hypothesis;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.LocalRideIdentifier;
import com.yandex.mrc.LocalRideListener;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.ServerRideIdentifier;
import com.yandex.mrc.UploadManager;
import com.yandex.mrc.UploadManagerListener;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.ViewHolder> {
    private final List<RideListItem> ridesList = new ArrayList<>();

    private final Context context;
    private final ImageDownloader imageDownloader;
    private final RideManager rideManager;
    private final UploadManager uploadManager;
    private List<String> uploadingQueue;

    public RideListAdapter(
            Context context,
            ImageDownloader imageDownloader,
            List<RideListItem> ridesList) {
        super();
        this.context = context;
        this.imageDownloader = imageDownloader;
        this.ridesList.addAll(ridesList);
        rideManager = MRCFactory.getInstance().getRideManager();
        uploadManager = MRCFactory.getInstance().getUploadManager();
        uploadManager.subscribe(uploadManagerListener);
        uploadingQueue = uploadManager.getUploadingQueue();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View parentView;
        private TextView titleTextView;
        private TextView statusView;
        private TextView durationTextView;
        private TextView distanceTextView;
        private TextView photosCountTextView;
        private ImageView albumImage;
        private TextView hypothesesCount;
        private TextView hypothesesList;
        private ImageView deleteRideButton;
        private ImageView uploadRideButton;

        RideListItem ride;
        LocalRideListener localRideListener;

        ImageSession albumImageLoadingSession;
        ImageSession.ImageListener albumImageListener;

        DeleteLocalRideSession deleteLocalRideSession = null;
        DeleteServerRideSession deleteServerRideSession = null;

        ViewHolder(View v,
                   View parentView,
                   TextView titleTextView,
                   TextView statusView,
                   TextView durationTextView,
                   TextView distanceTextView,
                   TextView photosCountTextView,
                   ImageView albumImage,
                   TextView hypothesesCount,
                   TextView hypothesesList,
                   ImageView deleteRideButton,
                   ImageView uploadRideButton) {
            super(v);
            this.parentView = parentView;
            this.titleTextView = titleTextView;
            this.statusView = statusView;
            this.durationTextView = durationTextView;
            this.distanceTextView = distanceTextView;
            this.photosCountTextView = photosCountTextView;
            this.albumImage = albumImage;
            this.hypothesesCount = hypothesesCount;
            this.hypothesesList = hypothesesList;
            this.deleteRideButton = deleteRideButton;
            this.uploadRideButton = uploadRideButton;
        }
    }

    @Override
    @NonNull
    public RideListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View convertView;
        convertView = inflater.inflate(R.layout.mrc_ride_list_item, parent, false);

        return new RideListAdapter.ViewHolder(
                convertView,
                convertView.findViewById(R.id.parent_layout),
                convertView.findViewById(R.id.ride_title),
                convertView.findViewById(R.id.ride_status),
                convertView.findViewById(R.id.ride_duration),
                convertView.findViewById(R.id.ride_distance),
                convertView.findViewById(R.id.ride_photos_count),
                convertView.findViewById(R.id.ride_image_preview),
                convertView.findViewById(R.id.ride_hypotheses_count),
                convertView.findViewById(R.id.ride_hypotheses_list),
                convertView.findViewById(R.id.delete_ride),
                convertView.findViewById(R.id.upload_ride));
    }

    @Override
    public void onBindViewHolder(@NonNull final RideListAdapter.ViewHolder holder, int position) {
        holder.ride = ridesList.get(position);
        refreshRideInfo(holder);

        if (holder.albumImageLoadingSession != null) {
            holder.albumImageLoadingSession.cancel();
        }

        holder.albumImageListener = new ImageSession.ImageListener() {
            @Override
            public void onImageLoaded(@NonNull Bitmap bitmap) {
                holder.albumImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.albumImage.setImageBitmap(bitmap);
            }

            @Override
            public void onImageLoadingError(@NonNull Error error) {
                holder.albumImage.setScaleType(ImageView.ScaleType.CENTER);
                Logger.error("Failed to load album photo for rideId " + holder.ride.getRideId());
            }
        };

        Image albumImage = holder.ride.getBriefInfo().getAlbumImage();
        if (albumImage != null) {
            holder.albumImage.setImageBitmap(null);
            Image.ImageSize size = getImageSize(albumImage, "thumbnail");
            if (size == null) {
                Logger.error("Missing album image sizes for rideId " + holder.ride.getRideId());
                return;
            }

            holder.albumImageLoadingSession = imageDownloader.loadImageBitmap(
                    albumImage.getUrlTemplate(),
                    size,
                    holder.albumImageListener);
        }

        setupDeleteButton(holder);
        setupUploadingButton(holder);

        if (holder.ride.hasLocalRide()) {
            holder.localRideListener = new LocalRideListener() {
                @Override
                public void onRideChanged(@NonNull LocalRide ride) {
                    if (holder.ride.hasLocalRide() &&
                            holder.ride.getLocalRide().id().equals(ride.id())) {
                        refreshRideInfo(holder);
                    }
                }

                @Override
                public void onRideError(@NonNull LocalRide ride, @NonNull Error error) {
                    showUserMessage("Local ride error");
                }
            };

            holder.ride.getLocalRide().subscribe(holder.localRideListener);
        }

        holder.parentView.setOnClickListener(view -> viewDetailedRide(holder.ride));
    }

    private void setupDeleteButton(@NonNull ViewHolder holder) {
        holder.deleteRideButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();

            if (pos != RecyclerView.NO_POSITION && ridesList.contains(holder.ride)) {
                if (holder.ride.hasLocalRide()) {
                    holder.deleteLocalRideSession = rideManager.deleteLocalRide(
                            holder.ride.getLocalRide().id(),
                            deleteLocalRideListener);
                    MRCFactory.getInstance().getUploadManager().clear(holder.ride.getRideId());
                }
                if (holder.ride.hasServerRide()) {
                    holder.deleteServerRideSession = rideManager.deleteServerRide(
                            holder.ride.getServerRide().id(),
                            deleteServerRideListener);
                }
            }
        });
    }

    private void setupUploadingButton(@NonNull ViewHolder holder) {
        if (holder.ride.hasServerRide()) {
            holder.uploadRideButton.setVisibility(View.GONE);
            holder.uploadRideButton.setOnClickListener(null);
        } else if (holder.ride.hasLocalRide()) {
            holder.uploadRideButton.setVisibility(View.VISIBLE);

            RideListItem item = holder.ride;

            if (uploadingQueue.contains(item.getRideId())) {
                holder.uploadRideButton.setImageResource(R.drawable.ic_uploading_in_progress_selector);
                holder.uploadRideButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && ridesList.contains(item) && item.hasLocalRide()) {
                        uploadManager.dequeueFromUploading(item.getRideId());
                    }
                });
            } else if (item.getLocalPhotosCount() > 0) {
                holder.uploadRideButton.setImageResource(R.drawable.ic_upload_selector);
                holder.uploadRideButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && ridesList.contains(item) && item.hasLocalRide()) {
                        uploadManager.enqueueForUploading(item.getRideId());
                    }
                });
            } else {
                holder.uploadRideButton.setImageResource(R.drawable.ic_uploading_done);
                holder.uploadRideButton.setOnClickListener(null);
            }
        }
    }

    void refreshRideInfo(@NonNull final RideListAdapter.ViewHolder holder) {
        BriefRideInfo bri = holder.ride.getBriefInfo();

        final CharSequence startedAt = DateUtils.getRelativeDateTimeString(context,
                bri.getStartedAt(), DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.titleTextView.setText(startedAt);

        holder.statusView.setText(context.getString(R.string.mrc_ride_status, bri.getStatus()));

        if (bri.getDuration() != null) {
            holder.durationTextView.setText(
                    context.getString(R.string.mrc_ride_duration, bri.getDuration().getText()));
        }
        if (bri.getTrackDistance() != null) {
            holder.distanceTextView.setText(
                    context.getString(R.string.mrc_ride_track_distance, bri.getTrackDistance().getText()));
        }

        final long localPhotosCount = holder.ride.getLocalPhotosCount();
        final long totalPhotosCount = bri.getPhotosCount() != null ? bri.getPhotosCount() : 0L;
        holder.photosCountTextView.setText(context.getString(R.string.mrc_ride_photos_count, localPhotosCount, totalPhotosCount));

        holder.hypothesesCount.setText(
                context.getString(R.string.mrc_ride_hypotheses_count, bri.getHypotheses().size()));
        StringBuilder hypothesesData = new StringBuilder();
        for (Hypothesis h : bri.getHypotheses()) {
            if (hypothesesData.length() > 0) {
                hypothesesData.append("\n");
            }
            hypothesesData.append(h.getType()).append(": ").append(h.getFeedbackTaskId());
        }
        holder.hypothesesList.setText(hypothesesData.toString());
    }

    @Override
    public int getItemCount() {
        return ridesList.size();
    }

    public void setRidesList(List<RideListItem> ridesList) {
        this.ridesList.clear();
        this.ridesList.addAll(ridesList);

        Collections.sort(this.ridesList, (lhs, rhs) -> {
            if (lhs.getBriefInfo().getStartedAt() < rhs.getBriefInfo().getStartedAt()) {
                return 1;
            } else if (rhs.getBriefInfo().getStartedAt() < lhs.getBriefInfo().getStartedAt()) {
                return -1;
            }

            if (lhs.hasLocalRide() && rhs.hasServerRide()) {
                return -1;
            }
            if (lhs.hasServerRide() && rhs.hasLocalRide()) {
                return 1;
            }
            return 0;
        });

        notifyDataSetChanged();
    }

    void viewDetailedRide(RideListItem ride) {
        Intent intent = new Intent(context, DetailedRideActivity.class);
        if (ride.hasLocalRide()) {
            intent.putExtra(DetailedRideActivity.INTENT_EXTRA_LOCAL_RIDE_ID,
                    rideManager.serializeLocalRideId(ride.getLocalRide().id()));
        } else if (ride.hasServerRide()) {
            intent.putExtra(DetailedRideActivity.INTENT_EXTRA_SERVER_RIDE_ID,
                    rideManager.serializeServerRideId(ride.getServerRide().id()));
        }
        context.startActivity(intent);
    }

    DeleteLocalRideSession.DeleteLocalRideListener deleteLocalRideListener = new DeleteLocalRideSession.DeleteLocalRideListener() {
        @Override
        public void onLocalRideDeleted(@NonNull LocalRideIdentifier localRideIdentifier) {
            Logger.info("Local Ride " + localRideIdentifier + " was deleted");
        }

        @Override
        public void onLocalRideDeletingError(@NonNull Error error) {
            showUserMessage("Failed to delete local ride");
        }
    };

    DeleteServerRideSession.DeleteServerRideListener deleteServerRideListener = new DeleteServerRideSession.DeleteServerRideListener() {
        @Override
        public void onServerRideDeleted(@NonNull ServerRideIdentifier serverRideIdentifier) {
            Logger.info("Server Ride " + serverRideIdentifier + " was deleted");
            for (int i = 0; i < ridesList.size(); i++) {
                RideListItem item = ridesList.get(i);
                if (item.hasServerRide() &&
                        item.getServerRide().id().equals(serverRideIdentifier)) {
                    ridesList.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, ridesList.size());
                    break;
                }
            }
        }

        @Override
        public void onServerRideDeletingError(@NonNull Error error) {
            showUserMessage("Failed to delete server ride");
        }
    };

    private UploadManagerListener uploadManagerListener = new UploadManagerListener() {
        @Override
        public void onCurrentUploadingItemChanged() {
            Logger.info("onCurrentUploadingItemChanged");
            notifyDataSetChanged();
        }

        @Override
        public void onUploadingStateChanged() {
            Logger.info("onUploadingStateChanged");
            notifyDataSetChanged();
        }

        @Override
        public void onUploadingQueueChanged() {
            Logger.info("onUploadingQueueChanged");
            uploadingQueue = uploadManager.getUploadingQueue();
            notifyDataSetChanged();
        }

        @Override
        public void onSizeCalculated(@NonNull List<String> list, long l, long l1) { }

        @Override
        public void onClearCompleted(@NonNull List<String> list) { }

        @Override
        public void onUploadingError(@NonNull Error error) { }

        @Override
        public void onDataOperationError(@NonNull Error error) { }
    };

    protected void showUserMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}

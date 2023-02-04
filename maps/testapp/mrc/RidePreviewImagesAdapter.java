package com.yandex.maps.testapp.mrc;

import static com.yandex.maps.testapp.mrc.ImageUtils.getImageSize;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.mapkit.Image;
import com.yandex.maps.testapp.R;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.mrc.TrackPreviewItem;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class RidePreviewImagesAdapter extends RecyclerView.Adapter<RidePreviewImagesAdapter.ViewHolder> {
    private final ImageDownloader imageDownloader;
    private final List<TrackPreviewItem> items;

    public RidePreviewImagesAdapter(
            ImageDownloader imageDownloader,
            List<TrackPreviewItem> itemsList) {
        super();
        this.imageDownloader = imageDownloader;
        this.items = new ArrayList<>(itemsList);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        TrackPreviewItem item;
        ImageSession imageLoadingSession;
        ImageSession.ImageListener imageLoadingListener;

        ViewHolder(View v, ImageView imageView) {
            super(v);
            this.imageView = imageView;
        }
    }

    @Override
    @NonNull
    public RidePreviewImagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View convertView;
        convertView = inflater.inflate(R.layout.mrc_ride_image_preview_list_item, parent, false);

        return new RidePreviewImagesAdapter.ViewHolder(convertView,
                convertView.findViewById(R.id.image_preview));
    }

    @Override
    public void onBindViewHolder(@NonNull final RidePreviewImagesAdapter.ViewHolder holder, int position) {
        holder.item = items.get(position);

        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (holder.imageLoadingSession != null) {
            holder.imageLoadingSession.cancel();
        }

        holder.imageLoadingListener = new ImageSession.ImageListener() {
            @Override
            public void onImageLoaded(@NonNull Bitmap bitmap) {
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onImageLoadingError(@NonNull Error error) {
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER);
                Logger.error("Failed to load track photo: " + holder.item.getPreviewImage().getUrlTemplate());
            }
        };

        Image image = holder.item.getPreviewImage();
        if (image != null) {
            holder.imageView.setImageBitmap(null);
            Image.ImageSize size = getImageSize(image, "thumbnail");
            if (size == null) {
                Logger.error("Missing image sizes for image " + image.getUrlTemplate());
                return;
            }

            holder.imageLoadingSession = imageDownloader.loadImageBitmap(
                    image.getUrlTemplate(),
                    size,
                    holder.imageLoadingListener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}

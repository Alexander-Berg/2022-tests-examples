package com.yandex.maps.testapp.mrc.walklist;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.yandex.mapkit.GeoPhoto;
import com.yandex.mapkit.Image;
import com.yandex.mapkit.geometry.Direction;
import com.yandex.mapkit.geometry.Point;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.mrc.ImageUtils;
import com.yandex.mrc.ImageDownloader;
import com.yandex.mrc.ImageSession;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class PreviewImagesAdapter extends RecyclerView.Adapter<PreviewImagesAdapter.ViewHolder> {
    public static class PreviewImage {
        String id;
        GeoPhoto.ShootingPoint shootingPoint;

        @Nullable
        Bitmap bitmap;
        @Nullable
        Image image;

        public GeoPhoto.ShootingPoint shootingPoint() { return shootingPoint; }

        public PreviewImage(
                String id,
                GeoPhoto.ShootingPoint shootingPoint,
                @Nullable Bitmap bitmap,
                @Nullable Image image) {
            this.id = id;
            this.shootingPoint = shootingPoint;
            this.bitmap = bitmap;
            this.image = image;
        }
    }

    public interface Listener {
        void addImage();
        void removeImage(String id);
    }

    private static final PreviewImage DUMMY_IMAGE = new PreviewImage("0",
            new GeoPhoto.ShootingPoint(
                    new GeoPhoto.Point3D(new Point(0, 0), 0.0),
                    new Direction(0.0, 0.0)), null, null);

    private final ImageDownloader imageDownloader;
    private final List<PreviewImage> imagesList;
    private final Listener actionListener;

    public enum IsEditable { YES, NO };
    private IsEditable isEditable;

    public PreviewImagesAdapter(
            ImageDownloader imageDownloader,
            List<PreviewImage> imagesList,
            Listener actionListener,
            IsEditable isEditable) {
        super();
        this.imageDownloader = imageDownloader;
        this.imagesList = new ArrayList<>(imagesList);
        this.actionListener = actionListener;
        this.isEditable = isEditable;

        if (isEditable == IsEditable.YES) {
            // Add extra item for the (+)-element which adds new photo when tapped.
            this.imagesList.add(0, DUMMY_IMAGE);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private ImageView deleteImageButton;
        private CardView cardView;

        PreviewImage image;

        ImageSession imageLoadingSession;
        ImageSession.ImageListener imageLoadingListener;

        ViewHolder(View v, CardView cardView, ImageView imageView, ImageView deleteImageButton) {
            super(v);
            this.cardView = cardView;
            this.imageView = imageView;
            this.deleteImageButton = deleteImageButton;
        }
    }

    @Override
    @NonNull
    public PreviewImagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View convertView;
        convertView = inflater.inflate(R.layout.mrc_walk_image_preview_item, parent, false);

        return new PreviewImagesAdapter.ViewHolder(convertView,
                convertView.findViewById(R.id.image_preview_cardview),
                convertView.findViewById(R.id.image_preview),
                convertView.findViewById(R.id.delete_image_button));
    }

    @Override
    public void onBindViewHolder(@NonNull final PreviewImagesAdapter.ViewHolder holder, int position) {
        holder.image = imagesList.get(position);

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
                String url = holder.image.image != null ? holder.image.image.getUrlTemplate() : null;
                Logger.error("Failed to load track photo: " + url);
            }
        };


        if (isEditable == IsEditable.YES && position == 0) {
            // Customize "Add new photo" element
            holder.imageView.setImageResource(R.drawable.add_mrc_walk_photo);
            holder.imageView.setOnClickListener(view -> actionListener.addImage());
            holder.deleteImageButton.setVisibility(View.GONE);
            holder.cardView.setCardElevation(0f);
        } else {
            // Customize regular image preview element
            if (holder.image.bitmap != null) {
                holder.imageView.setImageBitmap(holder.image.bitmap);
            } else if (holder.image.image != null) {
                holder.imageView.setImageBitmap(null);
                Image.ImageSize size = ImageUtils.getImageSize(holder.image.image, "thumbnail");
                if (size == null) {
                    Logger.error("Missing image sizes for image " + holder.image.image.getUrlTemplate());
                    return;
                }

                holder.imageLoadingSession = imageDownloader.loadImageBitmap(
                        holder.image.image.getUrlTemplate(),
                        size,
                        holder.imageLoadingListener);
            }
            holder.deleteImageButton.setOnClickListener(v -> {
                int pos = imagesList.indexOf(holder.image);
                if (pos >= 0) {
                    imagesList.remove(pos);
                    notifyItemRemoved(pos);
                }
                actionListener.removeImage(holder.image.id);
            });
            holder.deleteImageButton.setVisibility(isEditable == IsEditable.YES ? View.VISIBLE : View.GONE);
            holder.cardView.setCardElevation(4f);
        }
    }

    @Override
    public int getItemCount() {
        return imagesList.size();
    }

    public void setIsEditable(IsEditable newValue) {
        if (isEditable != newValue) {
            isEditable = newValue;
            if (isEditable == IsEditable.YES) {
                imagesList.add(0, DUMMY_IMAGE);
                notifyItemInserted(0);
            } else {
                imagesList.remove(0);
                notifyItemRemoved(0);
            }
            notifyItemRangeChanged(0, imagesList.size());
        }
    }

    public int getNumberOfImages() {
        return imagesList.size() - (isEditable == IsEditable.YES ? 1 : 0);
    }

    public PreviewImage getImageAt(int index) {
        if (isEditable == IsEditable.YES) {
            index += 1;
        }
        return imagesList.get(index);
    }

    public void addImage(PreviewImage image) {
        imagesList.add(image);
        notifyItemInserted(imagesList.size() - 1);
    }
}

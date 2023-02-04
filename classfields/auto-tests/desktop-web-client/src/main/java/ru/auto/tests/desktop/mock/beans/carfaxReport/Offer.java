package ru.auto.tests.desktop.mock.beans.carfaxReport;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.mock.beans.photos.Photo;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Offer {

    @SerializedName("offer_id")
    String offerId;
    Photo photo;
    @SerializedName("time_of_placement")
    String timeOfPlacement;
    Integer price;
    @SerializedName("region_name")
    String regionName;
    @SerializedName("offer_link")
    String offerLink;
    Integer mileage;
    @SerializedName("mileage_status")
    String mileageStatus;
    List<Photo> photos;
    @SerializedName("offer_status")
    String offerStatus;
    String description;
    String section;
    String category;
    String geobaseId;
    @SerializedName("time_of_expire")
    String timeOfExpire;
    @SerializedName("comments_info")
    CommentsInfo commentsInfo;

    public static Offer offer() {
        return new Offer();
    }

}

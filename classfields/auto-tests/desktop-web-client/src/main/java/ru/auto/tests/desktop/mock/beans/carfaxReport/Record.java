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
public class Record {

    String date;
    List<Photo> gallery;
    @SerializedName("comments_info")
    CommentsInfo commentsInfo;

    public static Record record() {
        return new Record();
    }

}

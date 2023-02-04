package ru.auto.tests.desktop.mock.beans.carfaxReport;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Commentable {

    @SerializedName("block_id")
    String blockId;
    @SerializedName("add_comment")
    Boolean addComment;

    public static Commentable commentable() {
        return new Commentable();
    }

}

package ru.auto.tests.desktop.mock.beans.offer.auction;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Auction {

    @SerializedName("current_state")
    CurrentState currentState;
    List<Segment> segments;

    public static Auction auction() {
        return new Auction();
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}

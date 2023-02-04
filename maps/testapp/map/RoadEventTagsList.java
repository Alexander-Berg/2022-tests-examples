package com.yandex.maps.testapp.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.yandex.mapkit.road_events.EventTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RoadEventTagsList implements Parcelable {
    private List<EventTag> tags;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(tags.size());
        for (EventTag tag : tags) {
            out.writeInt(tag.ordinal());
        }
    }

    public static final Parcelable.Creator<RoadEventTagsList> CREATOR = new Parcelable.Creator<RoadEventTagsList>() {
        public RoadEventTagsList createFromParcel(Parcel in) {
            return new RoadEventTagsList(in);
        }

        public RoadEventTagsList[] newArray(int size) {
            return new RoadEventTagsList[size];
        }
    };

    public RoadEventTagsList(List<EventTag> tags) {
        this.tags = new ArrayList<EventTag>(tags);
    }

    private RoadEventTagsList(Parcel in) {
        tags = new ArrayList<EventTag>();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            tags.add(EventTag.values()[in.readInt()]);
        }
    }

    public List<EventTag> getTags() { return tags; }
}

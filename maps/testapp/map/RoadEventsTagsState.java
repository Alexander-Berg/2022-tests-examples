package com.yandex.maps.testapp.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.yandex.mapkit.road_events.EventTag;

import java.util.LinkedHashMap;
import java.util.Map;

public class RoadEventsTagsState implements Parcelable {
    private Map<EventTag, Boolean> state;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(state.size());
        for (Map.Entry<EventTag, Boolean> entry : state.entrySet()) {
            out.writeInt(entry.getKey().ordinal());
            out.writeByte((byte) (entry.getValue().booleanValue() ? 1 : 0));
        }
    }

    public static final Parcelable.Creator<RoadEventsTagsState> CREATOR = new Parcelable.Creator<RoadEventsTagsState>() {
        public RoadEventsTagsState createFromParcel(Parcel in) {
            return new RoadEventsTagsState(in);
        }

        public RoadEventsTagsState[] newArray(int size) {
            return new RoadEventsTagsState[size];
        }
    };

    public RoadEventsTagsState(boolean[] enabled) {
        if (enabled.length != EventTag.values().length) {
            throw new IllegalArgumentException("Incorrect length of array");
        }

        state = new LinkedHashMap<>();
        for (int i = 0; i < enabled.length; i++) {
            state.put(EventTag.values()[i], enabled[i]);
        }
    }

    public RoadEventsTagsState(Map<EventTag, Boolean> newState) {
        if (newState.size() != EventTag.values().length) {
            throw new IllegalArgumentException("Incorrect length of array");
        }

        state = new LinkedHashMap<>(newState);
    }

    private RoadEventsTagsState(Parcel in) {
        state = new LinkedHashMap<>();
        int size = in.readInt();
        if (size != EventTag.values().length) {
            throw new AssertionError("Incorrect number of values was serialized");
        }
        for (int i = 0; i < size; i++) {
            EventTag tag = EventTag.values()[in.readInt()];
            Boolean enabled = in.readByte() != 0;
            state.put(tag, enabled);
        }
    }

    boolean isTagEnabled(EventTag tag) {
        return state.get(tag).booleanValue();
    }

    void setTagEnabled(EventTag tag, boolean enabled) {
        state.put(tag, enabled);
    }
}

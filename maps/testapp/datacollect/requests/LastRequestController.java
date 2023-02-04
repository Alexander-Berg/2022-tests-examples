package com.yandex.maps.testapp.datacollect.requests;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.yandex.runtime.logging.LogListener;
import com.yandex.runtime.logging.LogMessage;
import com.yandex.runtime.logging.LoggingFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LastRequestController implements LogListener {
    static private final Pattern datacollectRequestPattern =
            Pattern.compile(".*uri=(.*/mapkit2/datacollect/2.x/traffic.*)");

    private Listener listener;

    public interface Listener {
        void onLastRequestUpdated(Request lastRequest);
    }

    public static class Request {
        public long timestamp;
        public String lang;
        public String miid;
        public String vehicleType;
        public String source;

        public Request(long timestamp, String lang, String miid, String vehicleType, String source) {
            this.timestamp = timestamp;
            this.lang = lang;
            this.miid = miid;
            this.vehicleType = vehicleType;
            this.source = source;
        }
    }

    public void subscribe(Listener listener) {
        LoggingFactory.getLogging().subscribe(this);
        this.listener = listener;
    }

    public void unsubscribe() {
        LoggingFactory.getLogging().unsubscribe(this);
        this.listener = null;
    }

    @Override
    public void onMessageRecieved(@NonNull LogMessage logMessage) {
        Matcher matcher = datacollectRequestPattern.matcher(logMessage.getMessage());
        if (!matcher.matches()) {
            return;
        }

        String uriString = matcher.group(1);
        Uri lastDatacollectRequest = Uri.parse(uriString);
        Request lastRequest = new Request(
                logMessage.getTime(),
                lastDatacollectRequest.getQueryParameter("lang"),
                lastDatacollectRequest.getQueryParameter("miid"),
                lastDatacollectRequest.getQueryParameter("vehicle_type"),
                lastDatacollectRequest.getQueryParameter("source"));

        if (listener != null) {
            listener.onLastRequestUpdated(lastRequest);
        }
    }
}

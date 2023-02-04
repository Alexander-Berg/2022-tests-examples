package com.yandex.maps.testapp.logs;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.runtime.logging.LogListener;
import com.yandex.runtime.logging.LogMessage;
import com.yandex.runtime.logging.LoggingFactory;
import com.yandex.runtime.recording.EventListener;
import com.yandex.runtime.recording.EventLoggingFactory;

public class LogViewActivity extends TestAppActivity {

    private String filter = "";
    private SearchView searchView;
    private RateLimiter rateLimiter = new RateLimiter(1000);

    private LogListener logListener = message -> refresh();
    private EventListener eventListener = (event, data) -> refresh();

    private SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            filter = newText;
            refresh();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_view);
        ((TextView)findViewById(R.id.log_text)).setMovementMethod(new LinkMovementMethod());
        ((TextView)findViewById(R.id.log_text)).setOnFocusChangeListener((view, b) -> {
            if (b)
                searchView.requestFocus();
        });
        searchView = findViewById(R.id.log_search);
        searchView.setOnQueryTextListener(queryTextListener);
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null,
            null);
        TextView searchTextView = searchView.findViewById(id);
        searchTextView.setTextColor(Color.WHITE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogController.getInstance().resetCounters();
        LoggingFactory.getLogging().subscribe(logListener);
        EventLoggingFactory.getEventLogging().subscribe(eventListener);
        refresh();
    }

    @Override
    public void onStop() {
        LoggingFactory.getLogging().unsubscribe(logListener);
        EventLoggingFactory.getEventLogging().unsubscribe(eventListener);
        super.onStop();
    }

    protected void refresh() {
        rateLimiter.run(() -> {
            SpannableStringBuilder history = LogController.getInstance().getHistory(
                new LogController.ClickHandler() {
                    @Override
                    public void onClick(LogEvent msg) {
                        if (msg.getVerboseInfo().isEmpty()) {
                            Toast.makeText(LogViewActivity.this, msg.getScope(), Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(LogViewActivity.this, VerboseInfoActivity.class);
                            intent.putExtra(VerboseInfoActivity.EXTRA_SCOPE, msg.getScope());
                            intent.putExtra(VerboseInfoActivity.EXTRA_INFO, msg.getVerboseInfo());
                            startActivity(intent);
                        }
                    }
                }, filter);

            ((TextView)findViewById(R.id.log_info)).setText(
                LogController.getInstance().infoText());
            ((TextView)findViewById(R.id.log_text)).setText(
                history,
                TextView.BufferType.SPANNABLE);
        });
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}

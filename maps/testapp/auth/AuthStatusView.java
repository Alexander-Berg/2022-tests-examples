package com.yandex.maps.testapp.auth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yandex.maps.testapp.R;

public class AuthStatusView extends LinearLayout {
    public AuthStatusView(Context context) {
        super(context);
        init(context);
    }

    public AuthStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AuthStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void onResume() {
        init(getContext());
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.auth_status, this);

        TextView loginStatusView = findViewById(R.id.auth_status_login);
        Button actionButton = findViewById(R.id.auth_status_action);

        final boolean loggedIn = AuthUtil.getCurrentAccount() != null;

        loginStatusView.setText(loggedIn ? loggedInText() : loggedOutText());
        actionButton.setText("Auth...");
        actionButton.setOnClickListener(v -> openAuthActivity());
    }

    private void openAuthActivity() {
        this.getContext().startActivity(
            new Intent(getContext(), AuthActivity.class));
    }

    private SpannableString loggedInText() {
        final String text = "Logged in: ";
        final String accountName = AuthUtil.getCurrentAccount().getAndroidAccount().name;
        final String resultString = text + accountName;

        SpannableString result = new SpannableString(resultString);
        result.setSpan(new ForegroundColorSpan(Color.GREEN), 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        result.setSpan(new ForegroundColorSpan(Color.YELLOW), text.length(), resultString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        return result;
    }

    private SpannableString loggedOutText() {
        final CharSequence text = "Logged out";
        SpannableString result = new SpannableString(text);
        result.setSpan(new ForegroundColorSpan(Color.RED), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        return result;
    }
}

package com.yandex.maps.testapp;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.text.TextUtils;

import com.yandex.mapkit.search.SuggestItem;
import com.yandex.mapkit.search.SuggestItem.Action;
import com.yandex.mapkit.search.SuggestItem.Type;
import com.yandex.mapkit.SpannableString;
import com.yandex.mapkit.SpannableString.Span;
import com.yandex.mapkit.LocalizedValue;

public class SuggestResult {
    private SuggestItem element;

    private final int SUGGEST_COLOR = 0xffb0b0b0;
    private final int PERSONAL_COLOR = 0xffa41bc1;

    public SuggestResult(SuggestItem element) {
        this.element = element;
    }

    public CharSequence getTitle() {
        return spannableToCharSequence(element.getTitle(), element.getIsPersonal());
    }

    public CharSequence getSubtitle() {
        CharSequence subtitle = spannableToCharSequence(element.getSubtitle(), element.getIsPersonal());
        if (subtitle == null) {
            return "";
        }
        return subtitle;
    }

    public CharSequence getType() {
        Type type = element.getType();
        switch (type) {
            default: return "UNKNOWN";
            case TOPONYM: return "TOPONYM";
            case BUSINESS: return "BUSINESS";
            case TRANSIT: return "TRANSIT";
        }
    }

    public CharSequence getTags() {
        return TextUtils.join(", ", element.getTags());
    }

    public CharSequence getDistance() {
        LocalizedValue distance = element.getDistance();
        if (distance == null) {
            return "";
        }
        return distance.getText();
    }

    public String getSearchText() {
        return element.getSearchText().toString();
    }

    public String getDisplayText() {
        CharSequence text = element.getDisplayText();
        return (text != null) ? text.toString() : getSearchText();
    }

    public CharSequence getAction() {
        Action action = element.getAction();
        switch (action) {
            case SEARCH: return "SEARCH";
            case SUBSTITUTE: return "SUBSTITUTE";
        }
        return "UNKNOWN ACTION";
    }

    private CharSequence spannableToCharSequence(SpannableString string, boolean isPersonal) {
        if (string == null) {
            return null;
        }
        SpannableStringBuilder stringBuilder =
                new SpannableStringBuilder(string.getText());
        for (Span span : string.getSpans()) {
            addSpan(stringBuilder, span);
        }
        setColor(stringBuilder, isPersonal ? PERSONAL_COLOR : SUGGEST_COLOR);
        return stringBuilder;
    }

    private static void addSpan(SpannableStringBuilder builder, Span span) {
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        builder.setSpan(bold,
                span.getBegin(),
                span.getEnd(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private static void setColor(SpannableStringBuilder builder, int color) {
        ForegroundColorSpan colored = new ForegroundColorSpan(color);
        builder.setSpan(colored,
                0,
                builder.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }
}

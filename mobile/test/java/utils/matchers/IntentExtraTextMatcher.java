package utils.matchers;

import android.content.Intent;

import org.mockito.ArgumentMatcher;

public class IntentExtraTextMatcher implements ArgumentMatcher<Intent> {
    private String text;

    public IntentExtraTextMatcher(String text) {
        this.text = text;
    }

    @Override
    public boolean matches(Intent argument) {
        final Intent intent = argument.getParcelableExtra(Intent.EXTRA_INTENT);
        return intent.getStringExtra(Intent.EXTRA_TEXT).equals(text);
    }

    @Override
    public String toString() {
        return String.format("with extra Intent.EXTRA_TEXT='%s'", text);
    }
}

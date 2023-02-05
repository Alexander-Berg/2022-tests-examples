package utils.matchers;

import android.content.Intent;

import org.mockito.ArgumentMatcher;

import java.util.Map;

import javax.annotation.NonNullByDefault;

@NonNullByDefault
public class IntentMatcher implements ArgumentMatcher<Intent> {

    private final String className;
    private final Map<String, String> extras;

    public IntentMatcher(String className, Map<String, String> extras) {
        this.className = className;
        this.extras = extras;
    }

    @Override
    public boolean matches(Intent intent) {
        if (!intent.getComponent().getClassName().equals(className)) {
            return false;
        }
        for(final Map.Entry<String, String> entry : extras.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (!intent.getStringExtra(key).equals(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return className + extras.toString();
    }
}

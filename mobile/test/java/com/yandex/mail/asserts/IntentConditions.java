package com.yandex.mail.asserts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.yandex.mail.provider.Constants;
import com.yandex.mail.util.Utils;

import org.assertj.core.api.Condition;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.yandex.mail.asserts.Conditions.matching;

@SuppressWarnings("NewApi")
public final class IntentConditions {

    private IntentConditions() {
        throw new IllegalStateException("No instances please.");
    }

    @NonNull
    public static Condition<Intent> action(@NonNull String action) {
        return new Condition<Intent>() {
            @Override
            public boolean matches(@NonNull Intent value) {
                return Objects.equals(value.getAction(), action);
            }

            @Override
            @NonNull
            public String toString() {
                return String.format("Have action <%s>", action);
            }
        };
    }

    @NonNull
    public static Condition<Intent> component(@NonNull ComponentName component) {
        return matching(value -> Objects.equals(value.getComponent(), component));
    }

    @NonNull
    public static Condition<Intent> activity(@NonNull Class<? extends Activity> activityClass) {
        return matching(intent -> Objects.equals(intent.getComponent().getClassName(), activityClass.getName()));
    }

    @NonNull
    public static Condition<Intent> uid(long uid) {
        return extra(Constants.UID_EXTRA, uid);
    }

    @NonNull
    public static Condition<Intent> extra(@NonNull String name, @Nullable Object value) {
        return new Condition<Intent>() {
            @Override
            public boolean matches(@NonNull Intent intent) {
                if (!intent.hasExtra(name)) {
                    return false;
                }
                final Object valueFromIntent = intent.getExtras().get(name);

                // messageIds stored as long array
                if (value instanceof long[] && valueFromIntent instanceof long[] && Arrays.equals((long[]) value, (long[]) valueFromIntent)) {
                    return true;
                }
                return Utils.equals(valueFromIntent, value);
            }

            @Override
            @NonNull
            public String toString() {
                return String.format("Have key <%s> with value <%s>", name, value);
            }
        };
    }

    @NonNull
    public static Condition<Intent> containsExtra(@NonNull String name) {
        return new Condition<Intent>() {
            @Override
            public boolean matches(@NonNull Intent value) {
                return value.hasExtra(name);
            }
        };
    }

    @NonNull
    public static Condition<Intent> equalsByExtras(@NonNull Intent intent) {
        return new Condition<Intent>() {
            @Override
            public boolean matches(@NonNull Intent value) {
                final Bundle originExtras = intent.getExtras();
                final Bundle checkedExtras = value.getExtras();
                final Set<String> originKeySet = originExtras.keySet();
                final Set<String> checkedKeySet = checkedExtras.keySet();
                final boolean sameKeys = originKeySet.equals(checkedKeySet);
                if (!sameKeys) {
                    return false;
                }

                for (String key : originKeySet) {
                    final Object originValue = originExtras.get(key);
                    final Object checkedValue = checkedExtras.get(key);
                    if (!Objects.deepEquals(originValue, checkedValue)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}

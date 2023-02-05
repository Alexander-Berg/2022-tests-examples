package ru.yandex.market;

import androidx.annotation.NonNull;

public final class CommonFailMessages {

    @NonNull
    public static String defaultInstance(
            @NonNull final Class<?> clazz,
            @NonNull final Throwable exception) {

        return "Failed to create " + clazz.getSimpleName() + " instance with default builder. " +
                "Did you add members to class and forgot to add default values for those members " +
                "in builder() factory method?" +
                "\nException was: " + exception + ".";
    }

    @NonNull
    public static String jsonSerializationFailed(
            @NonNull final Class<?> clazz,
            @NonNull final Throwable exception) {

        return "Exception occurred while serializing instance of class " + clazz.getSimpleName() +
                " to Json. Do you forget to add TypeAdapter for class? If you are using " +
                "AutoValue, do not forget to add static method for generate type adapter, see " +
                "documentation at https://github.com/rharter/auto-value-gson for details." +
                "\nException was: " + exception + ".";
    }

    private CommonFailMessages() {
    }
}

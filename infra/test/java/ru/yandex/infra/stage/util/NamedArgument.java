package ru.yandex.infra.stage.util;

import java.util.Map;
import java.util.stream.Collectors;

public final class NamedArgument<T> {

    public static <KeyType, ValueType> Map<KeyType, ValueType> toArgumentsMap(Map<KeyType, NamedArgument<ValueType>> namedArgumentsMap) {
        return namedArgumentsMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getArgument()
                ));
    }

    public static <T> NamedArgument<T> of(T argument) {
        return new NamedArgument<>(String.valueOf(argument), argument);
    }

    public static <T> NamedArgument<T> of(String name, T argument) {
        return new NamedArgument<>(name, argument);
    }

    private final String name;

    @SuppressWarnings("FieldNotUsedInToString")
    private final T argument;

    private NamedArgument(String name, T argument) {
        this.name = name;
        this.argument = argument;
    }

    @Override
    public String toString() {
        return name;
    }

    public T getArgument() {
        return argument;
    }

    public String getName() {
        return name;
    }
}

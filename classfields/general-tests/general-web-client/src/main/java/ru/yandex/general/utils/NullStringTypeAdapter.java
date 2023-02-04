package ru.yandex.general.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;

public final class NullStringTypeAdapter extends TypeAdapter<String> {

    private NullStringTypeAdapter() {
    }

    @Override
    @SuppressWarnings("resource")
    public void write(final JsonWriter out, final String value)
            throws IOException {
        if (value.equals("null")) {
            out.setSerializeNulls(true);
            out.nullValue();
            out.setSerializeNulls(false);
        } else if (value != null) {
            out.value(value);
        }
    }

    @Override
    public String read(final JsonReader in)
            throws IOException {
        final JsonToken token = in.peek();
        switch (token) {
            case NULL:
                in.nextNull();
                return null;
            case NUMBER:
            case BEGIN_ARRAY:
            case END_ARRAY:
            case BEGIN_OBJECT:
            case END_OBJECT:
            case NAME:
            case STRING:
                final String d = in.nextString();
                return d;
            case BOOLEAN:
            case END_DOCUMENT:
                throw new MalformedJsonException("Unexpected token: " + token);
                // so this would never happen unless Gson adds a new token some day...
            default:
                throw new AssertionError("must never happen");
        }
    }

}

package com.yandex.launcher;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import androidx.annotation.NonNull;
import android.util.Xml;

import com.yandex.launcher.common.util.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TestResources extends Resources {

    final InputStream resourcesStream;
    private Map<Integer, String> resIdToValueMap = new HashMap<>();

    public TestResources(Context context, InputStream resourcesStream) throws IOException, XmlPullParserException {
        super(context.getAssets(), context.getResources().getDisplayMetrics(),
                context.getResources().getConfiguration());

        this.resourcesStream = resourcesStream;

        final XmlPullParser parser = Xml.newPullParser();
        parser.setInput(resourcesStream, "UTF-8");
        parseResources(parser);
    }

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
        final int resId = getResourceId(defType, name);
        final String value = resIdToValueMap.get(resId);
        if (value == null) {
            return 0;
        }
        return resId;
    }

    @NonNull
    @Override
    public String getString(int id) throws NotFoundException {
        return getValue(id);
    }

    @Override
    public int getInteger(int id) throws NotFoundException {
        final String value = getValue(id);
        return Integer.parseInt(value);
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        return getColor(id, null);
    }

    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        final String value = getValue(id);
        return Color.parseColor(value);
    }

    public void close() {
        if(resourcesStream != null) {
            try {
                resourcesStream.close();
            } catch (IOException ignore) {}
        }
    }

    private void parseResources(XmlPullParser parser) throws IOException, XmlPullParserException {
        while(parser.nextTag() == XmlPullParser.START_TAG) {
            final String type = parser.getName();

            switch (type) {
                case "string":
                case "integer":
                case "color":
                    //TODO add support for string-array
                    break;
                default:
                    continue;
            }

            final String name = parser.getAttributeValue(null, "name");
            final String value = parser.nextText();

            if (type == null || name == null || value == null) {
                throw new IllegalArgumentException("Illegal test resources format");
            }

            resIdToValueMap.put(getResourceId(type, name), value);
        }
    }

    private String getValue(int resId) {
        final String value = resIdToValueMap.get(resId);
        final int referenceResId = getReferenceResourceId(value);
        if (referenceResId != -1) {
            return getValue(referenceResId);
        }
        if (value == null) {
            throw new NotFoundException();
        }
        return value;
    }

    private static int getResourceId(String type, String name) {
        final String resource = TextUtils.engFormat("%s_%s", type, name);
        return resource.hashCode();
    }

    private static int getReferenceResourceId(String value) {
        if (!value.startsWith("@")) {
            return -1;
        }

        final int dividerIndex = value.indexOf("/");
        if (dividerIndex < 0) {
            return -1;
        }

        final String type = value.substring(1, dividerIndex);
        final String name = value.substring(Math.min(dividerIndex + 1, value.length()),
                value.length());
        return getResourceId(type, name);
    }
}

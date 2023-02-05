package ru.yandex.maps.utils;

import java.util.UUID;

import javax.annotation.NonNullByDefault;

import ru.yandex.yandexmaps.bookmarks.on_map.BookmarkOnMap;
import ru.yandex.yandexmaps.multiplatform.bookmarks.common.RawBookmark;
import ru.yandex.yandexmaps.multiplatform.bookmarks.common.BookmarkId;
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point;
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.places.ImportantPlace;
import ru.yandex.yandexmaps.multiplatform.datasync.wrapper.places.ImportantPlaceType;

import static ru.yandex.yandexmaps.multiplatform.core.geometry.AndroidPointKt.createPoint;

@NonNullByDefault
public class Stub {

    public static ImportantPlace home() {
        return new ImportantPlace(
                ImportantPlaceType.HOME,
                createPoint(22, -1),
                "home",
                "1600 Pennsylvania Avenue",
                "pens"
        );
    }


    public static BookmarkOnMap bookmark() {
        return new BookmarkOnMap(
                new RawBookmark(new BookmarkId(string()), string(), string(), string(), string()),
                null,
                null,
                createPoint(22, -1),
                string(),
                false
        );
    }

    public static Point point() {
        return createPoint(1f, 1f);
    }

    public static String string() {
        return UUID.randomUUID().toString();
    }
}

/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.database.sql.cursor;

import android.database.Cursor;
import androidx.annotation.NonNull;

import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.LIST_POSITION;
import static com.yandex.datasync.internal.database.sql.DatabaseDescriptor.Value.Rows.PARENT_ID;

public class ValueTestExtCursor extends ValueCursor {

    private final int parentIdIndex;

    private final int listPositionIndex;

    public ValueTestExtCursor(@NonNull final Cursor cursor) {
        super(cursor);
        parentIdIndex = cursor.getColumnIndex(PARENT_ID);
        listPositionIndex = cursor.getColumnIndex(LIST_POSITION);
    }

    public long getParentId() {
        return getLong(parentIdIndex);
    }

    public int getListPosition() {
        return getInt(listPositionIndex);
    }
}

package com.yandex.mail.utils;

import android.database.MatrixCursor;

import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.util.SolidUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

import static com.yandex.mail.provider.CursorUtils.extractInt;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.SetsKt.setOf;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(UnitTestRunner.class)
public class SolidUtilsUnitTest {

    @Test
    public void mapFromCursor() throws Exception {
        List<Integer> rows = listOf(1, 2, 4, 5);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"first_column"});
        for (Integer row : rows) {
            matrixCursor.addRow(new Object[]{row});
        }

        List<Integer> mapped = SolidUtils.mapFromCursor(matrixCursor, extractInt(0));

        assertThat(mapped).isEqualTo(rows);
        assertThat(matrixCursor.isClosed()).isTrue();
    }

    @Test
    public void mapToSolidSetFromCursor() throws Exception {
        Set<Integer> rows = setOf(1, 2, 4, 5);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"first_column"});
        for (Integer row : rows) {
            matrixCursor.addRow(new Object[]{row});
        }
        for (Integer row : rows) {
            matrixCursor.addRow(new Object[]{row});
        }

        Set<Integer> mapped = SolidUtils.mapToSetFromCursor(extractInt(0)).apply(matrixCursor);

        assertThat(mapped).isEqualTo(rows);
        assertThat(matrixCursor.isClosed()).isTrue();
    }
}

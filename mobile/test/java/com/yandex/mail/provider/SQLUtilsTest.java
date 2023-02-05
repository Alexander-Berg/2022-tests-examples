package com.yandex.mail.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.util.ContentValuesBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.Unit;
import kotlin.collections.ArraysKt;

import static com.yandex.mail.provider.SQLUtils.getAscSortClause;
import static com.yandex.mail.provider.SQLUtils.getConcatAndTrimClause;
import static com.yandex.mail.provider.SQLUtils.getDescSortClause;
import static com.yandex.mail.provider.SQLUtils.getEqualsClause;
import static com.yandex.mail.provider.SQLUtils.getEscapedEqualsClause;
import static com.yandex.mail.provider.SQLUtils.getInClause;
import static com.yandex.mail.provider.SQLUtils.getInSelectionClause;
import static com.yandex.mail.provider.SQLUtils.getNotEqualsClause;
import static com.yandex.mail.provider.SQLUtils.getNotInClause;
import static com.yandex.mail.provider.SQLUtils.tableExists;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(UnitTestRunner.class)
public class SQLUtilsTest {

    @NonNull
    private final String INTS_TABLE = "test_table_ints";

    @NonNull
    private final String STRINGS_TABLE = "test_table_strings";

    @NonNull
    private final String COLUMN = "test_column";

    @SuppressWarnings("NullableProblems") // initialized in @Before
    @NonNull
    private SQLiteDatabase db;

    @Before
    public void beforeEachTest() {
        db = SQLiteDatabase.create(null);
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY)", INTS_TABLE, COLUMN));
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT)", STRINGS_TABLE, COLUMN));
    }

    private void insertIntsHelper(@NonNull Integer... values) {
        ArraysKt.forEach(values, value -> {
            final ContentValues contentValues = new ContentValuesBuilder().put(COLUMN, value).get();
            db.insert(INTS_TABLE, null, contentValues);
            return Unit.INSTANCE;
        });
    }

    private void insertStringsHelper(@NonNull String... strings) {
        ArraysKt.forEach(strings, value -> {
            final ContentValues contentValues = new ContentValuesBuilder().put(COLUMN, value).get();
            db.insert(STRINGS_TABLE, null, contentValues);
            return Unit.INSTANCE;
        });
    }

    @Test
    public void getAscSortClause_sortsAscending() {
        insertIntsHelper(4, 0, 3, 1, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, null, null, null, null, getAscSortClause(COLUMN))) {
            List<Integer> sortedValues = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(sortedValues).containsExactly(0, 1, 2, 3, 4);
        }
    }

    @Test
    public void getDescSortClause_sortsDescending() {
        insertIntsHelper(4, 0, 3, 1, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, null, null, null, null, getDescSortClause(COLUMN))) {
            List<Integer> sortedValues = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(sortedValues).containsExactly(4, 3, 2, 1, 0);
        }
    }

    @Test
    public void testGetInClause() {
        insertIntsHelper(0, 3, 6, 2, 5, 1);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getInClause(listOf(2, 1, 4), COLUMN), null, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(1, 2);
        }
    }

    @Test
    public void testGetNotInClause() {
        insertIntsHelper(0, 3, 6, 2, 5, 1);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getNotInClause(listOf(2, 1, 4), COLUMN), null, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(0, 3, 6, 5);
        }
    }

    /**
     * NOTE: This test is only to prevent regression! Such use of getEqualsClause is discouraged!
     * Use {@link SQLUtils#getEscapedEqualsClause(String, String)} instead.
     */
    @Test
    public void getEqualsClause_handlesInts() {
        insertIntsHelper(0, 3, 6, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getEqualsClause(COLUMN, "3"), null, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(3);
        }
    }

    @Test
    public void getEqualsClause_handlesColumns() {
        insertStringsHelper("aaa", "bbb", "ccc");
        try (Cursor cursor = db.query(STRINGS_TABLE, new String[]{COLUMN}, getEqualsClause(COLUMN, COLUMN), null, null, null, null)) {
            final List<String> values = CursorsKt.toList(cursor, c -> c.getString(0));
            assertThat(values).containsOnly("aaa", "bbb", "ccc");
        }
    }

    @Test
    public void getEscapedEqualsClause_handlesStrings() {
        insertStringsHelper("aaa", "bbb", "ccc");
        try (Cursor cursor = db.query(STRINGS_TABLE, new String[]{COLUMN}, getEscapedEqualsClause(COLUMN, "bbb"), null, null, null, null)) {
            final List<String> values = CursorsKt.toList(cursor, c -> c.getString(0));
            assertThat(values).containsOnly("bbb");
        }
    }

    @Test
    public void testGetEqualsClause_parametrized() {
        insertIntsHelper(0, 3, 6, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getEqualsClause(COLUMN), new String[]{"3"}, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(3);
        }
    }

    @Test
    public void getSqlConcatQueryField_concatsAndTrims() {
        insertStringsHelper(" aaa", "  cc c ");
        try (Cursor cursor = db.rawQuery(
                String.format(
                        "SELECT %s FROM %s AS fst, %s AS snd",
                        getConcatAndTrimClause("fst." + COLUMN, "snd." + COLUMN),
                        STRINGS_TABLE,
                        STRINGS_TABLE
                ), null
        )) {
            List<String> values = CursorsKt.toList(cursor, c -> c.getString(0));
            assertThat(values).containsOnly(
                    "aaa  aaa",
                    "aaa   cc c",
                    "cc c   aaa",
                    "cc c    cc c"
            );
        }
    }

    @Test
    public void getInSelectionClause_filterBySelection() {
        insertIntsHelper(1, 2, 3, 4, 5, 6);
        String selection = "SELECT " + COLUMN + " AS m1 " + " FROM " + INTS_TABLE + " WHERE " + COLUMN + " > 3";
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getInSelectionClause(COLUMN, selection), null, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(4, 5, 6);
        }
    }

    @Test
    public void getNotEqualsClause_handlesInts() {
        insertIntsHelper(0, 3, 6, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getNotEqualsClause(COLUMN, "3"), null, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(0, 6, 2);
        }
    }

    @Test
    public void getNotEqualsClause_handlesColumns() {
        insertStringsHelper("aaa", "bbb", "ccc");
        try (Cursor cursor = db.query(STRINGS_TABLE, new String[]{COLUMN}, getNotEqualsClause(COLUMN, COLUMN), null, null, null, null)) {
            final List<String> values = CursorsKt.toList(cursor, c -> c.getString(0));
            assertThat(values).isEmpty();
        }
    }

    @Test
    public void getNotEqualsClause_parametrized() {
        insertIntsHelper(0, 3, 6, 2);
        try (Cursor cursor = db.query(INTS_TABLE, new String[]{COLUMN}, getNotEqualsClause(COLUMN), new String[]{"3"}, null, null, null)) {
            List<Integer> values = CursorsKt.toList(cursor, c -> c.getInt(0));
            assertThat(values).containsOnly(0, 6, 2);
        }
    }

    @Test
    public void test_tableExists() {
        insertIntsHelper(3, 2, 1);

        assertThat(tableExists(db, INTS_TABLE)).isTrue();
        assertThat(tableExists(db, STRINGS_TABLE)).isTrue();
        assertThat(tableExists(db, "whatever")).isFalse();
    }
}

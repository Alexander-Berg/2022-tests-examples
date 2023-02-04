package com.yandex.mobile.verticalcore.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import com.google.gson.Gson;
import com.yandex.mobile.verticalcore.BaseTest;
import com.yandex.mobile.verticalcore.BuildConfig;
import com.yandex.mobile.verticalcore.utils.AuthorityHelper;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.android.controller.ContentProviderController;

import java.util.List;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.ProviderCompartment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author rogovalex on 20.06.17.
 */
public class SQLiteContentProviderTest extends BaseTest {

    private Cupboard cupboard;
    private ProviderCompartment providerCompartment;

    @Before
    public void setup() {
        cupboard = CupboardProvider.withDefaultSettings(new Gson())
                .registerEntity(Entry.class)
                .buildAndSetupCupboard();
        SQLiteOpenHelper openHelper = new CupboardSQLiteOpenHelper2(context, "test.db", 1, cupboard);
        SQLiteContentProvider provider = new SQLiteContentProvider() {
            @Override
            public SQLiteOpenHelper getOpenHelper() {
                return openHelper;
            }
        };
        ProviderInfo info = new ProviderInfo();
        info.authority = BuildConfig.APPLICATION_ID + AuthorityHelper.ENTITY;
        ContentProviderController.of(provider).create(info);
        provider.onCreate();

        providerCompartment = cupboard.withContext(context);
    }

    @Test
    public void insertEntrySuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(1, list.size());
        assertEquals("first", list.get(0).value);
        assertNotNull(list.get(0)._id);
    }

    @Test
    public void deleteEntrySuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));
        providerCompartment.put(getUriForEntry(), new Entry(null, "second"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("second", list.get(1).value);

        providerCompartment.delete(getUriForEntry(), list.get(0));

        list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(1, list.size());
        assertEquals("second", list.get(0).value);
    }

    @Test
    public void updateEntryUsingUriSuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));
        providerCompartment.put(getUriForEntry(), new Entry(null, "second"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("second", list.get(1).value);

        ContentValues values = new ContentValues();
        values.put("value", "updated");
        Uri uriWithId = ContentUris.withAppendedId(getUriForEntry(), list.get(0)._id);
        providerCompartment.update(uriWithId, values);

        list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("updated", list.get(0).value);
        assertEquals("second", list.get(1).value);
    }

    @Test
    public void updateEntryUsingValuesSuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));
        providerCompartment.put(getUriForEntry(), new Entry(null, "second"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("second", list.get(1).value);

        ContentValues values = cupboard.withEntity(Entry.class)
                .toContentValues(new Entry(list.get(1)._id, "updated"));
        providerCompartment.update(getUriForEntry(), values);

        list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("updated", list.get(1).value);
    }

    @Test
    public void queryEntryUsingUriSuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));
        providerCompartment.put(getUriForEntry(), new Entry(null, "second"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("second", list.get(1).value);

        Uri uriWithId = ContentUris.withAppendedId(getUriForEntry(), list.get(0)._id);
        list = providerCompartment.query(uriWithId, Entry.class).query().list();

        assertEquals(1, list.size());
        assertEquals("first", list.get(0).value);
    }

    @Test
    public void queryEntryUsingSelectionSuccess() {
        providerCompartment.put(getUriForEntry(), new Entry(null, "first"));
        providerCompartment.put(getUriForEntry(), new Entry(null, "second"));

        List<Entry> list = providerCompartment.query(getUriForEntry(), Entry.class).list();
        assertEquals(2, list.size());
        assertEquals("first", list.get(0).value);
        assertEquals("second", list.get(1).value);

        list = providerCompartment.query(getUriForEntry(), Entry.class)
                .withSelection(BaseColumns._ID + " = ?", String.valueOf(list.get(1)._id))
                .query().list();

        assertEquals(1, list.size());
        assertEquals("second", list.get(0).value);
    }

    private Uri getUriForEntry() {
        return AuthorityHelper.uri(Entry.class, BuildConfig.APPLICATION_ID);
    }

    public static class Entry {
        Long _id;
        String value;

        public Entry() {
        }

        Entry(Long _id, String value) {
            this._id = _id;
            this.value = value;
        }
    }
}
